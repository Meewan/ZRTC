/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.network;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import fr.meewan.zrtc.com.ComConfiguration;
import fr.meewan.zrtc.com.Message;
import java.util.List;
import java.util.Map;
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

    public CoreWorker(String address, Map<String, List<String>> commands, int coreConfigListeningPort) 
    {
        this.commands = commands;
        this.address = address;
        this.coreConfigListeningPort = coreConfigListeningPort;
        
    }
    
    @Override
    public void run()
    {
        loadNetworkConfiguration();
        Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        ZMQ.Socket speaker;
        socket.connect (address);
        while (true) 
        {
            String request = socket.recvStr (0);
            Message message = new JSONDeserializer<Message>().deserialize( request );
            if(message.command != null && ! message.command.equals(""))
            {
                message.lifeCycle = commands.get(message.command);
                message.state = 0;
                String answer = new JSONSerializer().serialize(message);
                speaker = context.socket(ZMQ.REQ);
                //on se connecte au suivant
                speaker.connect(comConfiguration.get(message.lifeCycle.get(message.state)));
                //on lui passe le message
                speaker.send(answer,0);
                //on ferme la connexion (on a pas besoin de sa réponse)
                speaker.close();
            }
            
        }
        //context.term();
    }

    /**
     * recharge la configuration du réseau pour savoir a qui passer le suivant
     */
    public void loadNetworkConfiguration()
    {
        Context context = ZMQ.context(1);
        ZMQ.Socket speaker = context.socket(ZMQ.REQ);
        speaker.connect("tcp://localhost:"+coreConfigListeningPort);
        speaker.send("hello",0);
        byte[] reply = speaker.recv(0);
        ComConfiguration tmp = new JSONDeserializer<ComConfiguration>().deserialize(new String(reply));
        this.comConfiguration = tmp.modulList;
        speaker.close();
        context.term();
    }
    public void setCommands(Map<String, List<String>> commands) {
        this.commands = commands;
    }
}
