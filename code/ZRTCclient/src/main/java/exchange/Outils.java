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
public class Outils {
    private final String msgDelimiter="#";
    
    
        public String encode(String message){
        String finalMessage="";
        finalMessage += DatatypeConverter.printBase64Binary(message.getBytes());
        finalMessage += msgDelimiter;
       
        return finalMessage;
    }
    
    public String decode(String message){
        String finalMessage;
        finalMessage = new String(DatatypeConverter.parseBase64Binary(message));
        return finalMessage;
    }
    
        public Map<String,String> parseMessage(String rawMessage){
        Map<String,String> message = new HashMap<>();
        //parsing du message
        String[] tmp = rawMessage.split(this.msgDelimiter);
        System.out.print("recu du zmsg : ");
        for(String element : tmp){
            System.out.print(new String(DatatypeConverter.parseBase64Binary(element))+"#");
        }
        System.out.println("");
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
}