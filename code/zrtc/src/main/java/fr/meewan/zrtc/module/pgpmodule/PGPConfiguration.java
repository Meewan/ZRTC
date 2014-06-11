

package fr.meewan.zrtc.module.pgpmodule;

import java.io.File;
import java.io.IOException;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;

/**
 *
 * @author Meewan
 */
public class PGPConfiguration 
{
    private final String coreAddress;
    private final int corePort;
    private final int subPort;
    private final String subAddress;
    private final String configAddress;
    private final int configPort;
    private final int nbWorkers;
    
    public PGPConfiguration(String path) throws IOException
    {
        Ini config = getIni(path);
        Profile.Section mainSection = config.get("main");
        subPort = Integer.parseInt(mainSection.get("subPort"));
        if(mainSection.containsKey("subAddress"))
        	subAddress = mainSection.get("subAddress");
        else
        	subAddress = "*";
        coreAddress = mainSection.get("coreAddress");
        corePort = Integer.parseInt(mainSection.get("corePort"));
        configAddress = mainSection.get("configAddress");
        configPort = Integer.parseInt(mainSection.get("configPort"));
        nbWorkers = Integer.parseInt(mainSection.get("nbWorkers"));
    }

	public int getNbWorkers() {
		return nbWorkers;
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
    
    public int getSubPort() {
		return subPort;
	}

	public String getSubAddress() {
		return subAddress;
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

    
}
