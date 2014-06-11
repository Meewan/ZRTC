package fr.meewan.zrtc.module.pgpmodule;

import fr.meewan.zrtc.module.Module;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *Class faisant l'interface entre le noyeau et le module de commande
 * @author Meewan
 */
public class PGPModule implements Module
{
    private final String CONFPATH = "config" + File.separator + "pgpmodule.ini";
    private PGPServer PGPServer;
    public PGPModule()
    {
    	PGPServer = new PGPServer();
    }
    
    @Override
    public void startModule()
    {
        reloadConfig();
        PGPServer.start();
    }
    
    @Override
    public void reload()
    {
        reloadConfig();
        PGPServer.reload();
    }
    
    public void reloadConfig()
    {
        try {
        	PGPServer.setConfiguration(new PGPConfiguration(CONFPATH));
        } catch (IOException ex) {
            Logger.getLogger(PGPModule.class.getName()).log(Level.SEVERE, "Impossible de recharger la configuration", ex);
        }
    }

    @Override
    public void restartModule()
    {
        //on tue le thread 
    	PGPServer.setStop(true);
        try {
        	PGPServer.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(PGPModule.class.getName()).log(Level.SEVERE, null, ex);
        }
        //on recrée un serveur
        PGPServer = new PGPServer();
        //on le configure et on le lance
        startModule();
    }     
}

