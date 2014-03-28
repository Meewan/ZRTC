/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

/**
 * Classe gérant le parsing et l'exposition de la configuration du core
 * @author rpaoloni
 */
public class CoreConfiguration extends Configuration
{
    protected String privateListeningPort;
    protected Map<String,ModuleCoreConfiguration> moduleList;
    protected Map<String, List<String>> commands;
    protected String privateKey;
    
    public CoreConfiguration(String configFile) throws IOException
    {
        reloadCoreConfiguration(configFile);
    }
    
    public void reloadCoreConfiguration(String configFile) throws IOException
    {
        Ini config = getIni(configFile);
        Section mainSection = config.get("main");
        this.privateListeningPort = mainSection.get("privateListeningPort");
        this.publicListeningPort  = Integer.parseInt(mainSection.get("publicListeningPort"));
        
        Section cryptoSection = config.get("cryptography");
        this.publicKey  = this.parseKey(cryptoSection.get("publicKey"));
        this.privateKey = this.parseKey(cryptoSection.get("privateKey"));
        
        Section modules = config.get("modules");
        this.moduleList = new HashMap<>();
        for(String module : modules.keySet())
        {
            moduleList.put(module, new ModuleCoreConfiguration(config.get(module)));
        }
        
        Section commandConfiguration = config.get("commands");
        this.commands = new HashMap<>();
        for(String command : commandConfiguration.keySet())
        {
            this.commands.put(command, lifeCycleParser(commandConfiguration.get(command)));
        }
    }
    
    protected List<String> lifeCycleParser(String lifeCycleString)
    {
        List<String> lifeCycle = new ArrayList<>();
        String tmp[] = lifeCycleString.split("-");
        for(String checkpoint : tmp)
        {
            lifeCycle.add(checkpoint.replaceAll("\\s+",""));
        }
        return lifeCycle;
    }

    public String getPrivateListeningPort() {
        return privateListeningPort;
    }

    public Map<String, ModuleCoreConfiguration> getModuleList() {
        return moduleList;
    }

    public Map<String, List<String>> getCommands() {
        return commands;
    }

    public String getPrivateKey() {
        return privateKey;
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
