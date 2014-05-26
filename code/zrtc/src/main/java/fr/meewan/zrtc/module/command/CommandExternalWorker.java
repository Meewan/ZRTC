/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import flexjson.JSONSerializer;
import fr.meewan.zrtc.utils.NetworkMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;
import javax.xml.bind.DatatypeConverter;
import org.zeromq.ZContext;

/**
 * Implementation de la logique metier dans un thread pour la ogique 1 thread 
 * par connexion
 * @author Meewan
 */
public class CommandExternalWorker extends Thread
{
    public static final String DELIMITER = "#";
    private ZContext context;
    private CommandServer commandServer;
    private String rawCommand;
    private Map<String, String> message;
    private boolean sleep;
    private String commandId;

    public CommandExternalWorker(CommandServer commandServer, ZContext context) 
    {
        this.sleep = true;
        this.commandServer = commandServer;
        this.context = context;
    }
    
    
    @Override
    public void run()
    {
        //initialisation du réseau
        ZMQ.Socket socket = context.createSocket(ZMQ.REP);
        socket.connect (CommandServer.EXTERNAL_COM_ADRESS);
        //écoute d'une demande
        rawCommand = socket.recvStr (0);
        //des la reception d'une demande on créé un nouveau worker pour écouter le réseau
        (new CommandExternalWorker(commandServer, context)).start();
        
        //logique de traitement
        Map<String, String> inputMessage = parseMessage(rawCommand);
        generateCommandId(inputMessage.get("user"));
        //on enregistre la connexion aupres du server
        commandServer.addToWaitingCommands(commandId, this);
        int errCode = messageValid(inputMessage);
        if(errCode != 0)
        {
            sendError(socket, errCode);
        }
        else
        {
            ZMQ.Socket speaker = context.createSocket(ZMQ.REQ);
            //on se connecte au noyau pour qu'il complete l'objet
            speaker.connect(commandServer.getConfiguration().getCoreAddress() + ":" + commandServer.getConfiguration().getCorePort());
            //on lui passe le message
            speaker.send(new JSONSerializer().serialize(inputMessage),0);
            //on ferme la connexion (on a pas besoin de sa réponse)
            speaker.close();

            while (this.sleep)
            {
                try 
                {
                    this.wait();
                }
                catch (InterruptedException ex) 
                {
                    Logger.getLogger(CommandExternalWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            CommandLogic logic = new CommandLogic(this.message);
            if(!logic.isAuthorized())
            {
                sendError(socket, 2);// on envoit une erreur "forbiden"
            }
            else if(!logic.isCorrectSignature())
            {
                sendError(socket, 3);//on envoit une errue "signature invalide"
            }
            else// si on a le droit de le faire
            {
                logic = new CommandLogic(message);
                String answerMessage = logic.encode();
                socket.send(answerMessage.getBytes(), 0);
            }
           
        }
        socket.close();
    }
    /**
     * Méthode récupérant une suite de String en base64 et construisant un objet
     * Message (une hashmap) pour le rendre exploitable par le réseau
     * @param rawMessage le message brut récupéré
     * @return 
     */
    private Map<String,String> parseMessage(String rawMessage)
    {
        Map<String,String> message = new HashMap<>();
        //prasing du message
        String[] tmp = rawMessage.split(this.DELIMITER);
        message.put("user", new String(DatatypeConverter.parseBase64Binary(tmp[0]))); 
        message.put("command", new String(DatatypeConverter.parseBase64Binary(tmp[1]))); 
        //liste des arguments
        int i;
        for(i = 2; i < (tmp.length -1); i++)
        {
            message.put("arg" + (i - 2), new String(DatatypeConverter.parseBase64Binary(tmp[i])));
        }
        message.put("argc", ((Integer)(i -1)).toString());
        //on initialise un certain nombre de variables
        message.put("authorized", "false");
        message.put("correctsignature", "false");
        message.put("fromnetwork" , "false");
        //signature du clilent
        message.put("signature", tmp[tmp.length - 1]);
        return message;
    }
    
    
    
    /**
     * reveil le thread si il attend
     */
    public void wakeUp()
    {
        this.sleep = false;
        synchronized (this)
        {
            this.notify();
        }
    }
    /**
     * génere un id suposément unique pour cette commande
     * @param user 
     */
    private void generateCommandId(String user)
    {
        this.commandId = user + Math.random();
    }
    
    /**
     * vérifie si le message contien bien les camps minimaux a savoir une commande, un utilisateur et une signature et retourne le code d'erreur approprié si ce n'est aps le cas
     * @return 
     */
    private int messageValid(Map <String, String> message)
    {
        if(message.size() < 3)
        {
            return 1;
        }
        else if(message.get("user")  == null || message.get("user").length() == 0)
        {
            return 1;
        }
        else if(message.get("command") == null || message.get("command").length() == 0)
        {
            return 5;
        }
        else if (message.get("signature") == null || message.get("signature").length() == 0)
        {
            return 3;
        }
        else
        {
            return 0;
        }
    }

    public Map<String, String> generateErrorMesage(int reason, String commandId) 
    {
        return NetworkMessage.generateErrorMessage(reason, commandId);
    }
    
    /**
     * méthode envoyant au client les erreures
     * @param socket le socket auquel on répond
     * @param errCode code d'erreur a envoyer
     */
    private void sendError(ZMQ.Socket socket, int errCode)
    {
        CommandLogic logic = new CommandLogic(generateErrorMesage(errCode, message.get("commandid")));
        String answerMessage = logic.encode();
        socket.send(answerMessage.getBytes(), 0);
    }

    public void setMessage(Map<String, String> message) {
        this.message = message;
    }
    
    
    
}
