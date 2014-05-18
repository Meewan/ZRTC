package fr.meewan.zrtc.module.output;

import fr.meewan.zrtc.module.Module;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *Class faisant l'interface entre le noyeau et le module de commande
 * @author Meewan
 */
public class OutputModule implements Module
{
    private final String CONFPATH = "config" + File.separator + "output.ini";
    private OutputServer sayServer;
    public OutputModule()
    {
        sayServer = new OutputServer();
    }
    
    @Override
    public void startModule()
    {
        reloadConfig();
        sayServer.start();
    }
    
    @Override
    public void reload()
    {
        reloadConfig();
        sayServer.reload();
    }
    
    public void reloadConfig()
    {
        try {
            sayServer.setConfiguration(new OutputConfiguration(CONFPATH));
        } catch (IOException ex) {
            Logger.getLogger(OutputModule.class.getName()).log(Level.SEVERE, "Impossible de recharger la configuration", ex);
        }
    }

    @Override
    public void restartModule()
    {
        //on tue le thread 
        sayServer.setStop(true);
        try {
            sayServer.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(OutputModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        //on recrée un serveur
        sayServer = new OutputServer();
        //on le configure et on le lance
        startModule();
    }     
}

