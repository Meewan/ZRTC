package exchange;

import java.io.IOException;
import java.security.SignatureException;
import org.bouncycastle.openpgp.PGPException;
import fenetre.Window;
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
    
    private final String msgDelimiter = "#";
    private final ZMQ.Context context;
    private ZMQ.Socket echange;
    private final String adresse;
    private String uidKey;
    private User user;
    private boolean stop=false;
    private final Outils outils=new Outils();
    private Window fenetre;
    
    
    public ModuleConnexionServer(String adresse, ZMQ.Context context, User user, Window fenetre){
        this.user=user;
        this.context=context;
        this.adresse=adresse;
        this.fenetre=fenetre;
        System.out.println("connexion au server a l'adresse :"+adresse );
    }
    
    public String getUidKey(){
        return uidKey;
    }
    
    public String getAdresse(){
        return adresse;
    }
    
    
    /*fonction de connexion au server
    envoi CONNECT au server
    attent en retour la cleePGP
    connection ok ->return true
    no connexion -> return false
    */
    public boolean connectServer(User user) throws SignatureException, PGPException, IOException{
        echange = context.socket(ZMQ.REQ);
        echange.connect(adresse);
        
        //on envoi la demande de connexion
        System.out.println("Demande de connexion au server");
        String message = user.addSignature(outils.encode(user.getNick())+outils.encode("CONNECT")+outils.encode(user.getPublicKey()));
        System.out.println("Envoi au server :"+message);
        echange.send(message.getBytes(),0);
        
        //on attend la réponse du server
        System.out.println("En attente de retour server...");
        
        byte[] reply = echange.recv(0);
        String repServer = new String(reply);
        String[] tmp = repServer.split(this.msgDelimiter);
        System.out.print("Received : ");
        //affichage de la reception pour control
        for(int i=0;i<tmp.length;i++){
            tmp[i]=new String (DatatypeConverter.parseBase64Binary(tmp[i]));
            System.out.print(tmp[i]+" ");
        }
        System.out.println("");
        
        //recupération de la clee
        if(tmp[0].equals("CONNECT")){
            uidKey=tmp[1];
            System.out.println("Connexion au server OK, cleUID = " +uidKey);
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
    
    /*
    thrade de connexion au server envoi PING a chaque réponse server
    */
    @Override
    public void run(){
        System.out.println("Lancement du module de connexion");
        
        while (!stop) {
            
            //String message =outils.encode(user.getNick())+outils.encode("PING")+outils.encode(user.getSignature());
            String message = "";
				try {
					message = user.addSignature(outils.encode(user.getNick())+outils.encode("PING"));
				} catch (SignatureException | PGPException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            echange.send(message.getBytes(),0);
        
            
            byte[] reply = echange.recv(0);
            String infoServer = new String(reply);
            System.out.println("Received in return loop " + infoServer);
            
            if (!infoServer.equals("PING")){
                Map<String,String> tmp = outils.parsMessageRetour(infoServer);
                fenetre.traitementRetourBoucle(outils.parsMessageRetour(infoServer));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ModuleConnexionServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
    }
    
}
