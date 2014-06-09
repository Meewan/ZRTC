/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.configuration;

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
    protected int privateListeningPort;
    protected int configListeningPort;
    protected int workers;
    protected Map<String,ModuleCoreConfiguration> moduleList;
    protected Map<String, List<String>> commands;
    
    public CoreConfiguration(String configFile) throws IOException
    {
        reloadCoreConfiguration(configFile);
    }
    
    public void reloadCoreConfiguration(String configFile) throws IOException
    {
        Ini config = getIni(configFile);
        Section mainSection = config.get("main");
        this.privateListeningPort  = Integer.parseInt(mainSection.get("privateListeningPort"));
        this.configListeningPort   = Integer.parseInt(mainSection.get("configListeningPort"));
        this.workers               = Integer.parseInt(mainSection.get("workers"));

        
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

    public int getConfigListeningPort() {
        return configListeningPort;
    }

    public int getPrivateLiseningPort() {
        return privateListeningPort;
    }

    public Map<String, ModuleCoreConfiguration> getModuleList() {
        return moduleList;
    }

    public Map<String, List<String>> getCommands() {
        return commands;
    }

    public String getConfigurationFilePath() {
        return configurationFilePath;
    }

    public int getWorkers() {
        return workers;
    }
    
}
