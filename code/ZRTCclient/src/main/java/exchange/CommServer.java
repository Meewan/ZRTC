package exchange;


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
public class CommServer extends Thread{
    
    //private String message;
    private final String msgDelimiter = "#";
    private ZMQ.Socket requester;
    private ZMQ.Context context;
    private ZMQ.Socket echange;
    private final String adresse;
    
    private String retour;
    
    
    public CommServer(String adresse, ZMQ.Context context){
        this.context=context;
        this.adresse=adresse;
        System.out.println("connexion au server...");
        requester = context.socket(ZMQ.REQ);
        requester.connect("tcp://localhost:"+adresse);
        
    }
    
    public void send(String message){
        message = buildMessage(message);
        String messageFinal=encode("utilisateur")+message+"SIGNATURE";
        System.out.println("envoi au server :"+messageFinal);
        requester.send(messageFinal.getBytes(),0);
        
        byte[] reply = requester.recv(0);
        retour = new String(reply);
        System.out.println("Received " + retour);
    }
    
    public String getRetour(){
        return this.retour;
    }
    
    
    public String encode(String message){
        String finalMessage="";
        finalMessage += DatatypeConverter.printBase64Binary(message.getBytes());
        finalMessage += msgDelimiter;
        
        return finalMessage;
    }
    
    public String buildMessage(String message){
        String messageRetour="";
        String listCommande[] = {"CONNECT","REGISTER","IDENTIFY","NICK","QUIT","MODE","JOIN","PART"};//liste des commandes possible
        //on verifie si c'est une commande
        System.out.println(message);
        if(message.charAt(0)=='/'){//on verifie si on commance par un /
            boolean noCommande=true;
            
            message=message.substring(1);//on enlève le /
            String arglist[]=message.split(" ");//création d'un tableau d'argument
            
            String commande=arglist[0].toUpperCase();//on récupère le premier argument en majuscule
            System.out.println("la commande est : "+commande);
            for (String listCommande1 : listCommande) {
                if(commande.equals(listCommande1)) noCommande=false; //on compart avec toute les commandes possible
            }
            if(!noCommande){//si c'est une commande
                System.out.println("vraiment une commande");
                for (String argument : arglist){
                    messageRetour+=DatatypeConverter.printBase64Binary(argument.getBytes())+msgDelimiter;
                }
                
            }
            else{//si ce n'est pas une commande
                System.out.println("finalement non comande");
                messageRetour=DatatypeConverter.printBase64Binary("SAY".getBytes())+msgDelimiter;
                messageRetour+=DatatypeConverter.printBase64Binary(message.getBytes())+msgDelimiter;
            }
            
        }
        else{
            System.out.println("pas de commande");
            messageRetour=DatatypeConverter.printBase64Binary("SAY".getBytes())+msgDelimiter;
            messageRetour+=DatatypeConverter.printBase64Binary(message.getBytes())+msgDelimiter;
        }
        return messageRetour;
    }
    
    @Override
    public void run(){
        
        while (!Thread.currentThread().isInterrupted()) {
            echange = context.socket(ZMQ.REQ);
            echange.connect("tcp://localhost:"+adresse);
            echange.send("PING".getBytes(),0);
        
            byte[] reply = echange.recv(0);
            String infoServer = new String(reply);
            System.out.println("Received " + infoServer);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(CommServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        
    }
    
}
