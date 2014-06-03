/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.network;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import fr.meewan.zrtc.utils.NetworkMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 *
 * @author Meewan
 */
public class CoreWorker extends Thread
{
    private Map <String, List<String>> commands;
    private String address;
    private Map<String,String> comConfiguration;
    private int coreConfigListeningPort;
    private ZContext context;

    public CoreWorker(String address, Map<String, List<String>> commands, int coreConfigListeningPort, ZContext context) 
    {
        this.commands = commands;
        this.address = address;
        this.coreConfigListeningPort = coreConfigListeningPort;
        this.context = context;
        
    }
    /**
     * gere les cycles de vie dans le réseau
     */
    @Override
    public void run()
    {
        loadNetworkConfiguration();
        ZMQ.Socket socket = context.createSocket(ZMQ.REP);
        ZMQ.Socket speaker;
        socket.connect (address);
        while (true) 
        {
            String request = socket.recvStr (0);
            socket.send("ok", 0);
            Map<String,String> message = new JSONDeserializer<HashMap>().deserialize( request );
            if(message.get("command") != null && ! message.get("command").equals("") && commands.get(message.get("command"))!= null)
            {
                List<String> lifeCycle = commands.get(message.get("command"));
                int i;
                for(i = 0; i< lifeCycle.size(); i++)
                {
                    message.put("lifecycle" + i, lifeCycle.get(i));
                }
                message.put("lifecyclestates", Integer.toString(i));
                message.put("state", "0");
                String answer = new JSONSerializer().serialize(message);
                speaker = context.createSocket(ZMQ.REQ);
                //on se connecte au suivant
                speaker.connect(comConfiguration.get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
                //on lui passe le message
                speaker.send(answer,0);
                //on ferme la connexion (on a pas besoin de sa réponse)
                speaker.close();
            }
            else
            {
                message = NetworkMessage.generateErrorMessage(5, message.get("commandid"));
                List<String> lifeCycle = commands.get(message.get("command"));
                int i;
                for(i = 0; i< lifeCycle.size(); i++)
                {
                    message.put("lifecycle" + i, lifeCycle.get(i));
                }
                message.put("lifecyclestates", Integer.toString(i));
                message.put("state", "0");
                String answer = new JSONSerializer().serialize(message);
                speaker = context.createSocket(ZMQ.REQ);
                //on se connecte au suivant
                speaker.connect(comConfiguration.get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
                //on lui passe le message
                speaker.send(answer,0);
                //on ferme la connexion (on a pas besoin de sa réponse)
                speaker.close();
            }
            
        }
        //socket.close();
    }

    /**
     * recharge la configuration du réseau pour savoir a qui passer le suivant
     */
    public void loadNetworkConfiguration()
    {
        ZMQ.Socket speaker = context.createSocket(ZMQ.REQ);
        speaker.connect("tcp://localhost:"+coreConfigListeningPort);
        speaker.send("hello",0);
        byte[] reply = speaker.recv(0);
        this.comConfiguration = new JSONDeserializer<HashMap>().deserialize(new String(reply));
        speaker.close();
    }
    public void setCommands(Map<String, List<String>> commands) {
        this.commands = commands;
    }
}
