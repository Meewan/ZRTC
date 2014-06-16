package exchange;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import org.zeromq.ZMQ;
        
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gauthier
 */
public class ModuleConnexionServer extends Thread{
    
    //private String message;
    private final String msgDelimiter = "#";
    private final ZMQ.Context context;
    private ZMQ.Socket echange;
    private final String adresse;
    private String pgpKey;
    private User user;
    private boolean stop=false;
    private final Outils outils=new Outils();
    
    
    public ModuleConnexionServer(String adresse, ZMQ.Context context, User user){
        this.user=user;
        this.context=context;
        this.adresse=adresse;
        System.out.println("connexion au server a l'adresse :"+adresse );

        
        
    }
    
    public String getPgpKey(){
        return pgpKey;
    }
    
    public String getAdresse(){
        return adresse;
    }
    
    
    //création du message de commande a envoyer
    
    

    
    
    //fonction de connexion au server
    public boolean connectServer(User user){
        echange = context.socket(ZMQ.REQ);
        echange.connect(adresse);
        
        //on envoi la demande de connexion
        System.out.println("Demande de connexion au server");
        System.out.println("Envoi au server :"+user.getNick()+"#CONNECT#"+user.getPgpKey()+"#"+user.getSignature());
        String message = outils.encode(user.getNick())+outils.encode("CONNECT")+outils.encode(user.getPgpKey())+outils.encode(user.getSignature());
        echange.send(message.getBytes(),0);
        
        //on attend la réponse du server
        byte[] reply = echange.recv(0);
        String repServer = new String(reply);
        String[] tmp = repServer.split(this.msgDelimiter);
        System.out.print("Received : ");
        for(int i=0;i<tmp.length;i++){
            tmp[i]=new String (DatatypeConverter.parseBase64Binary(tmp[i]));
            System.out.print(tmp[i]+" ");
        }
        System.out.println("");
        
        //recupération de la clee
        if(tmp[0].equals("CONNECT")){
            pgpKey=tmp[1];
            System.out.println("Connexion au server OK, cleUID = " +pgpKey);
            return true;
        }
        else{
            System.out.println("Connexion au server echoué");
            return false;
        }
    }
    
    public void close(){
        stop=true;
        echange.close();
    }
    
    
    @Override
    public void run(){
        
        while (!stop) {
            
            String message =outils.encode(user.getNick())+outils.encode("PING")+outils.encode(user.getSignature());
            echange.send(message.getBytes(),0);
        
            
            byte[] reply = echange.recv(0);
            String infoServer = new String(reply);
            System.out.println("Received " + infoServer);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ModuleConnexionServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
    }
    
}
