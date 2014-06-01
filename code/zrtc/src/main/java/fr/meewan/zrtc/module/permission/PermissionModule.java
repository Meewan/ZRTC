/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission;

import fr.meewan.zrtc.module.Module;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meewan
 */
public class PermissionModule implements Module {
    
    private final String CONFPATH = "config" + File.separator + "permission.ini";
    PermissionServer permissionServer;

    public PermissionModule() {
        permissionServer = new PermissionServer();
        
    }
    
    

    @Override
    public void startModule() {
        reload();
        permissionServer.start();
    }

    @Override
    public void restartModule() {
        
    }

    @Override
    public void reload() {
        try {
            permissionServer.setConfiguration(new PermissionConfiguration(CONFPATH));
        } catch (IOException ex) {
            Logger.getLogger(PermissionModule.class.getName()).log(Level.SEVERE, "Impossible de recharger la configuration", ex);
        }
    }
    
    
}
