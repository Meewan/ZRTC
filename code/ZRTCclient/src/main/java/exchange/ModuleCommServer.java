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
public class ModuleCommServer extends Thread{
    
    //private String message;
    private final String msgDelimiter = "#";
    private final ZMQ.Socket requester;
    private final ZMQ.Context context;
    private ZMQ.Socket echange;
    private final String adresse;
    private String cleUID;
    private User user;
    
    private String retour;
    
    
    public ModuleCommServer(String adresse, ZMQ.Context context, User user){
        this.user=user;
        this.context=context;
        this.adresse=adresse;
        System.out.println("connexion au server a l'adresse :"+adresse );
        requester = context.socket(ZMQ.REQ);
        requester.connect(adresse);
        
        //ZMQ.Socket test = context.socket(ZMQ.PULL);
        //test.setIdentity("id".getBytes());
        
    }
    
    public void sendMessage(String message, User user){
        message = buildMessage(message);
        String messageFinal=encode(user.getNick())+message+"SIGNATURE";
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
    
    //création du message de commande a envoyer
    public String buildMessage(String message){
        String messageRetour="";
        String listCommande[] = {"CONNECT","REGISTER","IDENTIFY","NICK","QUIT","MODE","JOIN","PART","MESSAGE"};//liste des commandes possible
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
                    messageRetour+=DatatypeConverter.printBase64Binary((argument.toUpperCase()).getBytes())+msgDelimiter;
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
    
    
    public Map<String,String> parseMessage(String rawMessage){
        Map<String,String> message = new HashMap<>();
        //prasing du message
        String[] tmp = rawMessage.split(this.msgDelimiter);
        message.put("user", new String(DatatypeConverter.parseBase64Binary(tmp[0])));
        message.put("command", new String(DatatypeConverter.parseBase64Binary(tmp[1])));
        //liste des arguments
        int i;
        for(i = 2; i < (tmp.length)-1; i++)
        {
            message.put("arg" + (i - 2), new String(DatatypeConverter.parseBase64Binary(tmp[i])));
        }
        message.put("argc", ((Integer)(i -1)).toString());
        //on initialise un certain nombre de variables
        message.put("authorized", "false");
        message.put("correctsignature", "false");
        message.put("fromnetwork" , "false");
        //signature du clilent
        message.put("signature", tmp[tmp.length - 1]);
        
        return message;
    }
    
    
    //fonction de connexion au server
    public boolean connectServer(User user){
        echange = context.socket(ZMQ.REQ);
        echange.connect(adresse);
        
        //on envoi la demande de connexion
        String message = encode(user.getNick())+encode("CONNECT")+encode(user.getPgpKey())+encode(user.getSignature());
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
        
        if(tmp[0].equals("CONNECT")){
            cleUID=tmp[1];
            System.out.println("Connexion au server OK, cleUID = " +cleUID);
            return true;
        }
        else{
            System.out.println("Connexion au server echoué");
            return false;
        }
    }
    
    public void close(){
        echange.close();
        requester.close();
    }
    
    
    
    @Override
    public void run(){
        
        while (!Thread.currentThread().isInterrupted()) {
            
            String message =encode(user.getNick())+encode("PING")+user.getSignature();
            
            echange.send(message.getBytes(),0);
        
            byte[] reply = echange.recv(0);
            String infoServer = new String(reply);
            System.out.println("Received " + infoServer);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ModuleCommServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
        }
        
        
    }
    
}
