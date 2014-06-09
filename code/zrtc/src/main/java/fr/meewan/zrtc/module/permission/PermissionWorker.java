/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author Meewan
 */
class PermissionWorker extends Thread
{
    private PermissionServer permissionServer;
    private boolean stop = false;
    private ZContext context;

    public PermissionWorker(PermissionServer permissionServer, ZContext context) 
    {
        this.permissionServer = permissionServer;
        this.context = context;
    }

    @Override
    public void run() 
    {
        //initialisation du réseau
        ZMQ.Socket socket = context.createSocket(ZMQ.REP);
        socket.connect (permissionServer.getINTERNAl_COM_ADRESS());
        while(! this.stop)
        {
            //écoute d'une demande
            String rawMessage = socket.recvStr (0);
            Map<String,String> message = new JSONDeserializer<HashMap>().deserialize(rawMessage);
            socket.send("ok", 0);
            
            //les deux tratements possibles
            //le message est validé et on execute els traitements
            if(message.get("correctsignature") != null && message.get("correctsignature").toLowerCase().equals("true") && 
                    message.get("authorized") != null && message.get("authorized").toLowerCase().equals("true"))
            {
                execute(message);
            }
            else if(message.get("authorized") == null)// si personne n'a géré les authorisations pour ce message
            {
                message.put("authorized", resolvePermission(message)? "true" : "false");
                if(!message.get("command").toLowerCase().equals("connect"))
                {
                    message.put("pgpkey", permissionServer.getUserCache().getPgpKey(message.get("user")));
                }
                else
                {
                    message.put("pgpkey", message.get("arg0"));
                }
            }
            //on passe le message au suivant
            message.put("state", ((Integer)(Integer.parseInt(message.get("state") +1))).toString());
            if(Integer.parseInt(message.get("state")) <= Integer.parseInt(message.get("lifecyclestates")))
            {
               sendInNetwork(message);
            }
            
        }
    }
    
    
    /**
     * méthode gérant l'execution des commandes contenu dans le message si besoin
     * @param message 
     */
    private void execute(Map <String, String> message)
    {
        String command = message.get("command").toLowerCase();
        switch(command)
        {
            case "connect":
                connect(message);
                break;
            case "register":
                register(message);
                break;
            case "identify":
                identify(message);
                break;
            case "nick":
                nick(message);
                break;
            case "quit":
                quit(message);
                break;
            case "mode":
                mode(message);
                break;
            case "join":
                joinChan(message);
                break;
            case "part":
                part(message);
                break;
        }
    }
    /**
     * methode retournant true si l'opération est authorisé par les droits et 
     * false sinon
     * @param message
     * @return 
     */
    private boolean resolvePermission(Map<String, String> message)
    {
        String user = message.get("user");
        String command = message.get("command").toLowerCase();
        if(isChannelCommand(command))
        {
            String chan = extractChan(message);
            //on vérifie si le user a un droit spécifique
            Boolean right = permissionServer.getUserCache().getUserPermission(user, chan, command);
            if(right != null)
            {
                return right;
            }
            //on vérifie les droits particuliers du chan
            right = permissionServer.getChanCache().getChanPermission(chan, command);
            if(right != null)
            {
                return right;
            }
            //on vérifie les droits généraux
            right = permissionServer.getDefaultRightMap().get(command);
            if(right != null)
            {
                return right;
            }
            return false;
        }
        else
        {
            //on vérifie si le user a un droit spécifique
            Boolean right = permissionServer.getUserCache().getUserPermission(user, command);
            if(right != null)
            {
                return right;
            }
            //on vérifie les droits généraux
            right = permissionServer.getDefaultRightMap().get(command);
            if(right != null)
            {
                return right;
            }
            return false;
        }
    }
    
