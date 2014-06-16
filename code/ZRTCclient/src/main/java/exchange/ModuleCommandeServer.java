/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package exchange;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;
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
    
    public void sendMessage(String message, User user, String cible){
        message = buildMessage(message,cible);
        String messageFinal=outils.encode(user.getNick())+message+outils.encode("SIGNATURE");
        commande=messageFinal;
        System.out.println("envoi au server :"+messageFinal);
        requester.send(messageFinal.getBytes(),0);
        
        byte[] reply = requester.recv(0);
        retour = new String(reply);
        System.out.println("Received " + retour);
    }
    
    public String buildMessage(String message, String cible){
        String messageRetour="";
        //on verifie si c'est une commande
        System.out.println("le message a envoyer est : "+message);
        if(message.charAt(0)=='/'){//on verifie si on commance par un '/'
            message=message.substring(1);//on enlève le /
            String arglist[]=message.split(" ");//création d'un tableau d'argument
            
            commande=arglist[0].toUpperCase();//on récupère le premier argument en majuscule
            System.out.println("la commande est : "+commande);
            commande=user.getNick()+msgDelimiter;
                for (String argument : arglist){
                    messageRetour+=DatatypeConverter.printBase64Binary((argument.toUpperCase()).getBytes())+msgDelimiter;
                    commande+=argument.toUpperCase()+msgDelimiter;
                }
                
            }
            
        //si ce n'est pas une commande c'est un say
        else{
            System.out.println("pas de commande");
            commande=user.getNick()+msgDelimiter+"SAY"+msgDelimiter+cible+msgDelimiter+message;
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
