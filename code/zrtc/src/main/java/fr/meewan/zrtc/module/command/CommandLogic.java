/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import java.util.Map;
import javax.xml.bind.DatatypeConverter;

/**
 * Classe implémentant la logique pour chaque commande
 * @author Meewan
 */
public class CommandLogic 
{
    private Map<String, String> message;

    public CommandLogic(Map<String, String> message) 
    {
        this.message = message;
    }
    
    /**
     * Méthode encodant la Map sous la forme d'une string conforme au protocol de commande.
     * @return 
     */
    public String encode()
    {
        String finalMessage = "";
        finalMessage += DatatypeConverter.printBase64Binary(message.get("command").getBytes());
        int argc = Integer.parseInt(message.get("argc"));
        for (int i = 0; i < argc; i++)
        {
            finalMessage += CommandExternalWorker.DELIMITER + DatatypeConverter.printBase64Binary(message.get("arg" + i).getBytes());
        }
        return finalMessage;
    }
    
    /**
     * Méthode vérifiant le flag disant si le module de permission a donné son 
     * accord pour effectuer la commande
     * @return 
     */
    public boolean isAuthorized()
    {
       return this.message.get("authorized").toLowerCase().equals("true");
    }
    
    /**
     * Méthode retournant le flag disant si oui ou non la signature a pu être valider
     * @return 
     */
    public boolean isCorrectSignature()
    {
        return this.message.get("correctsignature").toLowerCase().equals("true");
    }
}
