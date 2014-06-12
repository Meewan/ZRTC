

package fr.meewan.zrtc.module.command;

import java.io.File;
import java.io.IOException;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;

/**
 *
 * @author Meewan
 */
public class CommandConfiguration 
{
    private final int maxWorkers;
    private final String coreAddress;
    private final int corePort;
    private final String configAddress;
    private final int configPort;
    private final int listeningPort;
    private final int publicListeningPort;
    
    public CommandConfiguration(String path) throws IOException
    {
        Ini config = getIni(path);
        Profile.Section mainSection = config.get("main");
        maxWorkers = Integer.parseInt(mainSection.get("workers"));
        listeningPort = Integer.parseInt(mainSection.get("listeningPort"));
        coreAddress = mainSection.get("coreAddress");
        corePort = Integer.parseInt(mainSection.get("corePort"));
        configAddress = mainSection.get("configAddress");
        configPort = Integer.parseInt(mainSection.get("configPort")); 
        publicListeningPort = Integer.parseInt(mainSection.get("publiclisteningport"));
    }
    
    private Ini getIni(String path) throws IOException
    {
        if(File.separator.equals("\\"))
        {
            return (Ini) new Wini(new File(path));
        }
        else
        {
            return new Ini(new File(path));
        }
    }

    public int getMaxWorkers() {
        return maxWorkers;
    }

    public String getCoreAddress() {
        return coreAddress;
    }

    public int getCorePort() {
        return corePort;
    }

    public String getConfigAddress() {
        return configAddress;
    }

    public int getConfigPort() {
        return configPort;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public int getPublicListeningPort() {
        return publicListeningPort;
    }
            
}
