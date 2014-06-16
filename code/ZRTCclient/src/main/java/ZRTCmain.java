

import fenetre.Window;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import javax.swing.UIManager;
import org.bouncycastle.openpgp.PGPException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gauthier
 */
public class ZRTCmain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SignatureException, NoSuchAlgorithmException, PGPException, IOException {
        // TODO code application logic here
        setBestLookAndFeelAvailable();
        
        Window fenetre = new Window();
        //fenetre.recip("Hello World");
    }
    
    public static void setBestLookAndFeelAvailable(){
        String system_lf = UIManager.getSystemLookAndFeelClassName().toLowerCase();
        if(system_lf.contains("metal")){
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
                }catch (Exception e) {}
            }else{
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }catch (Exception e) {}
            }
    }
    
}
