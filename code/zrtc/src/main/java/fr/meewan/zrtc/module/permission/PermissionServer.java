/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission;

import flexjson.JSONDeserializer;
import fr.meewan.zrtc.module.permission.Cache.ChanCache;
import fr.meewan.zrtc.module.permission.Cache.UserCacheImpl;
import fr.meewan.zrtc.module.permission.business.ListRefCommand;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author Meewan
 */
public class PermissionServer extends Thread
{
    private PermissionConfiguration configuration;
    private Map<String, String> comConfiguration; 
    private UserCacheImpl userCache;
    private ChanCache cacheMap;
    private static final Logger logger = Logger.getLogger(PermissionServer.class.getName());
    
    private ZContext context;
    
    @Override
    public void run()
    {
        logger.log(Level.INFO, "Chargement des données du réseau");
        loadNetworkConfiguration();
        logger.log(Level.INFO, "verification des commandes enregistré en base");
        checkCommandInBase();

    }

    /**
     * recharge la configuration du réseau pour savoir a qui passer le suivant
     */
    public void loadNetworkConfiguration()
    {
        logger.log(Level.INFO, "Récuperation de la configuration du réseau serveur de commande");
        ZMQ.Socket speaker = context.createSocket(ZMQ.REQ);
        speaker.connect("tcp://"+ configuration.getConfigAddress() + ":" + configuration.getConfigPort());
        speaker.send("hello",0);
        byte[] reply = speaker.recv(0);
        logger.log(Level.FINE, "La configuration s\u00e9rialis\u00e9 fait {0} byte de long", reply.length);
        String serializedConfiguration = new String(reply);
        this.comConfiguration = new JSONDeserializer<HashMap>().deserialize(serializedConfiguration);
        if(this.comConfiguration != null && !this.comConfiguration.isEmpty())
        {
            logger.log(Level.INFO, "configuration du réseau reçut et deserialisé avec succes");
        }
        else
        {
            logger.log(Level.SEVERE, "Le réseaue st vide ou l'objet n'a pas pu être déserialisé.");
        }
        speaker.close();
    }
    public PermissionConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PermissionConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Méthode vérifiant les commandes qui sont en base et les ajoutant si besoin
     * @return 
     */
    private void checkCommandInBase()
    {
        java.sql.Connection connection = null;
        try 
        {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql:" + configuration.getSqlAdress() + ":" + configuration.getSqlPort() , configuration.getSqlUser(), configuration.getSqlPassword());
        } 
        catch (Exception e) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, e);
            return ;
        }
        Statement statement = null;
        try 
        {
            statement = connection.createStatement();
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
            return ;
        }
        ResultSet rs = null;
        try 
        {
            rs = statement.executeQuery("SELECT * FROM list_ref_command");
        }
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        List<ListRefCommand> commandList = new ArrayList<>();
        try 
        {
            while(rs.next())
            {
                commandList.add(new ListRefCommand(rs));//construction de la liste des commandes en base
            }
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        for(String command : configuration.getCommands().keySet())
        {
            Boolean inbase = false;
            for(ListRefCommand dbCommand : commandList)
            {
                if(command.equals(dbCommand.getCommandLabel()))
                {
                    if(!dbCommand.getDefaultRight().equals(configuration.getCommands().get(command)))
                    {
                        dbCommand.setDefaultRight(configuration.getCommands().get(command));
                        try 
                        {
                            dbCommand.persist(connection);
                        } 
                        catch (Exception ex) 
                        {
                            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
                            return;
                        }
                    }
                    inbase = true;
                    break;
                }
            }
            if(!inbase)// si la commande est dans le fichier de conf et pas en base, on l'ajoute
            {
                ListRefCommand newCommand = new ListRefCommand(command, configuration.getCommands().get(command));
                try 
                {
                    newCommand.persist(connection);
                } 
                catch (Exception ex) 
                {
                    Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }
        }
        
        try 
        {
            connection.close();
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
