/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission;

import flexjson.JSONDeserializer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author Meewan
 */
class PermissionWorker implements Runnable
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
            try 
            {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            Connection connection;
            try {
                connection = DriverManager.getConnection("jdbc:mysql:" + permissionServer.getConfiguration().getSqlAdress() + ":" + permissionServer.getConfiguration().getSqlPort() , permissionServer.getConfiguration().getSqlUser(), permissionServer.getConfiguration().getSqlPassword());
            
            //on vérifie les droits particuliers du chan
            right = permissionServer.getChanCache().getChanPermission(chan, command, connection);
            connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
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
        try 
        {
            Class.forName("com.mysql.jdbc.Driver");
        } 
        catch (ClassNotFoundException ex)
        {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        Connection connection;
        try 
        {
            connection = DriverManager.getConnection("jdbc:mysql:" + permissionServer.getConfiguration().getSqlAdress() + ":" + permissionServer.getConfiguration().getSqlPort() , permissionServer.getConfiguration().getSqlUser(), permissionServer.getConfiguration().getSqlPassword());
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        if(permissionServer.getUserCache().ConnectUser(message.get("user"), message.get("pgpkey"), connection))
        {
            message.put("uid", permissionServer.getUserCache().getUid(message.get("user")));
        }
        else
        {
            message.put("authorized", "false");
        }
        try 
        {
            connection.close();
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    private void register(Map<String, String> message) 
    {
        try 
        {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:mysql:" + permissionServer.getConfiguration().getSqlAdress() + ":" + permissionServer.getConfiguration().getSqlPort() , permissionServer.getConfiguration().getSqlUser(), permissionServer.getConfiguration().getSqlPassword());
        } catch (SQLException ex) {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        if(!permissionServer.getUserCache().registerUser(message.get("user"), message.get("arg0"), connection))
        {
            message.put("authorized", "false");
        }
        try 
        {
            connection.close();
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
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
        try 
        {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:mysql:" + permissionServer.getConfiguration().getSqlAdress() + ":" + permissionServer.getConfiguration().getSqlPort() , permissionServer.getConfiguration().getSqlUser(), permissionServer.getConfiguration().getSqlPassword());
        } catch (SQLException ex) {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        if(! permissionServer.getUserCache().changeUserName(message.get("user"), message.get("arg0"), connection))
        {
            message.put("authorized", "false");
        }
        try 
        {
            connection.close();
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void quit(Map<String, String> message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void mode(Map<String, String> message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void joinChan(Map<String, String> message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void part(Map<String, String> message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
