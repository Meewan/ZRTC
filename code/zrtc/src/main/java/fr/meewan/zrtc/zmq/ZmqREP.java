/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.zmq;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;
/**
 *
 * @author Meewan
 */
public class ZmqREP extends Thread 
{
    protected Integer port;
    protected boolean alive;
    @Override
    public void run()
    {
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket responder = context.socket(ZMQ.REP);
        responder.bind("tcp://*:"+ port);
        while (this.alive) 
        {
            // Wait for next request from the client
            byte[] request = responder.recv(0);

            // Do some 'work'
            synchronized(this)
            {
                try 
                {
                    Thread.sleep(1000);
                } 
                catch (InterruptedException ex) 
                {
                    Logger.getLogger(ZmqREP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // Send reply back to client
            String reply = "World";
            responder.send(reply.getBytes(), 0);
        }
    }
}
