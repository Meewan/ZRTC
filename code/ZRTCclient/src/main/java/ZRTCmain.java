

import fenetre.Window;
import javax.swing.UIManager;

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
    public static void main(String[] args) {
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