    /**
     * methode retournant true si la commande concerne un chan et false sinon
     * @param message
     * @return 
     */
    private boolean isChannelCommand(String command)
    {
        if("mode".equals(command) || 
           "join".equals(command) || 
           "part".equals(command) ||
           "say".equals(command))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * méthode extrayant le nom du chan ciblé par une commande selon la commande
     * @param message
     * @return 
     */
    private String extractChan(Map<String, String> message)
    {
        switch(message.get("command").toLowerCase())
        {
            case "mode":
            {
                return message.get("arg0");
            }
            case "join":
            {
                return message.get("arg0");
            }
            case "say":
            {
                return message.get("arg0");
            }
            case "part":
            {
                return message.get("arg0");
            }
            default:
                return null;
        }
    }

    private void connect(Map<String, String> message) 
    { 
        if(permissionServer.getUserCache().ConnectUser(message.get("user"), message.get("pgpkey")))
        {
            message.put("uid", permissionServer.getUserCache().getUid(message.get("user")));
        }
        else
        {
            message.put("authorized", "false");
        }
    }

    private void register(Map<String, String> message) 
    {
        if(!permissionServer.getUserCache().registerUser(message.get("user"), message.get("arg0")))
        {
            message.put("authorized", "false");
        }
    }

    private void identify(Map<String, String> message) 
    {
        if(! permissionServer.getUserCache().checkPassword(message.get("user"), message.get("arg0")))
        {
            message.put("authorized", "false");
        }
    }

    private void nick(Map<String, String> message) 
    { 
        if(! permissionServer.getUserCache().changeUserName(message.get("user"), message.get("arg0")))
        {
            message.put("authorized", "false");
        }
    }

    private void quit(Map<String, String> message) 
    {
        String user = message.get("user");
        
        //recuperation des chans ou l'utilisateur était inscrit
        List<String> chans = permissionServer.getUserCache().getAllChanForUser(user);
        //destruction de la session utilisateur
        permissionServer.getUserCache().UnconnectUser(user);
        //deconnection de tout les chans auxquels était connecté l'utilisateur
        for(String chan : chans)
        {
            permissionServer.getChanCache().revoveUserFromChan(user, chan);
        }
    }

    private void mode(Map<String, String> message) 
    {
        String type = message.get("arg0");
        int argc = Integer.parseInt(message.get("argc"));
        if (type == null || argc < 4)
        {
            message.put("authorized", "false");
            return;
        }
        if(type.toLowerCase().equals("user"))
        {
            //cas ou on change les droits d'un utilisateur sur un chan
            if (argc == 5)
            {
                String user = message.get("arg2");
                String command = message.get("arg3");
                String rightString = message.get("arg4");
                Boolean right = internalStringToBoolean(rightString);
                String chan = message.get("arg1");
                if(!permissionServer.getUserCache().setUserCommand(user, chan, command, right))
                {
                    message.put("authorized", "false");
                }
            }
            else//cas ou c'est une commande générique
            {
                String user = message.get("arg1");
                String command = message.get("arg2");
                Boolean right = internalStringToBoolean(message.get("arg3"));
                if(!permissionServer.getUserCache().setUserCommand(user, command, right))
                {
                    message.put("authorized", "false");
                }
            }
        }
        else if(type.toLowerCase().equals("chan"))//changer les droits d'un chan
        {
            String chan = message.get("arg1");
            String command = message.get("arg2");
            Boolean right = internalStringToBoolean(message.get("arg3"));
            if(!permissionServer.getChanCache().setChanPermission(chan, command, right))
            {
                message.put("authorized", "false");
            }
        }
    }

    private void joinChan(Map<String, String> message) 
    {
       String user = message.get("user");
       String chan = message.get("arg0");
       
       if(permissionServer.getUserCache().addUserTochan(user, chan))
       {
            if(!permissionServer.getChanCache().addUserToChan(user, chan))
            {
                message.put("authorized", "false");
            }
       }
       else
       {
           message.put("authorized", "false");
       }
    }

    private void part(Map<String, String> message) 
    {
        String user = message.get("user");
        String chan = message.get("arg0");
        if(permissionServer.getUserCache().removeUserFromchan(user, chan))
        {
            if(!permissionServer.getChanCache().revoveUserFromChan(user, chan))
            {
                message.put("authorized", "false");
            }
        }
        else
        {
            message.put("authorized", "false");
        }
    }
    
    /**
     * méthode convertissant une string entré en argument de "mode" en boolean 
     * utilisable avec les spec de l'interface de cache
     * @param right
     * @return 
     */
    private Boolean internalStringToBoolean(String right)
    {
        if (right == null)
        {
            return false;
        }
        if(right.toLowerCase().equals("true"))
        {
            return true;
        }
        if(right.toLowerCase().equals("false"))
        {
            return false;
        }
        return null;
    }
    /**
     * Méthode faisant suivre le message au suivant sur dans le cycle de vie
     * @param message 
     */
    private void sendInNetwork(Map<String, String> message)
    {
         ZMQ.Socket speaker = context.createSocket(ZMQ.REQ);
        //on se connecte au au suivant pour qu'il complete l'objet
        speaker.connect(permissionServer.getComConfiguration().get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
        //on lui passe le message
        speaker.send(new JSONSerializer().serialize(message),0);
        //on ferme la connexion (on a pas besoin de sa réponse)
        speaker.close();
    }
}
