/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package exchange;

import fenetre.Window;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.openpgp.PGPException;
import org.zeromq.ZMQ;

/**
 *
 * @author Gauthier
 */
public class ModuleCommandeServer extends Thread{
    public String commande;
    private final String msgDelimiter = "#";
    private final ZMQ.Socket requester;
    private final ZMQ.Context context;
    private final String adresse;
    private String retour;
    private User user;
    private boolean stop=false;
    private boolean requete=false;
    private final Outils outils=new Outils();
    private String messageFinal;
    private String cible;
    private Window fenetre;
    
    
    public ModuleCommandeServer(String adresse, ZMQ.Context context, User user, Window fenetre){
        this.user=user;
        this.context=context;
        this.adresse=adresse;
        this.fenetre=fenetre;
        requester = context.socket(ZMQ.REQ);
        requester.connect(adresse);
}
    
    public void close(){
        stop=true;
        requester.close();
    }
    
    public void setRequete(boolean requete){
        this.requete=requete;
        System.out.println(this.requete);
    }
    
    public String getRetour(){
        return retour;
    }
    
    /*fonction envoi un message au server 
    message : message a envoyer
    user : utilisateur
    cible: chan de depart=destination
    */
    
    /*
    public void sendMessage (String message, User usertmp, String cibletmp) throws SignatureException, PGPException, IOException{
        cible=cibletmp;
        user=usertmp;
        System.out.println("send message");
        message = buildMessage(message,cible);
        System.out.println("message: "+message);
        messageFinal= user.addSignature(outils.encode(user.getNick())+message);
        commande=messageFinal;
    }
    */
    public void sendMessage(String message, User user, String cible) throws SignatureException, PGPException, IOException{
        System.out.println("send message");
        message = buildMessage(message,cible);
        System.out.println("message: "+message);
        String messageFinal= user.addSignature(outils.encode(user.getNick())+message);
        commande=messageFinal;
        System.out.println("envoi au server :"+messageFinal);
        requester.send(messageFinal.getBytes(),0);
        System.out.println("en attente de reponse server...");
        
        byte[] reply = requester.recv(0);
        retour = new String(reply);
        System.out.println("Received " + retour);
    }
    
    /*
    fonction de création du message
    recherche si c'est une commande
    commande->creation d'argument
    no commande -> say message
    */
    public String buildMessage(String message, String cible){
        String messageRetour="";
        //on verifie si c'est une commande
        System.out.println("le message a envoyer est : "+message);
        if(message.charAt(0)=='/'){//on verifie si on commance par un '/'
            message=message.substring(1);//on enlève le /
            String arglist[]=message.split(" ");//création d'un tableau d'argument
            commande=arglist[0].toUpperCase();//on récupère le premier argument en majuscule
            System.out.println("la commande est : "+commande);
            if (commande.equals("MESSAGE")){//si la commande est un message
                messageRetour+=DatatypeConverter.printBase64Binary((commande).getBytes())+msgDelimiter;
                messageRetour+=DatatypeConverter.printBase64Binary((arglist[1]).getBytes())+msgDelimiter;
                for(int i=2;i<arglist.length;i++){
                    messageRetour+=DatatypeConverter.printBase64Binary((arglist[i]+" ").getBytes());
                }
                messageRetour+="#";
            }
            else if(commande.equals("PART")){
                messageRetour+=DatatypeConverter.printBase64Binary((commande).getBytes())+msgDelimiter;
                messageRetour+=DatatypeConverter.printBase64Binary(cible.getBytes())+msgDelimiter;
            }
            else{
                for (String argument : arglist){
                    messageRetour+=DatatypeConverter.printBase64Binary((argument).getBytes())+msgDelimiter;
                }
            }
         }
            
        //si ce n'est pas une commande c'est un say
        else{
            System.out.println("pas de commande");
            messageRetour+=DatatypeConverter.printBase64Binary("SAY".getBytes())+msgDelimiter;
            messageRetour+=DatatypeConverter.printBase64Binary(cible.getBytes())+msgDelimiter;
            messageRetour+=DatatypeConverter.printBase64Binary(message.getBytes())+msgDelimiter;
            
        }
        return messageRetour;
    }
    
    //methode pour transformer un message recu du server de commande encodé en map avec 'demande''commande''arg1''arg2'...'argc'
    public Map<String,String> parseCommandeServer(String recMessage){
        Map<String,String> message = outils.parseMessage(commande);
        //parsing du message
        String[] tmp = recMessage.split(this.msgDelimiter);
        System.out.print("Reponse du server : ");
        for(String element : tmp){
            System.out.print(new String(DatatypeConverter.parseBase64Binary(element))+"#");
        }
        System.out.println("");
        int i;
        for(i = 0; i < (tmp.length); i++)
        {
            message.put("argRetour" + (i), new String(DatatypeConverter.parseBase64Binary(tmp[i])));
        }
        message.put("argRetourc", ((Integer)(i +1)).toString());
        
        return message;
    }
    
    @Override
    public void run(){
        System.out.println("Lancement du module de commande");
        while (!stop){
            
            while(requete)
            {
                System.out.println("envoi au server :"+messageFinal);
                requester.send(messageFinal.getBytes(),0);
                System.out.println("en attente de reponse server...");
        
                byte[] reply = requester.recv(0);
                String retour = new String(reply);
                System.out.println("Received " + retour);
                fenetre.traitementRetourServer(parseCommandeServer(retour));
                requete=false;
            }
            
        }
        System.out.println("Le module de commande s'est arrete");
        
    }
    
    
}
