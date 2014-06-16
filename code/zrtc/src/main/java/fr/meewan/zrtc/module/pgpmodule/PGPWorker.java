package fr.meewan.zrtc.module.pgpmodule;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Socket;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class PGPWorker extends Thread
{
	private ZContext ctx;
	private Socket sck;
	private Map<String, String> comConfiguration;
	private String socketBind;
	private boolean stop = false;
	
	public PGPWorker(ZContext ctx, Map<String, String> comConfiguration, String socketBind)
	{
		this.ctx = ctx;
		this.comConfiguration = comConfiguration;
		this.socketBind = socketBind;
	}
	
	public void run()
	{
		sck = this.ctx.createSocket(ZMQ.REQ);
		
		sck.connect(this.socketBind);
		sck.send("READY");
		
		while(!stop)
		{
			ZMsg msg = ZMsg.recvMsg(sck);
			// Remove address frame
			msg.pop();
			msg.pop();//pourquoi une frame vide ?
                        
			if(msg.size() == 1)
        	{
        		Map<String,String> msgMap = new JSONDeserializer<HashMap<String,String>>().deserialize(new String(msg.getFirst().getData()));
        		
        		if(msgMap.get("authorized").equals("true") && msgMap.containsKey("signature") &&
        				msgMap.containsKey("pgpkey") && msgMap.containsKey("commandraw"))
        		{
        			//msgMap.put("correctsignature", "true");
        			// Décommenter pour utiliser le vrai execute
        			msgMap = verify(msgMap);
				}
				else
				{
				    msgMap.put("correctsignature", "false");
				}
				
				//gestion du cycle de vie
				Integer state = Integer.parseInt(msgMap.get("state"));
				state ++;
				msgMap.put("state", state.toString());
				if(state <= Integer.parseInt(msgMap.get("lifecyclestates")))
				{
				    sendInNetwork(msgMap);
				}
        		
        	}
			
			sck.send("READY");
		}
	}
	
	private Map<String,String> verify(Map<String,String> msg)
	{
		String b64Signature = msg.get("signature");
		String b64PubKey = msg.get("pgpkey");
		String cmdRaw = msg.get("commandraw");
		
		msg.put("correctsignature", "false");
		
		try
		{
			// Get public key from the b64 encoded string
			PGPPublicKeyRing ring;
			ring = new PGPPublicKeyRing(DatatypeConverter.parseBase64Binary(b64PubKey), new JcaKeyFingerprintCalculator());
			PGPPublicKey pubKey = ring.getPublicKey();
			
			// Initialize signature with b64 encoded signature
			PGPObjectFactory fact = new PGPObjectFactory(DatatypeConverter.parseBase64Binary(b64Signature));
			PGPSignatureList sigList = (PGPSignatureList)fact.nextObject();
			PGPSignature signature = sigList.get(0);
			
			// Initialize verifier with the public key
			signature.init(new BcPGPContentVerifierBuilderProvider(), pubKey);
			// Update the signature object with the data to verify against
			signature.update(cmdRaw.getBytes());
			
			msg.put("correctsignature", (signature.verify())?"true":"false");
		}
		catch(Exception ex)
		{
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
		return msg;
	}
    
    private void sendInNetwork(Map<String, String> message)
    {
        ZMQ.Socket speaker = ctx.createSocket(ZMQ.REQ);
        //on se connecte au au suivant pour qu'il complete l'objet
        speaker.connect(this.comConfiguration.get(message.get("lifecycle" + Integer.parseInt(message.get("state")))));
        //on lui passe le message
        speaker.send(new JSONSerializer().serialize(message),0);
        speaker.recv(0);
        //on ferme la connexion (on a pas besoin de sa réponse)
        speaker.close();
    }
    
    public void setStop(boolean stop)
    {
    	this.stop = stop;
    }
    
    public PGPWorker restart(String socketBind)
    {
    	ctx.destroySocket(sck);
    	return new PGPWorker(ctx, comConfiguration, socketBind);
    }
}
