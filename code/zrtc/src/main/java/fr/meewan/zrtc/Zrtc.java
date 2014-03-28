package zrtc;
import java.io.IOException;
import zrtc.utils.CoreConfiguration;
/**
 *
 * @author rpaoloni
 */
public class Zrtc {

    /**
     * @param args the command line arguments
     */
    final public static String CONFIGURATION_FILE = "config.ini";
    public static void main(String[] args) throws IOException 
    {
        CoreConfiguration configuration = new CoreConfiguration(CONFIGURATION_FILE);
    }
    
}
