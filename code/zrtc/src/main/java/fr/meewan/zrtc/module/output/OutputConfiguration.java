

package fr.meewan.zrtc.module.output;

import java.io.File;
import java.io.IOException;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;

/**
 *
 * @author Meewan
 */
public class OutputConfiguration 
{
    private final String coreAddress;
    private final int corePort;
    private final int externalPort;
    private final String configAddress;
    private final int configPort;
    
    public OutputConfiguration(String path) throws IOException
    {
        Ini config = getIni(path);
        Profile.Section mainSection = config.get("main");
        externalPort = Integer.parseInt(mainSection.get("externalPort"));
        coreAddress = mainSection.get("coreAddress");
        corePort = Integer.parseInt(mainSection.get("corePort"));
        configAddress = mainSection.get("configAddress");
        configPort = Integer.parseInt(mainSection.get("configPort"));
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

    public String getCoreAddress() {
        return coreAddress;
    }

    public int getCorePort() {
        return corePort;
    }

    public int getExternalPort() {
        return externalPort;
    }

	public String getConfigAddress() {
		return configAddress;
	}

	public int getConfigPort() {
		return configPort;
	}

    
}
