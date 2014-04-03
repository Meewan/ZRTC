package fr.meewan.zrtc;
import fr.meewan.zrtc.module.Module;
import fr.meewan.zrtc.module.ModuleFactory;
import java.io.IOException;
import fr.meewan.zrtc.configuration.CoreConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author rpaoloni
 */
public class Zrtc {


    final public static String CONFIGURATION_FILE = "config.ini";
    public static void main(String[] args) throws Exception
    {
        CoreConfiguration configuration = null;
        try 
        {
            configuration = new CoreConfiguration(CONFIGURATION_FILE);
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(Zrtc.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        //génération de la liste des modules 
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
