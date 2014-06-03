/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission;

import flexjson.JSONDeserializer;
import fr.meewan.zrtc.module.permission.Cache.ChanCache;
import fr.meewan.zrtc.module.permission.Cache.ChanCacheImpl;
import fr.meewan.zrtc.module.permission.Cache.UserCache;
import fr.meewan.zrtc.module.permission.Cache.UserCacheImpl;
import fr.meewan.zrtc.module.permission.business.ListRefCommand;
import fr.meewan.zrtc.network.Proxy;
import java.io.File;
import java.io.IOException;
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
    private final String INTERNAl_COM_ADRESS = "inproc://internam_permission_module_communication";
    private final String CONFIG_PATH = "config" + File.separator + "permission.ini";
    private PermissionConfiguration configuration;
    private Map<String, String> comConfiguration; 
    private final UserCache userCache;
    private final ChanCache chanCache;
    private final Map<String, Boolean> defaultRightMap;
    private static final Logger logger = Logger.getLogger(PermissionServer.class.getName());
    private List<PermissionWorker> workers;
    private boolean stop = false;
    private ZContext context;
    private Proxy proxy;

    public PermissionServer() 
    {
        defaultRightMap = new HashMap<>();
        try 
        {
            configuration = new PermissionConfiguration(CONFIG_PATH);
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        for(String command : configuration.getCommands().keySet())
        {
            defaultRightMap.put(command, configuration.getCommands().get(command).toLowerCase().equals("true"));
        }
        userCache = new UserCacheImpl(configuration.getAdminUser(), configuration.getAdminPassword());
        chanCache = new ChanCacheImpl();   
        workers = new ArrayList<>();
    }
    
    @Override
    public void run()
    {
        context = new ZContext();
        logger.log(Level.INFO, "Chargement des données du réseau");
        loadNetworkConfiguration();
        logger.log(Level.INFO, "Lancement du proxy pour les workers");
        proxy = new Proxy("tcp://*:" + configuration.getListeningPort(), INTERNAl_COM_ADRESS, context);
        logger.log(Level.INFO, "verification des commandes enregistré en base");
        checkCommandInBase();
        
        for (int i = 0; i < configuration.getMaxWorkers(); i++)
        {
            workers.add(new PermissionWorker(this, context));
            workers.get(i).run();
        }
        //on garde en vie
        while (!stop)
        {
            synchronized(this)
            {
                try {
                    this.wait(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        logger.log(Level.WARNING, "mort du serveur de Permission");
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
        catch (ClassNotFoundException e) 
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, "toto", e);
            return ;
        }
        catch (SQLException e)
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, "tata", e);
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

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public String getINTERNAl_COM_ADRESS() {
        return INTERNAl_COM_ADRESS;
    }

    public Map<String, String> getComConfiguration() {
        return comConfiguration;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public ChanCache getChanCache() {
        return chanCache;
    }

    public Map<String, Boolean> getDefaultRightMap() {
        return defaultRightMap;
    }
    
}
