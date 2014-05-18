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
    private Socket publisher;
    private Socket internalInput;
    private Socket internalOutput;
    private HashMap<String, VirtualClient> clients = new HashMap<>();
    private String msgDelimiter = "#";

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
    	//edgeProxy = new Proxy("tcp://*:" + configuration.getExternalPort(), "tcp://127.0.0.1:5559");

        logger.log(Level.INFO, "démarrage des sockets de publication (réseau interne et intra-module) (output)");
        publisher = coreContext.createSocket(ZMQ.PUB);
        publisher.bind("inproc://outputPublisher");
        //publisher.bind("tcp://127.0.0.1:5558");
        internalOutput = coreContext.createSocket(ZMQ.PUSH);

        logger.log(Level.INFO, "démarrage de la socket input réseau interne (output)");
        internalInput = coreContext.createSocket(ZMQ.PULL);
        internalInput.bind("tcp://" + configuration.getCoreAddress() + ":" + configuration.getCorePort());
        
        stop = false;

        logger.log(Level.INFO, "démarrage des fonctionnalités (output)");
        while(!stop)
        {
        	ZMsg msg = ZMsg.recvMsg(internalInput);
        	System.out.println("Received message: " + msg.size());
        	
        	if(msg.size() == 1)
        	{
        		Map<String,String> msgMap = new JSONDeserializer<HashMap<String,String>>().deserialize(msg.getFirst().toString());
        		
        		if(msgMap.get("authorized").equals("true") && msgMap.get("correctsignature").equals("true"))
        		{
        			execute(msgMap);
        		}
        		sendInNetwork(msgMap);
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
    	coreContext.destroySocket(internalInput);
    	internalInput = coreContext.createSocket(ZMQ.PULL);
    	internalInput.bind("tcp://" + configuration.getCoreAddress() + ":" + configuration.getCorePort());
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
    
    /**
     * Méthode exécutant le traitement à faire et vérifiant les droits.
     * @param message
     * @return 
     */
    private Map<String, String> execute(Map <String, String> message)
    {
        String command = message.get("command");
        boolean authorized = message.get("authorized") != null && "true".equals(message.get("authorized"));
        boolean correctsignature = message.get("correctsignature")!= null && "true".equals(message.get("correctsignature"));
        String commandId = message.get("commandid");
        String user = message.get("user");
        // En attendant la vrai variable
        String uId = user;
        // Nécessaire pour la commande nick??
        String oldUser = user;
        int state = Integer.parseInt(message.get("state")) + 1;
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
                case "join":
                {
                    if(clients.containsKey(uId))
                    {
                    	clients.get(uId).subscribe(args.get(0));
                    }
                    else
                    {
                    	clients.put(uId, new VirtualClient(coreContext, edgeContext, user, args.get(0)));
                    }
                	
                	ZMsg msg = new ZMsg();
                	msg.add(args.get(0));
                	String content = 
                			DatatypeConverter.printBase64Binary("info".getBytes()) + msgDelimiter +
                			DatatypeConverter.printBase64Binary((user + " has joined the channel").getBytes());
                	msg.add(content);
                	msg.send(publisher);
                }
                    break;
                    
                case "say":
                {
                	ZMsg msg = new ZMsg();
                	msg.add(args.get(0));
                	String content = 
                			DatatypeConverter.printBase64Binary("message".getBytes()) + msgDelimiter +
                			DatatypeConverter.printBase64Binary(user.getBytes()) + msgDelimiter +
                			DatatypeConverter.printBase64Binary(args.get(1).getBytes());
                	msg.add(content);
                	msg.send(publisher);
                }
                    break;
                    
                case "nick":
                {
                	if(clients.containsKey(uId))
                    {
                    	ArrayList<String> chansToNotify = clients.get(uId).getChans();
                    	ZMsg msg = new ZMsg();
                    	String content = 
                    			DatatypeConverter.printBase64Binary("info".getBytes()) + msgDelimiter +
                    			DatatypeConverter.printBase64Binary((oldUser + " is now known as " + user).getBytes());
                    	msg.add(content);
                    	for(String chan : chansToNotify)
                    	{
                    		ZMsg tmp = msg.duplicate();
                    		tmp.addFirst(chan);
                    		tmp.send(publisher);
                    	}
                    }
                }
                    break;
                    
                case "part":
                {
                	if(clients.containsKey(uId))
                    {
                    	for(String chan : args)
                    	{
                    		VirtualClient client = clients.get(uId);
                    		if(client.getChans().contains(chan))
                    		{
                    			client.unsubscribe(chan);
                    			ZMsg msg = new ZMsg();
                    			msg.add(chan);
                            	String content = 
                            			DatatypeConverter.printBase64Binary("info".getBytes()) + msgDelimiter +
                            			DatatypeConverter.printBase64Binary((user + "has left the channel").getBytes());
                            	msg.add(content);
                    		}
                    	}
                    }
                }
                    break;
                    
                case "partall":
                {
                	if(clients.containsKey(user))
                    {
                    	VirtualClient client = clients.get(uId);
                		for(String chan : client.getChans())
                		{
                			client.unsubscribe(chan);
                			ZMsg msg = new ZMsg();
                			msg.add(chan);
                        	String content = 
                        			DatatypeConverter.printBase64Binary("info".getBytes()) + msgDelimiter +
                        			DatatypeConverter.printBase64Binary((user + "has left the channel").getBytes());
                        	msg.add(content);
                		}
                    }
                }
                    break;
                    
                case "quit":
                {
                	if(clients.containsKey(uId))
                    {
                    	VirtualClient client = clients.get(user);
                		for(String chan : client.getChans())
                		{
                			client.unsubscribe(chan);
                			ZMsg msg = new ZMsg();
                			msg.add(chan);
                        	String content = 
                        			DatatypeConverter.printBase64Binary("info".getBytes()) + msgDelimiter +
                        			DatatypeConverter.printBase64Binary((user + "has left the channel (" + args.get(0) + ")").getBytes());
                        	msg.add(content);
                		}
                		clients.remove(uId);
                		client.setStop(true);
                    }
                }
                    break;
                    
                default:
                    return NetworkMessage.generateErrorMessage(0, commandId);
                    
            }
        }
        return message;
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
