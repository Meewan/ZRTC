/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.output;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import fr.meewan.zrtc.network.Proxy;
import fr.meewan.zrtc.utils.NetworkMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

/**
 * Implémentation réelle de la logique de controle du module de commande dans un
 * serveur multi-threadé
 * @author Meewan
 */
public class OutputServer extends Thread
{
    private OutputConfiguration configuration;
    private ZContext coreContext;
    private ZContext edgeContext;
    private boolean stop = true;//champ indiquant si l'arret du serveur a été demandé
    private Map<String, String> comConfiguration;
    private static final Logger logger = Logger.getLogger(OutputServer.class.getName());
    private Proxy edgeProxy;
    private Proxy internalProxy;
    private Socket publisher;
    private Socket internalInput;
    private Socket internalOutput;
    private HashMap<String, VirtualClient> clients = new HashMap<>();
    private String msgDelimiter = "#";
    private ArrayList<OutputWorker> workers = new ArrayList<OutputWorker>();

    public OutputServer() 
    {
       
        this.stop = false;
        coreContext = new ZContext();
        edgeContext = new ZContext();
    }

    @Override
    public void run()
    {
        logger.log(Level.INFO, "lancement du serveur d'output");
        loadNetworkConfiguration();
        
        logger.log(Level.INFO, "démarrage du proxy (output)");
        edgeProxy = new Proxy("tcp://*:" + configuration.getExternalPort(), "inproc://outputProxy", edgeContext);
        edgeProxy.start();
    	//edgeProxy = new Proxy("tcp://*:" + configuration.getExternalPort(), "tcp://127.0.0.1:5559");

        logger.log(Level.INFO, "démarrage des sockets de publication (réseau interne et intra-module) (output)");
        publisher = coreContext.createSocket(ZMQ.PUB);
        publisher.bind("inproc://outputPublisher");
        //publisher.bind("tcp://127.0.0.1:5558");
        internalOutput = coreContext.createSocket(ZMQ.PUSH);

        logger.log(Level.INFO, "démarrage du proxy input réseau interne (output)");
        internalProxy = new Proxy("tcp://" + configuration.getCoreAddress() + ":" + configuration.getCorePort(),"inproc://outputWorkers", coreContext);
        internalProxy.start();
        
        logger.log(Level.INFO, "démarrage des workers (output)");
        for(int i = 0; i < configuration.getNbWorkers(); i++)
        {
        	OutputWorker worker = new OutputWorker(coreContext, edgeContext, "inproc://outputWorkers", publisher,
        			comConfiguration, clients, msgDelimiter);
        	worker.start();
        	workers.add(worker);
        }
        
        stop = false;
        
        logger.log(Level.INFO, "démarrage des fonctionnalités (output)");
        while(!stop)
        {
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        logger.log(Level.WARNING, "mort du serveur d'output");
    }
    
    
    /**
     * recharge la configuration du réseau pour savoir a qui passer le suivant
     */
    public void loadNetworkConfiguration()
    {
        logger.log(Level.INFO, "Récuperation de la configuration du réseau serveur d'output");
        ZMQ.Socket speaker = edgeContext.createSocket(ZMQ.REQ);
        speaker.connect("tcp://"+ configuration.getConfigAddress() + ":" + configuration.getConfigPort());
        speaker.send("hello",0);
        byte[] reply = speaker.recv(0);
        logger.log(Level.FINE, "La configuration sérialisé fait " + reply.length + " byte de long");
        String serializedConfiguration = new String(reply);
        this.comConfiguration = new JSONDeserializer<HashMap<String,String>>().deserialize(serializedConfiguration);
        if(this.comConfiguration != null && this.comConfiguration.size() != 0)
        {
            logger.log(Level.INFO, "configuration du réseau reçu et déserialisé avec succès");
        }
        else
        {
            logger.log(Level.SEVERE, "Le réseau est vide ou l'objet n'a pas pu être déserialisé.");
        }
        speaker.close();
    }
    
    public OutputConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(OutputConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public void reload()
    {
    	if(stop)
    	{
    		return;
    	}
    	edgeProxy.restart("tcp://*:" + configuration.getExternalPort(), "inproc://outputProxy");
    	internalProxy.restart("tcp://" + configuration.getCoreAddress() + ":" + configuration.getCorePort(),
        		"inproc://outputWorkers");
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public Map<String, String> getComConfiguration() {
        return comConfiguration;
    }
    
    private void sendInNetwork(Map<String, String> message)
    {
        //on se connecte au au suivant pour qu'il complete l'objet
    	internalOutput.connect(getComConfiguration().get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
        //on lui passe le message
    	internalOutput.send(new JSONSerializer().serialize(message),0);
        internalOutput.recv(0);
        //on ferme la connexion (on a pas besoin de sa réponse)
    	internalOutput.disconnect(getComConfiguration().get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
    }
}
