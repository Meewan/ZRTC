package fenetre;


import exchange.User;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.zeromq.ZMQ;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gauthier
 */
public class Panneau extends JPanel{
    
    private JTextPane textConv = new JTextPane();
    private int position = 0; //position du curseur
    
    
    public Panneau(){
    }
    
    public Panneau(ZMQ.Context context,User user){
        JScrollPane scroll = new JScrollPane();//création du scroll
        
        scroll.getViewport().add(textConv,null);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);//scroll vertical uniquement quand nécéssaire
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textConv.setEditable(false);
        textConv.setBackground(Color.WHITE);
        
        //petit astuce pour mettre le scroll en plein écran
        this.add(scroll, null);
        this.setLayout(new GridLayout(1,1));
        this.add(scroll);
        
        
        
    }
    
    //affichage du text dans la page de dialogue sous forme de texte
    public void displayTextMessage(String text, String user){
        StyledDocument afficheZone = (StyledDocument)textConv.getDocument();
        try{
            
            java.text.SimpleDateFormat heure = new java.text.SimpleDateFormat("HH:mm:ss");
            String clock = "["+heure.format(new Date())+"]";
            String entre =text+"\n";
            String nicknam = " <"+user+"> : ";
            
            //création du style du pseudo
            MutableAttributeSet stylePseudo = new SimpleAttributeSet();
            StyleConstants.setForeground(stylePseudo, Color.red);
            StyleConstants.setBold(stylePseudo, true);
            
            //affichage du texte
            afficheZone.insertString(this.position, clock, null);
            this.position+= clock.length();
            afficheZone.insertString(this.position, nicknam, stylePseudo);
            this.position+= nicknam.length();
            afficheZone.insertString(this.position, entre, null);
            this.position+= entre.length();
        } catch (BadLocationException ev) {}
        
    }
    
    //affichage d'un message privé
    public void displayPrivatMessage(String message, String user, String cible){
        StyledDocument afficheZone = (StyledDocument)textConv.getDocument();
        try{
            
            java.text.SimpleDateFormat heure = new java.text.SimpleDateFormat("HH:mm:ss");
            String clock = "["+heure.format(new Date())+"]";
            String entre =message+"\n";
            String nicknam = " <"+user+"> to <"+cible+">: ";
            
            //création du style du pseudo
            MutableAttributeSet stylePseudo = new SimpleAttributeSet();
            StyleConstants.setForeground(stylePseudo, Color.GREEN);
            StyleConstants.setBold(stylePseudo, true);
            
            //affichage du texte
            afficheZone.insertString(this.position, clock, null);
            this.position+= clock.length();
            afficheZone.insertString(this.position, nicknam, stylePseudo);
            this.position+= nicknam.length();
            afficheZone.insertString(this.position, entre, null);
            this.position+= entre.length();
        } catch (BadLocationException ev) {}
    }
    
    //affichage de text simple dans la fenetre, message d'info
    public void displayTextInfo(String message){
        StyledDocument afficheZone = (StyledDocument)textConv.getDocument();
        //création du style
        MutableAttributeSet messageInfo = new SimpleAttributeSet();
        StyleConstants.setForeground(messageInfo, Color.BLUE);
        StyleConstants.setBold(messageInfo, true);
        try {
            //affichage du texte
            message+="\n";
            afficheZone.insertString(this.position, message, messageInfo);
        } catch (BadLocationException ex) {
            Logger.getLogger(Panneau.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.position+=message.length();
    }
    
    
    
}
