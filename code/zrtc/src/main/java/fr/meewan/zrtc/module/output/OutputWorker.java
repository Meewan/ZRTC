package fr.meewan.zrtc.module.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Socket;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import fr.meewan.zrtc.utils.NetworkMessage;

public class OutputWorker extends Thread
{
	private ZContext coreContext;
	private ZContext edgeContext;
	private boolean stop = false;//champ indiquant si l'arret du serveur a été demandé
	private Map<String, String> comConfiguration;
	private static final Logger logger = Logger.getLogger(OutputServer.class.getName());
	private Socket socket;
	private Socket pub;
	private Socket internalOutput;
	private Map<String, VirtualClient> clients;
	private String msgDelimiter;
	
	public OutputWorker(ZContext coreContext, ZContext edgeContext, String socketBind, Socket pub, Map<String,String> comConfiguration, Map<String, VirtualClient> clients, String msgDelimiter) 
	{
	   this.coreContext = coreContext;
	   this.edgeContext = edgeContext;
	   this.pub = pub;
	   this.comConfiguration = comConfiguration;
	   this.clients = clients;
	   this.msgDelimiter = msgDelimiter;
	   
	   this.socket = this.coreContext.createSocket(ZMQ.REP);
	   this.socket.connect(socketBind);
	   
	   this.internalOutput = this.coreContext.createSocket(ZMQ.REQ);
	}
	
	@Override
	public void run()
	{
	    while(!stop)
	    {
	    	ZMsg msg = ZMsg.recvMsg(socket);
	    	socket.send("ok");
	    	
	    	if(msg.size() == 1)
	    	{
	    		Map<String,String> msgMap = new JSONDeserializer<HashMap<String,String>>().deserialize(new String(msg.getFirst().getData()));
	    		
	    		if(msgMap.get("authorized").equals("true") && msgMap.get("correctsignature").equals("true"))
	    		{
	    			execute(msgMap);
	    		}
	    		//gestion du cycle de vie
                        Integer state = Integer.parseInt(msgMap.get("state"));
                        state ++;
                        msgMap.put("state", state.toString());
                        if(state <= Integer.parseInt(msgMap.get("lifecyclestates")))
                        {
                            sendInNetwork(msgMap);
                        }
	    	}
	    }
	    
	    logger.log(Level.WARNING, "mort du serveur d'output");
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
        String uId = message.get("uid");
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
                    	clients.put(uId, new VirtualClient(coreContext, edgeContext, uId, args.get(0)));
                    }
                	
                	ZMsg msg = new ZMsg();
                	msg.add(args.get(0));
                	String content = 
                			DatatypeConverter.printBase64Binary("info".getBytes()) + msgDelimiter +
                			DatatypeConverter.printBase64Binary((user + " has joined the channel").getBytes());
                	msg.add(content);
                	msg.send(pub);
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
                	msg.send(pub);
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
                    		tmp.send(pub);
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
    	internalOutput.connect(comConfiguration.get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
        //on lui passe le message
    	internalOutput.send(new JSONSerializer().serialize(message),0);
        internalOutput.recv();
        //on ferme la connexion (on a pas besoin de sa réponse)
    	internalOutput.disconnect(comConfiguration.get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
    }
}
