package fr.meewan.zrtc;
import fr.meewan.zrtc.module.Module;
import fr.meewan.zrtc.module.ModuleFactory;
import java.io.IOException;
import fr.meewan.zrtc.configuration.CoreConfiguration;
import fr.meewan.zrtc.network.CoreConfigurationServer;
import fr.meewan.zrtc.network.CoreWorker;
import fr.meewan.zrtc.network.Proxy;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
/**
 *
 * @author rpaoloni
 */
public class Zrtc {


    final public static String CONFIGURATION_FILE = "config" + File.separator + "config.ini";
    final public static String INTERNAL_COM_ADRESS = "inproc://core_internal_com_adress"; 
    private static List<CoreWorker> coreWorkers;
    public static HashMap<String, String> comConfiguration;
    private static Map<String, Module> internalModules;
    public static void main(String[] args) throws Exception
    {
        CoreConfiguration configuration = configure();
        
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage du module reseau du core" );
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage du module reseau du core concernant la communication des configurations du réseau");
        
        //la communication des configs entre els modules a pas besoin d'être rapide et peut donc se faire via un simple REQ-REP
        CoreConfigurationServer coreConfigurationServer = new CoreConfigurationServer(comConfiguration, configuration.getConfigListeningPort());
        coreConfigurationServer.start();
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage du module reseau du core concernant l'écoute la gestion des commandes entrantes");
        
        //on lance un proxy pour pouvoir gérer de nombreuses commandes en parralleles
        Proxy proxy = new Proxy("tcp://*:" + configuration.getPublicListeningPort() , INTERNAL_COM_ADRESS);
        //on lance les workers qui ferons le travail
        coreWorkers = new ArrayList<>();
        Context context = ZMQ.context(1);
        for(int i = 0 ; i < configuration.getWorkers() ; i++)
        {
            coreWorkers.add(new CoreWorker(INTERNAL_COM_ADRESS, configuration.getCommands(), configuration.getConfigListeningPort(), context));
            coreWorkers.get(i).start();
        }
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage des modules locaux");
        Set<String> moduleList = internalModules.keySet();
        for(String moduleName : moduleList)
        {
            Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage du module " + moduleName);
            internalModules.get(moduleName).startModule();
            Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage du module " + moduleName + " terminé");
        }
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Demarage du module reseau du core termine");
        //configuration terminé
    }
    
    public static CoreConfiguration configure()
    {
        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "debut de la configuration");
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
        internalModules = new HashMap<>();
        comConfiguration = new HashMap<>();
        for(String moduleName : moduleNamesSet)
        {
            String adress = configuration.getModuleList().get(moduleName).getAdress();
            if(configuration.getModuleList().get(moduleName).getPublicListeningPort() != 0)
            {
                adress += configuration.getModuleList().get(moduleName).getPublicListeningPort();
            }
            comConfiguration.put(moduleName, adress);
            
            if(configuration.getModuleList().get(moduleName).isInternal())
            {
                Module module = ModuleFactory.get(moduleName);
                if (module != null) 
                {
                    internalModules.put(moduleName, module);
                }
            }
        }

        Logger.getLogger(Zrtc.class.getName()).log(Level.INFO, "Configuration termine");
        return configuration;
    }
    
}
