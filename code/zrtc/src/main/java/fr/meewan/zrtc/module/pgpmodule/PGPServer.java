/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.pgpmodule;

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
public class PGPServer extends Thread
{
    private PGPConfiguration configuration;
    private ZContext ctx;
    private boolean stop = true;//champ indiquant si l'arret du serveur a été demandé
    private Map<String, String> comConfiguration;
    private static final Logger logger = Logger.getLogger(PGPServer.class.getName());
    private PGPProxy loadBalancer;
    private Socket publisher;
    private Socket internalInput;
    private Socket internalOutput;
    private String msgDelimiter = "#";
    private ArrayList<PGPWorker> workers = new ArrayList<PGPWorker>();

    public PGPServer() 
    {
       
        this.stop = false;
        ctx = new ZContext();
    }

    @Override
    public void run()
    {
        logger.log(Level.INFO, "lancement du serveur d'output");
        loadNetworkConfiguration();
        
        logger.log(Level.INFO, "démarrage du proxy (pgp)");
        String[] backBinds = {"inproc://pgpWorkers", "tcp://" + configuration.getSubAddress() + ":" + configuration.getSubPort()};
        loadBalancer = new PGPProxy(ctx, "tcp://" + configuration.getCoreAddress() + ":" + configuration.getCorePort(), backBinds);
        while(!loadBalancer.isReady())
        {
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        logger.log(Level.INFO, "proxy démarré (pgp)");

        logger.log(Level.INFO, "démarrage des workers (pgp)");
        for(int i = 0; i < configuration.getNbWorkers(); i++)
        {
        	PGPWorker worker = new PGPWorker(ctx, comConfiguration, "inproc://pgpWorkers");
        	worker.start();
        	workers.add(worker);
        }
        logger.log(Level.INFO, "workers démarrés (pgp)");

        logger.log(Level.INFO, "module PGP opérationel");
        
        while(!stop)
        {
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        logger.log(Level.WARNING, "mort du serveur PGP");
    }
    
    
    /**
     * recharge la configuration du réseau pour savoir a qui passer le suivant
     */
    public void loadNetworkConfiguration()
    {
        logger.log(Level.INFO, "Récuperation de la configuration du réseau serveur d'output");
        ZMQ.Socket speaker = ctx.createSocket(ZMQ.REQ);
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
    
    public PGPConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PGPConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public void reload()
    {
    	if(stop)
    	{
    		return;
    	}
    	String[] backBinds = {"inproc://pgpWorkers", "tcp://" + configuration.getSubAddress() + ":" + configuration.getSubPort()};
    	loadBalancer.restart("tcp://" + configuration.getCoreAddress() + ":" + configuration.getCorePort(), backBinds);
    	ArrayList<PGPWorker> newList = new ArrayList<PGPWorker>();
    	for(PGPWorker wrk : workers)
    	{
    		PGPWorker newWrk = wrk.restart("inproc://pgpWorkers");
    		newWrk.start();
    		newList.add(newWrk);
    		wrk.setStop(true);
    	}
    	workers = newList;
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
        //on ferme la connexion (on a pas besoin de sa réponse)
    	internalOutput.disconnect(getComConfiguration().get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
    }
}
