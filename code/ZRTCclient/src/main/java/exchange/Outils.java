/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package exchange;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Gauthier
 */

// class pour effectuer les encodeges et decodage de message

public class Outils {
    private final String msgDelimiter="#";
    
    
    //encode le message en base64
    public String encode(String message){
        String finalMessage="";
        finalMessage += DatatypeConverter.printBase64Binary(message.getBytes());
        finalMessage += msgDelimiter;
       
        return finalMessage;
    }
    
     //decode le message de base64 vers string
    public String decode(String message){
        String finalMessage;
        finalMessage = new String(DatatypeConverter.parseBase64Binary(message));
        return finalMessage;
    }
    
    //encode le message en base64
    public String encode(byte[] message){
        String finalMessage="";
        finalMessage += DatatypeConverter.printBase64Binary(message);
        finalMessage += msgDelimiter;
       
        return finalMessage;
    }
    
    //decode le message de base64 issu d'un retour de commande en map 'user''commande''arg1''argc'..
    public Map<String,String> parseMessage(String rawMessage){
        Map<String,String> message = new HashMap<>();
        //parsing du message
        String[] tmp = rawMessage.split(this.msgDelimiter);
        System.out.print("commande envoyé ");
        message.put("user", new String(DatatypeConverter.parseBase64Binary(tmp[0])));
        message.put("command", new String(DatatypeConverter.parseBase64Binary(tmp[1])));
        //liste des arguments
        int i;
        for(i = 2; i < (tmp.length)-1; i++)
        {
            message.put("arg" + (i - 2), new String(DatatypeConverter.parseBase64Binary(tmp[i])));
        }
        message.put("argc", ((Integer)(i -1)).toString());
        return message;
    }
    
    //decode d'un message issu de l'output
    public Map<String,String> parseOutput(String rawMessage){
        Map<String,String> message = new HashMap<>();
        //parsing du message
        String[] tmp = rawMessage.split(this.msgDelimiter);
        System.out.print("recu du zmsg : ");
        for(String element : tmp){
            System.out.print(new String(DatatypeConverter.parseBase64Binary(element))+"#");
        }
        System.out.println("");
        message.put("command", new String(DatatypeConverter.parseBase64Binary(tmp[0])));
        message.put("message", new String(DatatypeConverter.parseBase64Binary(tmp[1])));
        if (tmp.length>2) message.put("user", new String(DatatypeConverter.parseBase64Binary(tmp[2])));
        return message;
    }
    
    //decode d'un message issu de la boucle de retour
    public Map<String,String> parsMessageRetour(String rawMessage){
        Map<String,String> message = new HashMap<>();
        String[] tmp = rawMessage.split(this.msgDelimiter);
        System.out.print("recu de la boucle : ");
        for(String element : tmp){
            System.out.print(new String(DatatypeConverter.parseBase64Binary(element))+"#");
        }
        System.out.println("");
        message.put("command", new String(DatatypeConverter.parseBase64Binary(tmp[0])));
        message.put("source", new String(DatatypeConverter.parseBase64Binary(tmp[1])));
        message.put("argument", new String(DatatypeConverter.parseBase64Binary(tmp[2])));
        return message;
    }
}
