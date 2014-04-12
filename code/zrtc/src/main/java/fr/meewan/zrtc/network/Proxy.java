/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.network;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 *classe gérant les proxy entre les REQS et RP de maniere universelle. Toujours utiliser dans un thread séparé.
 * @author Meewan
 */
public class Proxy extends Thread
{
    /**
     * attention aux tcp et aux inproc (protocoles)
     * @param frontEndAdress
     * @param backEndAdress 
     */
    private final String frontEndAdress;
    private final String backEndAdress;

    public Proxy(String frontEndAdress, String backEndAdress) 
    {
        this.frontEndAdress = frontEndAdress;
        this.backEndAdress = backEndAdress;
        this.start();
    }
    

    @Override
    public void run()
    {
        ZContext ctx = new ZContext();
        Socket frontend = ctx.createSocket(ZMQ.ROUTER);
        frontend.bind(frontEndAdress);

        Socket backend = ctx.createSocket(ZMQ.DEALER);
        backend.bind(backEndAdress);
        
        ZMQ.proxy(frontend, backend, null);
        
        //inutile mais au cas ou
        ctx.destroy();
        
    }
}
