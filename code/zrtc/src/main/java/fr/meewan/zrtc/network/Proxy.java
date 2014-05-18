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
    private final ZContext frontContext;
    private final ZContext backContext;
    private Socket frontend;
    private Socket backend;
    

    public Proxy(String frontEndAdress, String backEndAdress) 
    {
        this.frontEndAdress = frontEndAdress;
        this.backEndAdress = backEndAdress;
        frontContext = new ZContext();
        backContext = frontContext;
        this.start();
    }

    public Proxy(String frontEndAdress, String backEndAdress, ZContext ctx) 
    {
        this.frontEndAdress = frontEndAdress;
        this.backEndAdress = backEndAdress;
        frontContext = ctx;
        backContext = ctx;
        this.start();
    }

    public Proxy(String frontEndAdress, ZContext ctx1, String backEndAdress, ZContext ctx2) 
    {
        this.frontEndAdress = frontEndAdress;
        this.backEndAdress = backEndAdress;
        frontContext = ctx1;
        backContext = ctx2;
        this.start();
    }
    

    @Override
    public void run()
    {
        frontend = frontContext.createSocket(ZMQ.ROUTER);
        frontend.bind(frontEndAdress);

        backend = backContext.createSocket(ZMQ.DEALER);
        backend.bind(backEndAdress);
        
        ZMQ.proxy(frontend, backend, null);
    }
    
    public Proxy restart(String frontEndAddress, String backEndAddress)
    {
    	frontContext.destroySocket(frontend);
    	backContext.destroySocket(backend);
    	return new Proxy(frontEndAddress, frontContext, backEndAddress, backContext);
    }
}
