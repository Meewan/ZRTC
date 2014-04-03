/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.mavenproject1;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;

/**
 *
 * @author Meewan
 */
public class dumbThread extends Thread
{
    private ZMQ.Socket responder;
    public void run()
    {
        while (true)
        {
            try {
                System.out.println("new wait");
                synchronized(this)
                {
                    this.wait();
                }
            } catch (InterruptedException ex) {
                System.out.println("new interupt");
                Logger.getLogger(dumbThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("end wait");
            tata();
        }
    }
    public void toto(ZMQ.Socket responder)
    {
        this.responder = responder;
        this.notify();
    }
    public void tata()
    {
        synchronized(this)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(dumbThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // Send reply back to client
//        String reply = "World";
//        responder.send(reply.getBytes(), 0);
            
    }
}
