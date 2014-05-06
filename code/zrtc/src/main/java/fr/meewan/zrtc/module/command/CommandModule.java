/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.command;

import fr.meewan.zrtc.module.Module;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *Class faisant l'interface entre le noyeau et le module de commande
 * @author Meewan
 */
public class CommandModule implements Module
{
    private final String CONFPATH = "command.ini";
    private CommandServer commandServer;
    public CommandModule()
    {
        commandServer = new CommandServer();
    }
    
    @Override
    public void startModule()
    {
        reload();
        commandServer.start();
    }
    
    @Override
    public void reload()
    {
        try {
            commandServer.setConfiguration(new CommandConfiguration(CONFPATH));
        } catch (IOException ex) {
            Logger.getLogger(CommandModule.class.getName()).log(Level.SEVERE, "Impossible de recharger la configuration", ex);
        }
    }

    @Override
    public void restartModule()
    {
        //on tue le thread 
        commandServer.setStop(true);
        try {
            commandServer.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(CommandModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        //on recrée un serveur
        commandServer = new CommandServer();
        //on le configure et on le lance
        startModule();
    }     
}
