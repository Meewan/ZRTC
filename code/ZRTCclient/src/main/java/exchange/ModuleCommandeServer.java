/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package exchange;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.zeromq.ZMQ;

/**
 *
 * @author Gauthier
 */
public class ModuleCommandeServer {
    public String commande;
    private final String msgDelimiter = "#";
    private final ZMQ.Socket requester;
    private final ZMQ.Context context;
    private final String adresse;
    private String retour;
    private User user;
    private boolean stop=false;
    private final Outils outils=new Outils();
    
    
    public ModuleCommandeServer(String adresse, ZMQ.Context context, User user){
        this.user=user;
        this.context=context;
        this.adresse=adresse;
        requester = context.socket(ZMQ.REQ);
        requester.connect(adresse);
}
    
    /*fonction envoi un message au server 
    message : message a envoyer
    user : utilisateur
    cible: chan de depart=destination
    */
    public void sendMessage(String message, User user, String cible) throws SignatureException, PGPException, IOException{
    	System.out.println("send message");
        message = buildMessage(message,cible);
        System.out.println("message: "+message);
        String messageFinal= user.addSignature(outils.encode(user.getNick())+message);
        System.out.println("final Message: "+messageFinal);
        commande=messageFinal;
        System.out.println("envoi au server :"+messageFinal);
        requester.send(messageFinal.getBytes(),0);
        
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
                    messageRetour+=DatatypeConverter.printBase64Binary((arglist[i]).getBytes())+msgDelimiter;
                }
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
        message.put("retour", new String(DatatypeConverter.parseBase64Binary(tmp[0])));
        message.put("codeErr", new String(DatatypeConverter.parseBase64Binary(tmp[1])));
        
        return message;
    }
    
    public String getRetour(){
        return retour;
    }
    
    
}
