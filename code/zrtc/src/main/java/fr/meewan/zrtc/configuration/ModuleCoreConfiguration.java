/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.configuration;

import org.ini4j.Profile.Section;

/**
 *Configuration des modules a destination du core.
 * ne pas confondre avec les fichiers de configuration des chaque module
 * @author rpaoloni
 * 
 * 
 * Cet ojet est immutable
 */
public class ModuleCoreConfiguration extends Configuration
{

    protected String adress;
    protected boolean internal;
    
    public ModuleCoreConfiguration(Section module) 
    {  
        this.internal = "true".equals(module.get("internal").toLowerCase());
        System.out.println("toto" + module.get("internal"));
        this.adress = module.get("adress");
        this.publicListeningPort = Integer.parseInt(module.get("listeningPort"));
    }
    
    @Override
    public String toString()
    {
        String output = "adress : " + this.adress + ":" + this.publicListeningPort + "\n";
        output += "active : " + this.internal + "\n";
        return output;
    }

    public String getAdress() {
        return adress;
    }

    public boolean isInternal() {
        return internal;
    }

    public String getConfigurationFilePath() {
        return configurationFilePath;
    }

    public int getPublicListeningPort() {
        return publicListeningPort;
    }
    
}
