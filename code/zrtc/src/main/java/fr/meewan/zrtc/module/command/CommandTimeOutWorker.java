/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import fr.meewan.zrtc.utils.Misc;
import fr.meewan.zrtc.utils.NetworkMessage;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meewan
 */
public class CommandTimeOutWorker extends Thread
{
    final private CommandServer commandServer;
    private Set<String> oldWaitingCommandList;

    public CommandTimeOutWorker(CommandServer commandServer) 
    {
        this.commandServer = commandServer;
    }
    
    
    @Override
    public void run()
    {
        while(true)
        {
            Set<String> newUserList = commandServer.getActiveExternalConnexions().keySet();
            pingAll(newUserList);
            Set<String> waytingCommandList = commandServer.getWaitingCommands().keySet();
            List<String> zombies = (List<String>) Misc.compareSet(oldWaitingCommandList, waytingCommandList);
            oldWaitingCommandList = waytingCommandList;
            try {
                this.wait(30000);
            } catch (InterruptedException ex) {
                Logger.getLogger(CommandTimeOutWorker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
    
    /**
    * envois un message ping a tout les utilisateurs transmis en argument
    */
    private void pingAll(Set<String> usersToPing)
    {
        for(String user : usersToPing)
        {
            CommandExternalWorker userWorker = commandServer.getActiveExternalConnexions(user);
            if(userWorker != null)
            {
                userWorker.setMessage(NetworkMessage.generatePingMessage(""));
                userWorker.wakeUp();
            }
            commandServer.removeFromActiveExternalConnexions(user);
        }
    }
    
    /**
     * méthode terminant les commandes en attente passé en argument a l'aide d'un mesage timeout
     * @param targets liste des commandes a terminer
     */
    private void sendTimeout(List<String> targets)
    {
        
        for(String target : targets)
        {
            CommandExternalWorker external = commandServer.getWaitingCommand(target);
            external.setMessage(NetworkMessage.generateErrorMessage(7, target));
            external.wakeUp();
            commandServer.removeFromWaitingCommands(target);
        }
    }
    
}
