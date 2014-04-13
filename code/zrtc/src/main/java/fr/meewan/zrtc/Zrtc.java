package fr.meewan.zrtc;
import fr.meewan.zrtc.com.ComConfiguration;
import fr.meewan.zrtc.module.Module;
import fr.meewan.zrtc.module.ModuleFactory;
import java.io.IOException;
import fr.meewan.zrtc.configuration.CoreConfiguration;
import fr.meewan.zrtc.network.CoreConfigurationServer;
import fr.meewan.zrtc.network.CoreWorker;
import fr.meewan.zrtc.network.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    final public static String INTERNAL_COM_ADRESS = "inproc://core_internal_com_adress"; 
    private static List<CoreWorker> coreWorkers;
    public static ComConfiguration comConfiguration;
    public static void main(String[] args) throws Exception
    {
        CoreConfiguration configuration = configure();
        Set<String> moduleNamesSet = configuration.getModuleList().keySet();
        
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, null, "Demarage du module reseau du core");
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, null, "Demarage du module reseau du core concernant la communication des configurations du réseau");
        //la communication des configs entre els modules a pas besoin d'être rapide et peut donc se faire via un simple REQ-REP
        CoreConfigurationServer coreConfigurationServer = new CoreConfigurationServer(comConfiguration, configuration.getConfigListeningPort());
        coreConfigurationServer.start();
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, null, "Demarage du module reseau du core concernant l'écoute la gestion des commandes entrantes");
        //on lance un proxy pour pouvoir gérer de nombreuses commandes en parralleles
        Proxy proxy = new Proxy("tcp://*" + configuration.getPublicListeningPort() , INTERNAL_COM_ADRESS);
        proxy.start();
        //on lance les workers qui ferons le travail
        coreWorkers = new ArrayList<>();
        for(int i = 0 ; i < configuration.getWorkers() ; i++)
        {
            coreWorkers.add(new CoreWorker(INTERNAL_COM_ADRESS, configuration.getCommands(), configuration.getConfigListeningPort()));
            coreWorkers.get(i).start();
        }
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, null, "Demarage du module reseau du core termine");
        //configuration terminé
    }
    
    public static CoreConfiguration configure()
    {
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, null, "debut de la configuration");
        CoreConfiguration configuration;
        try 
        {
            configuration = new CoreConfiguration(CONFIGURATION_FILE);
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(Zrtc.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        //génération de la liste des modules 
        Set<String> moduleNamesSet = configuration.getModuleList().keySet();
        Map<String, Module> modules = new HashMap<>();
        Map<String, String> comConfigurationContent = new HashMap<>();
        for(String moduleName : moduleNamesSet)
        {
            String adress = configuration.getModuleList().get(moduleName).getAdress();
            if(configuration.getModuleList().get(moduleName).getPublicListeningPort() != 0)
            {
                adress += configuration.getModuleList().get(moduleName).getPublicListeningPort();
            }
            comConfigurationContent.put(moduleName, adress);
            
            if(configuration.getModuleList().get(moduleName).isInternal())
            {
                Module module = ModuleFactory.get(moduleName);
                if (module != null) 
                {
                    modules.put(moduleName, module);
                }
            }
        }
        comConfiguration = new ComConfiguration();
        comConfiguration.modulList = comConfigurationContent;
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, null, "Configuration termine");
        return configuration;
    }
    
}
