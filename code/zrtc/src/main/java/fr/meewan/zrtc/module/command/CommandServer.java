/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import flexjson.JSONDeserializer;
import fr.meewan.zrtc.network.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Implémentation réelle de la logique de controle du module de commande dans un
 * serveur multi-threadé
 * @author Meewan
 */
public class CommandServer extends Thread
{
    private CommandConfiguration configuration;
    private final ZContext context;
    final public static String EXTERNAL_COM_ADRESS = "inproc://command_external_com_adress";
    final public static String INTERNAL_COM_ADRESS = "inproc://command_internal_com_adress";
    private boolean stop;//champ indiquant si l'arret du serveur a été demandé
    private Map<String, String> comConfiguration;
    private final Map<String, CommandExternalWorker> activeExternalConnexions;
    private final Map<String, CommandExternalWorker> waitingCommands;
    private static final Logger logger = Logger.getLogger(CommandServer.class.getName());
    private List<CommandInternalWorker> internalWorkers;

    public CommandServer() 
    {
       
        this.stop = false;
        this.activeExternalConnexions = new ConcurrentHashMap<>();
        this.waitingCommands = new ConcurrentHashMap<>();
        context =  new ZContext();
    }

    @Override
    public void run()
    {
        logger.log(Level.INFO, "lancement du serveur de commande");
        loadNetworkConfiguration();
        logger.log(Level.INFO, "lancement du proxy pour le serveur de commande");
        Proxy proxy = new Proxy("tcp://*:" + configuration.getListeningPort(), EXTERNAL_COM_ADRESS, context);
        
        //on lance le premier thread, c'est qui qui lancera les autres petit a petit
        (new CommandExternalWorker(this, context)).start();
        //on lance les workers internes
        internalWorkers = new ArrayList<>();
        for(int i = 0; i < configuration.getMaxWorkers(); i++)
        {
            internalWorkers.add(new CommandInternalWorker(this, context));
            internalWorkers.get(i).start();
        }
        
        
        //boucle pour garder le thread actif (mais sans qu'il consomme trop de ressources)
        //TODO chercher une méthode plus elegante
        while (!stop)
        {
            synchronized(this)
            {
                try {
                    this.wait(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CommandServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        logger.log(Level.WARNING, "mort du serveur de commande");
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
    
    public CommandConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(CommandConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
    
    public void addToActiveExternalConnexions(String clientName, CommandExternalWorker externalConnexion)
    {
        this.activeExternalConnexions.put(clientName, externalConnexion);
    }
    public synchronized void removeFromActiveExternalConnexions(String clientName)
    {
        this.activeExternalConnexions.remove(clientName);
    }
    
    public CommandExternalWorker getWaitingCommand(String commandId)
    {
        return this.waitingCommands.get(commandId);
    }

    public CommandExternalWorker getActiveExternalConnexions(String user) 
    {
        return this.activeExternalConnexions.get(user);
    }
    
    public void addToWaitingCommands(String commandId, CommandExternalWorker externalConnexion)
    {
        this.activeExternalConnexions.put(commandId, externalConnexion);
    }
    public void removeFromWaitingCommands(String clientName)
    {
        this.activeExternalConnexions.remove(clientName);
    }

    public Map<String, String> getComConfiguration() {
        return comConfiguration;
    }
    
    
}
