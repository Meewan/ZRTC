package fr.meewan.zrtc;
import fr.meewan.zrtc.module.Module;
import fr.meewan.zrtc.module.ModuleFactory;
import java.io.IOException;
import fr.meewan.zrtc.utils.CoreConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
/**
 *
 * @author rpaoloni
 */
public class Zrtc {


    final public static String CONFIGURATION_FILE = "config.ini";
    public static void main(String[] args) throws IOException 
    {
        CoreConfiguration configuration = new CoreConfiguration(CONFIGURATION_FILE);
        Set<String> moduleNamesSet = configuration.getModuleList().keySet();
        Map<String, Module> modules = new HashMap<>();
        for(String moduleName : moduleNamesSet)
        {
            if(configuration.getModuleList().get(moduleName).isInternal())
            {
                Module module = ModuleFactory.get(moduleName);
                if (module != null) 
                {
                    modules.put(moduleName, module);
                }
            }
        }
    }
    
}
