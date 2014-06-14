package fenetre;



import exchange.ModuleCommServer;
import exchange.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
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
    
    private final JPanel container = new JPanel();
    private JSplitPane panelTop = new JSplitPane();
    private final JPanel panelBottom = new JPanel();
    private JPanel panelRight = new JPanel();
    private JPanel panelLeft = new JPanel();
    //private JTabbedPaneWithCloseIcons tabOnglet = new JTabbedPaneWithCloseIcons();
    private JMenuBar menuBar = new JMenuBar();
    private JMenu menuItem1 = new JMenu("Fichier");
    private JMenu menuItem2 = new JMenu("Edition");
    private JMenuItem itemNewOnglet = new JMenuItem("Nouvel onglet");
    private JTextField textUser = new JTextField();
    private JLabel nickname = new JLabel("...");
    private int COUNT=0;
    
    private User user = new User("pseudo","mdp","admin");
    private ZMQ.Context context = ZMQ.context(1);
    private ModuleCommServer commServer;
    private Map<String , Panneau> listeOnglet = new HashMap<>();
    private String ongletCurrent;
    
    public Window(){
        //Création de la fenètre
        this.setTitle("ZRTC - Le chat en temps réel en Java");
        this.setSize(800,700);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //parametrage du menu
        itemNewOnglet.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event){
                newTab("Onglet 4");
                System.out.println("ajout d'un onglet");
            }
        });
        menuItem2.add(itemNewOnglet);
        this.menuBar.add(menuItem1);
        this.menuBar.add(menuItem2);
        this.setJMenuBar(menuBar);
        //tabOnglet.setTabPlacement(JTabbedPane.BOTTOM);
        //tabOnglet.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        initialisation();
        
        //affichage du container et de la fenetre
        this.setContentPane(container);
        this.setVisible(true);
        
        //initialisation du module de connexion
        commServer = new ModuleCommServer("tcp://localhost:22333",context,user);
        if(commServer.connectServer(user)){
            (listeOnglet.get(ongletCurrent)).displayTextInfo("Connexion au server etablie");
            commServer.start();
        }
        else {
            (listeOnglet.get(ongletCurrent)).displayTextInfo("Connexion au server refusé");
            commServer.close();
        }
        
    }
    
    public void initialisation(){
        
        
        //Parametrage du Haut
        panelLeft.setBackground(Color.LIGHT_GRAY);
        panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
        panelRight.setBackground(Color.red);
        newTab("Onglet de demarrage");
        panelTop = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft,panelRight);
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
        panelBottom.add(nickname, BorderLayout.LINE_START);
        panelBottom.add(textUser, BorderLayout.CENTER);
        panelBottom.setBackground(Color.DARK_GRAY);
        
        //ajout du haut et du bas a la fenêtre
        container.setLayout(new BorderLayout());
        container.add(panelTop, BorderLayout.CENTER);
        container.add(panelBottom, BorderLayout.SOUTH);
        
    }
    
    //fonction ajout d'un onglet
    public void newTab(String title){
        //tabOnglet.addTab(COUNT, title, context, user);
        Panneau onglet = new Panneau(COUNT, title, context,user);
        JButton bouton = new JButton(title);
        bouton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event){
                ongletCurrent=((JButton) event.getSource()).getText();
                System.out.println(ongletCurrent);
                panelRight.removeAll();
                panelRight.add(listeOnglet.get(ongletCurrent), null);
                panelRight=listeOnglet.get(ongletCurrent);
            }
        });
        panelLeft.add(bouton);
        listeOnglet.put(title,onglet);
        ongletCurrent=title;
        panelRight.removeAll();
        panelRight.add(onglet, null);
        panelRight=onglet;
        this.validate();
        COUNT++;
    }
    
    //fonction chagement de pseudo
    public void changeNickname(String pseudo){
        nickname.setText(pseudo);
        user.setNick(pseudo);
    }
    
    public void traitmentEv(String message){
        (listeOnglet.get(ongletCurrent)).displayTextMessage(message, user.getNick());//affichage du text dans la fenetre
        commServer.sendMessage(message, user);//traitement et envoi du message au server
        this.traitmentRv(commServer.parseMessage(commServer.getRetour()));//affichage de la réponse du server
    }
    
    public void traitmentRv(Map<String,String> message){
        String commande=message.get("command");
        int argc = Integer.parseInt(message.get("argc"));
        List<String> args = new ArrayList<>();
        for(int i = 0; i < argc; i++)
        {
            args.add(i, message.get("arg" + i));
        }
        
        
        switch (commande.toLowerCase())
        {
            case "say":
            {
                (listeOnglet.get(ongletCurrent)).displayTextMessage(args.get(0), message.get("user"));
            }
            break;
                
            case "default":
            {
            switch (args.get(0))
            {
                case "200":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Commande OK");
                    break;
                case "402":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo(args.get(0));
                    break;
                default:
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+args.get(0));
                    break;
            }
                
            }
            break;
                
            case "connect":
            {
                
            }
            break;
                
            case "message":
            {
                
            }
            break;
        }
                
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {

        //Panneau ongletSelect = (Panneau) tabOnglet.getSelectedComponent();
        traitmentEv(textUser.getText());
        //ongletSelect.traitmentEv(textUser.getText(),user);//envoi du texte saisi et utilisateur pour traitement
        textUser.setText(null);//on vide le champ de texte
        
    }
    
}

