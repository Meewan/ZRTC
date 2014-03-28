/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package zrtc.utils;

import org.ini4j.Profile.Section;

/**
 *Configuration des modules a destination du core.
 * ne pas confondre avec les fichiers de configuration des chaque module
 * @author rpaoloni
 */
public class ModuleCoreConfiguration extends Configuration
{

    protected String adress;
    protected boolean active;
    
    public ModuleCoreConfiguration(Section module) 
    {   
        this.active = Boolean.getBoolean(module.get("activated"));
        this.adress = module.get("adress");
        this.publicListeningPort = Integer.parseInt(module.get("listeningPort"));
        this.publicKey = parseKey(module.get("publicKey"));
    }
    
    @Override
    public String toString()
    {
        String output = "adress : " + this.adress + ":" + this.publicListeningPort + "\n";
        output += "active : " + this.active + "\n";
        output += "key : \n" + this.publicKey;
        return output;
    }
}
