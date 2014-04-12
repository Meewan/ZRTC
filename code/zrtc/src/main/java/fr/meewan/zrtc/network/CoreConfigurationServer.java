/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.network;

import flexjson.JSONSerializer;
import fr.meewan.zrtc.com.ComConfiguration;
import org.zeromq.ZMQ;

/**
 *Thread distribuant la configuration du réseau a tout ceux qui s'y connecterons et enverons une donnée quelconque.
 * @author Meewan
 */
public class CoreConfigurationServer extends Thread
{
    private String comconfiguration;
    private final String listeningAddress;

    //surcharge du constructeur a des fin esthetique
    public CoreConfigurationServer(ComConfiguration comconfiguration, Integer listeningPort) 
    {
        this(comconfiguration, listeningPort.toString());
    }
    
    public CoreConfigurationServer(ComConfiguration comconfiguration, String listeningPort) 
    {
        this.listeningAddress = "tcp://*:"+listeningPort;
        JSONSerializer s = new JSONSerializer();
        this.comconfiguration = s.serialize(comconfiguration);
    }
    
    @Override
    public void run()
    {
        ZMQ.Context context = ZMQ.context(1);

        // Socket to talk to clients
        ZMQ.Socket responder = context.socket(ZMQ.REP);
        responder.bind(listeningAddress);

        while (!Thread.currentThread().isInterrupted()) 
        {
            responder.recv(0);
            responder.send(comconfiguration.getBytes(), 0);
        }
        //on ne devrait jamais sortir de la boucle, mais au cas ou on fait le ménage
        responder.close();
        context.term();
    }

    public void setComconfiguration(ComConfiguration comconfiguration) 
    {
        JSONSerializer s = new JSONSerializer();
        this.comconfiguration = s.serialize(comconfiguration);
    }

}
