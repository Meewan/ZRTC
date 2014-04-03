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
 */
public class ModuleCoreConfiguration extends Configuration
{

    protected String adress;
    protected boolean internal;
    
    public ModuleCoreConfiguration(Section module) 
    {   
        this.internal = Boolean.getBoolean(module.get("internal"));
        this.adress = module.get("adress");
        this.publicListeningPort = Integer.parseInt(module.get("listeningPort"));
        this.publicKey = parseKey(module.get("publicKey"));
        System.out.println(this.toString());
    }
    
    @Override
    public String toString()
    {
        String output = "adress : " + this.adress + ":" + this.publicListeningPort + "\n";
        output += "active : " + this.internal + "\n";
        output += "key : \n" + this.publicKey;
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

    public String getPublicKey() {
        return publicKey;
    }

    public int getPublicListeningPort() {
        return publicListeningPort;
    }
    
}
