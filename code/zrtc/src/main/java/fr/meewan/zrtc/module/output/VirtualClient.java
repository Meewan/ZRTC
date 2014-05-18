package fr.meewan.zrtc.module.output;

import org.zeromq.*;
import org.zeromq.ZMQ.*;

public class VirtualClient extends Thread
{
	private final ZContext coreContext;
	private final ZContext edgeContext;
	private final String identity;
	private Socket subscriber;
	private final String firstChan;
	private boolean stop = false;
	
	public VirtualClient(ZContext coreContext, ZContext edgeContext, String identity, String firstChan)
	{
		this.coreContext = coreContext;
		this.edgeContext = edgeContext;
		this.identity = identity;
		this.firstChan = firstChan;
		
		this.start();
	}
	
	public void run()
	{
		Socket pusher = edgeContext.createSocket(ZMQ.PUSH);
		pusher.setIdentity(identity.getBytes());
		pusher.connect("inproc://outputProxy");
		//pusher.connect("tcp://127.0.0.1:5559");
		
		subscriber = coreContext.createSocket(ZMQ.SUB);
		subscriber.connect("inproc://outputPublisher");
		//subscriber.connect("tcp://127.0.0.1:5558");
		subscriber.subscribe(firstChan.getBytes());
		
		while(!stop)
		{
			ZMsg msg = ZMsg.recvMsg(subscriber);
			msg.wrap(new ZFrame(identity));
			msg.send(pusher);
		}
	}
	
	public void subscribe(String topic)
	{
		subscriber.subscribe(topic.getBytes());
	}
	
	public void unsubscribe(String topic)
	{
		subscriber.unsubscribe(topic.getBytes());
	}

    public boolean isStop() {
        return stop;
    }
	
	public void setStop(boolean stop)
	{
		this.stop = stop;
	}
}
