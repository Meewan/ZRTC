/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fenetre;

import exchange.ModuleCommServer;
import exchange.User;
import javax.swing.JPanel;
import org.zeromq.ZMQ;

/**
 *
 * @author Gauthier
 */
public class Onglet extends JPanel{
    
    private Panneau panneauAffiche;
    private ModuleCommServer commServer;
    private String title;
    private final int NUMBER; //numero de l'onglet
    
    public Onglet(int COUNT, String title, ZMQ.Context context){
        NUMBER=COUNT;
        this.title=title;
    }
    
    public int getNumber(){
        return NUMBER;
    }
    
    public Panneau getPanneau(){
        return panneauAffiche;
    }
    
    @Override
    public String getName(){
        return title;
    }
    
    
    public void traitment(String message, User user){
        panneauAffiche.displayTextMessage(message, user.getNick());//affichage du text dans la fenetre
        commServer.sendMessage(message, user);//traitement et envoi du message au server
        panneauAffiche.displayTextInfo(commServer.getRetour());//affichage de la réponse du server
    }
}
