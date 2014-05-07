/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe implémentant des méthodes statiques capable de générer certain messages réseau
 * des méthodes statiques pour contourner la limitation de l'héritage multiple
 * @author Meewan
 */
public class NetworkMessage {
    
    /*
    *Méthode générant un message d'erreur a renvoyer a l'utilisateur si le message qu'il a envoyé n'est aps correcte
    reason: 
    1: le message soumis ne peut être parsé
    2: forbidden
    3: signature pgp invalide
    4: missing arguments
    5: commande inconue
    6: cible non trouvé
    */
    public static Map<String,String> generateErrorMessage(int reason, String commandId)
    {
        Map <String, String > message = new HashMap<String,String>();
        message.put("commandid", commandId);
        message.put("command", "default");
        message.put("arg0", ((Integer) (400 + reason)).toString());
        message.put("argc", (Integer.toString(1)));
        message.put("authorized", "true");
        message.put("correctsignature", "true");
        message.put("fromnetwork" , "true");
        return message;
    }
    
    /**
     * message a envoyer si tout va bien
     * @return 
     */
    public static Map<String,String> generateSuccessMessage(String commandId)
    {
        Map <String, String > message = new HashMap<String,String>();
        message.put("command", "default");
        message.put("commandid", commandId);
        message.put("arg0", Integer.toString(200));
        message.put("argc", (Integer.toString(1)));
        message.put("authorized", "true");
        message.put("correctsignature", "true");
        message.put("fromnetwork" , "true");
        return message;
    }
   
    /**
     * genere un message de ping
     * @param commandId
     * @return 
     */
    public static Map<String, String> generatePingMessage(String commandId)
    {
        Map <String, String > message = new HashMap<String,String>();
        message.put("command", "ping");
        message.put("commandid", commandId);
        message.put("argc", (Integer.toString(0)));
        message.put("authorized", "true");
        message.put("correctsignature", "true");
        message.put("fromnetwork" , "true");
        return message;
    }
    
    
}
