/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fenetre;


import exchange.User;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;
import org.zeromq.ZMQ;

/**
 *
 * @author Gauthier
 */
public class JTabbedPaneWithCloseIcons extends javax.swing.JTabbedPane {
    JTabbedPaneWithCloseIcons moi;
    
    public JTabbedPaneWithCloseIcons(){
        super();
        moi =this;
    }
    
    public void addTab(int COUNT, String title, ZMQ.Context context, User user) {
        Panneau onglet = new Panneau(COUNT, title, context,user);
        super.addTab(onglet.getName(), onglet); //on ajoute une Tab à JTabbedPane
        super.setTabComponentAt(onglet.getNumber(), new CloseTabPanel(onglet.getName())); //on applique le closeTabPanel a l'element "endroit"
    }
    
    //fonction qui permet d'affiché le bouton close
    public void afficheIconAt(int endroit){
        ((CloseTabPanel)moi.getTabComponentAt(endroit)).afficheIcon(true);
    }
    
    
    class CloseTabPanel extends JPanel{
        JButton button;
        JLabel title;
 
	//constructeur sans boolean  qui de base met un bouton close
        public CloseTabPanel(String titre) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            title = new JLabel(titre);
            add(title);
            button = new TabButton();
            add(button);
            setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	}
    
        //permet d'afficher ou cacher le bouton close
        public void afficheIcon(boolean b){
            if(b){
                if(this.getComponentCount()==1)
                    this.add(button);
            }else{
                if(this.getComponentCount()>1)
                    this.remove(button);
            }
        }
        
        public void setTitle(String title){
            this.title.setText(title);
        }
}
    class TabButton extends JButton implements ActionListener {
        public TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Fermer cet onglet");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Rends le bouton transparent
            setContentAreaFilled(false);
            //pas besoin d'avoir le focus
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            addActionListener(this);            
        }
		/*
		* fonction qui ferme l'onglet du bouton close sur lequel on a cliqué
		*/
        @Override
        public void actionPerformed(ActionEvent e) {
            int X = new Double(((JButton)e.getSource()).getMousePosition().getX()).intValue();
            int Y = new Double(((JButton)e.getSource()).getMousePosition().getY()).intValue();
 
            int i = moi.getUI().tabForCoordinate((JTabbedPane)moi, X,Y);
            if (i != -1) {
                moi.remove(i);
            }
        }
 
        //we don't want to update UI for this button
        @Override
        public void updateUI() {
        }
 
        //dessine la croix dans le bouton
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            } 
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }            
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }
    
}
