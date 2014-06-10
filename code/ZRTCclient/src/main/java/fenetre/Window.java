package fenetre;


import exchange.CommServer;
import exchange.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
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
public class Window extends JFrame implements ActionListener {
    
    private JPanel container = new JPanel();
    private JSplitPane panelTop = new JSplitPane();
    private JPanel panelBottom = new JPanel();
    private JPanel panelRight = new JPanel();
    private JPanel panelLeft = new JPanel();
    private JTabbedPaneWithCloseIcons onglet = new JTabbedPaneWithCloseIcons();
    //private JTabbedPane onglet = new JTabbedPane(JTabbedPane.BOTTOM,JTabbedPane.SCROLL_TAB_LAYOUT);
    private JMenuBar menuBar = new JMenuBar();
    private JMenu menuItem1 = new JMenu("Fichier");
    private JMenu menuItem2 = new JMenu("Edition");
    private JMenuItem itemNewOnglet = new JMenuItem("Nouvel onglet");
    private JTextField textUser = new JTextField();
    private JLabel nickname = new JLabel("...");
    private int COUNT=0;
    
    private User user = new User("pseudo","mdp","admin");
    private ZMQ.Context context = ZMQ.context(1);
    
    public Window(){
        //Création de la fenètre
        this.setTitle("ZRTC - Le chat IRC en Java");
        this.setSize(800,700);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //parametrage du menu
        itemNewOnglet.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event){
                newTab("Onglet 4",Color.MAGENTA);
                System.out.println("ajout d'un onglet");
            }
        });
        menuItem2.add(itemNewOnglet);
        this.menuBar.add(menuItem1);
        this.menuBar.add(menuItem2);
        this.setJMenuBar(menuBar);
        onglet.setTabPlacement(JTabbedPane.BOTTOM);
        onglet.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        initialisation();
        
        //affichage du container et de la fenetre
        this.setContentPane(container);
        this.setVisible(true);
    }
    
    public void initialisation(){
        //Parametrage du Haut
        panelLeft.setBackground(Color.LIGHT_GRAY);
        newTab("Onglet de test",Color.GREEN);
        //newTab("Onglet 2",Color.BLUE);
        //newTab("Onglet 3",Color.ORANGE);
        panelTop = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft,onglet);
        panelTop.setOneTouchExpandable(true);
        panelTop.setDividerSize(10);
        panelTop.setDividerLocation(200);

        
        
        //Parametrage du Bas
        Font policeTxt = new Font("Arial",Font.LAYOUT_LEFT_TO_RIGHT,16);
        Font policeNick = new Font("Arial",Font.BOLD,16);
        textUser.setPreferredSize(new Dimension(500,30));
        textUser.setFont(policeTxt);
        textUser.addActionListener(this);
        nickname.setForeground(Color.RED);
        nickname.setFont(policeNick);
        nickname.setText(user.getNick());
        panelBottom.add(nickname);
        panelBottom.add(textUser);
        panelBottom.setBackground(Color.DARK_GRAY);
        
        //ajout du haut et du bas a la fenêtre
        container.setLayout(new BorderLayout());
        container.add(panelTop, BorderLayout.CENTER);
        container.add(panelBottom, BorderLayout.SOUTH);
        
    }
    
    //fonction ajout d'un onglet
    public void newTab(String title, Color color){
        Panneau newOnglet = new Panneau(COUNT,title,color,context);
        onglet.addTab(newOnglet, newOnglet);
        COUNT++;
    }
    

    
    @Override
    public void actionPerformed(ActionEvent e) {

        Panneau selectPanneau = (Panneau)onglet.getSelectedComponent();
        selectPanneau.displayTextMessage(textUser.getText(),user);//on affiche le text entré dans la fenetre
        selectPanneau.comm.send(textUser.getText());//on envoi le message
        textUser.setText(null);//on vide le champ de texte
        selectPanneau.displayTextInfo(selectPanneau.comm.getRetour());
        
    }
}
