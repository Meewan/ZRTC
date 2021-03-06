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
    private final List<PermissionWorker> workers;
    private boolean stop = false;
    private ZContext context;
    private Proxy proxy;
    private Connection connection;

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
        try 
        {
            connection = DriverManager.getConnection("jdbc:mysql://" + configuration.getSqlAdress() + ":" + configuration.getSqlPort() + "/" + configuration.getSqlDb() + "?autoreconnect=true" , configuration.getSqlUser(), configuration.getSqlPassword());
        } 
        catch (SQLException e)
        {
            Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, "SQLException", e);
        }
       
        userCache = new UserCacheImpl(configuration.getAdminUser(), configuration.getAdminPassword(), connection);
        chanCache = new ChanCacheImpl(connection);  
       
        workers = new ArrayList<>();
    }
    
    @Override
    public void run()
    {
        logger.log(Level.INFO, "------------------- Lancement du module de commande commencé");
        context = new ZContext();
        logger.log(Level.INFO, "Chargement des données du réseau");
        loadNetworkConfiguration();
        logger.log(Level.INFO, "Lancement du proxy pour les workers");
        proxy = new Proxy("tcp://*:" + configuration.getListeningPort(), INTERNAl_COM_ADRESS, context);
        proxy.start();
        //on laisse le temps au proxy de démarer
        synchronized(this)
        {
            try 
            {
                this.wait(500);
            }
            catch (InterruptedException ex) 
            {
                Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        logger.log(Level.INFO, "verification des commandes enregistré en base");
        checkCommandInBase(connection);
        
        for (int i = 0; i < configuration.getMaxWorkers(); i++)
        {
            PermissionWorker worker = new PermissionWorker(this, context);
            worker.start();
            workers.add(worker);
        }
        //on garde en vie
        logger.log(Level.INFO, "------------------- Lancement du module de permission termine");
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
    private void checkCommandInBase(Connection connection)
    {
        
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
        logger.log(Level.INFO, "creation de la liste des commandes en base");
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
                        catch (SQLException ex) 
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
                logger.log(Level.INFO, "{0}n''est pas en base, on l''ajoute", command);
                ListRefCommand newCommand = new ListRefCommand(command, configuration.getCommands().get(command));
                try 
                {
                    newCommand.persist(connection);
                } 
                catch (SQLException ex) 
                {
                    Logger.getLogger(PermissionServer.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }
        }
        logger.log(Level.INFO, "fin de la verification des commandes en base");
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
