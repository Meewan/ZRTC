/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import fr.meewan.zrtc.utils.NetworkMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * worker effectuant els taches venant du réseau interne
 * @author Meewan
 */
public class CommandInternalWorker extends Thread
{
    private CommandServer commandServer;
    private ZContext  context;
    private boolean stop;
    
    public CommandInternalWorker(CommandServer commandServer, ZContext context) 
    {
        this.commandServer = commandServer;
        this.context= context;
        this.stop = false;
    }
    
    @Override
    public void run()
    {
        //initialisation du réseau
        ZMQ.Socket socket = context.createSocket(ZMQ.REP);
        socket.connect (commandServer.INTERNAL_COM_ADRESS);
        while(! this.stop)
        {
            //écoute d'une demande
            String rawMessage = socket.recvStr (0);
            Map<String,String> message = new JSONDeserializer<HashMap>().deserialize(rawMessage);
            socket.send("ok", 0);
            message = execute(message);
            if(message != null && Integer.parseInt(message.get("state")) < Integer.parseInt(message.get("lifecyclestates")))
            {
               sendInNetwork(message);
            }
        }
    }
    
    /**
     * Méthode executant le traitement a faire et vérifiant els droits.
     * Cette méthode retourne null si si elle ne trouve pas le worker et modifie lemodifie sinon
     * @param message
     * @return 
     */
    private Map<String, String> execute(Map <String, String> message)
    {
        String command = message.get("command");
        boolean authorized = message.get("authorized") != null && "true".equals(message.get("authorized"));
        boolean correctsignature = message.get("correctsignature")!= null && "true".equals(message.get("correctsignature"));
        String commandId = message.get("commandid");
        int state = Integer.parseInt(message.get("state")) + 1;
        message.put("state", Integer.toString(state));
        int argc = Integer.parseInt(message.get("argc"));
        List<String> args = new ArrayList<>();
        for(int i = 0; i < argc; i++)
        {
            args.add(i, message.get("arg" + i));
        }
        if (!authorized)
        {
            message = NetworkMessage.generateErrorMessage(2, commandId);
        }
        else if(!correctsignature)
        {
            message = NetworkMessage.generateErrorMessage(3, commandId);
        }
        else
        {
            switch (command.toLowerCase())// traitement pour chaque commande
            {
                case "ping":
                {
                   if (commandServer.getWaitingCommand(commandId) != null)
                   {
                       commandServer.addToActiveExternalConnexions(message.get("user"), commandServer.getWaitingCommand(commandId));
                       commandServer.removeFromWaitingCommands(commandId);
                       return null;
                   }
                   else 
                   {
                       return null;
                   }
                }
                case "connect":
                {
                    if(!sendToClient(NetworkMessage.generateMessage(commandId, command, message.get("uid"))))
                    {
                        return null;
                    }
                }
                    break;
                case "mode":
                {
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "join":
                {
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "message":
                {
                    CommandExternalWorker target = commandServer.getActiveExternalConnexions(message.get("arg0"));
                    if(target == null)
                    {
                        if(sendToClient(NetworkMessage.generateErrorMessage(6, commandId)))
                        {
                            return NetworkMessage.generateErrorMessage(6, commandId);
                        }
                        else
                        {
                            return null;
                        }
                    }
                    else
                    {
                        Map<String, String> newMessage = new HashMap<>(message);
                        newMessage.put("arg0", message.get("user"));
                        target.setMessage(newMessage);
                        target.wakeUp();
                        if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                        {
                            return null;
                        }
                    }
                }
                    break;
                case "default":
                {
                    if("true".equals(message.get("fromnetwork")))
                    {
                        if(!sendToClient(message))
                        {
                            return null;
                        }
                    }
                    else
                    {
                        return null;
                    }
                }
                    break;
                case "say":
                {
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "register":
                {
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "identify":
                {
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "nick":
                {
                    CommandExternalWorker external = commandServer.getActiveExternalConnexions(message.get("user"));
                    commandServer.addToActiveExternalConnexions(message.get("arg0"), external);
                    commandServer.removeFromActiveExternalConnexions(message.get("user"));
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "quit":
                {
                    commandServer.removeFromActiveExternalConnexions(message.get("user"));
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                case "part":
                {
                    if(!sendToClient(NetworkMessage.generateSuccessMessage(commandId)))
                    {
                        return null;
                    }
                }
                    break;
                default:
                    return NetworkMessage.generateErrorMessage(0, commandId);
                    
            }
        }
        return message;
    }
    
    /**
     * Méthode faisant suivre le message au suivant sur dans le cycle de vie
     * @param message 
     */
    private void sendInNetwork(Map<String, String> message)
    {
         ZMQ.Socket speaker = context.createSocket(ZMQ.REQ);
        //on se connecte au au suivant pour qu'il complete l'objet
        speaker.connect(commandServer.getComConfiguration().get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
        //on lui passe le message
        speaker.send(new JSONSerializer().serialize(message),0);
        speaker.recv(0);
        //on ferme la connexion (on a pas besoin de sa réponse)
        speaker.close();
    }
    
    
    /**
     * méthode envoyant le message passé a l'argument au command id compris dans el message.
     * @param message
     * @return true si la personne a été trouvé et false sinon
     */
    private boolean sendToClient(Map<String, String> message)
    {
        CommandExternalWorker external = commandServer.getWaitingCommand(message.get("commandid"));
        if (external != null)
        {
            external.setMessage(message);
            external.wakeUp();
            commandServer.removeFromWaitingCommands(message.get("commandid"));
            return true;
        }
        return false;
    }
}
