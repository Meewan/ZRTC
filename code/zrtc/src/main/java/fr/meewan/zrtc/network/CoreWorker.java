/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.network;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
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

    public CoreWorker(String address, Map<String, List<String>> commands) 
    {
        this.commands = commands;
        this.address = address;
    }
    
    @Override
    public void run()
    {
        Context context = ZMQ.context(1);
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        socket.connect (address);
        while (true) 
        {
            String request = socket.recvStr (0);
            Message message = new JSONDeserializer<Message>().deserialize( request );
            if(message.command != null && ! message.command.equals(""))
            {
                message.lifeCycle = commands.get(message.command);
                message.state = 0;
            }
            String answer = new JSONSerializer().serialize(message);
            socket.send(answer, 0);
        }
    }

    public void setCommands(Map<String, List<String>> commands) {
        this.commands = commands;
    }
}
