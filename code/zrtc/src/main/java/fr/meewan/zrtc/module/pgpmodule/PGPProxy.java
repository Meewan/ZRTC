package fr.meewan.zrtc.module.pgpmodule;

import java.util.LinkedList;
import java.util.Queue;

import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import fr.meewan.zrtc.network.Proxy;

public class PGPProxy extends Thread
{
	private ZContext ctx;
	private boolean ready = false;
	private String frontBind;
	private String[] backBinds;
	Queue<String> workerQueue = new LinkedList<String>();
	private boolean stop = false;
	private Socket front;
	private Socket back;
	
	public PGPProxy(ZContext ctx, String frontBind, String[] backBinds)
	{
		this.ctx = ctx;
		this.frontBind = frontBind;
		this.backBinds = backBinds;
	}
	
	public void run()
	{
		front = this.ctx.createSocket(ZMQ.ROUTER);
		front.bind(this.frontBind);
		back = this.ctx.createSocket(ZMQ.DEALER);
		for(String bind : this.backBinds)
		{
			back.bind(bind);
		}
		
		//System.out.println("Starting proxy");
		this.ready = true;
		while(!this.stop)
		{
			ZMQ.Poller items = new ZMQ.Poller (2);

			items.register(back, Poller.POLLIN);
			if(workerQueue.size() > 0)
			{
				items.register(front, ZMQ.Poller.POLLIN);
			}
			items.poll();

			if(items.pollin(0))
			{
				ZMsg msg = ZMsg.recvMsg(back);
				workerQueue.add(msg.popString());
			}
			if(items.pollin(1))
			{
				ZMsg msg = ZMsg.recvMsg(front);
				msg.addFirst(new ZFrame(workerQueue.poll()));
				msg.send(back);
			}
		}
	}
	
	public boolean isReady()
	{
		return this.ready;
	}
	
	public void setStop(boolean stop)
	{
		this.stop = stop;
	}
    
    public PGPProxy restart(String frontBind, String[] backBinds)
    {
    	ctx.destroySocket(front);
    	ctx.destroySocket(back);
    	return new PGPProxy(ctx, frontBind, backBinds);
    }
}
