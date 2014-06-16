package fenetre;



import exchange.ModuleCommandeServer;
import exchange.ModuleConnexionServer;
import exchange.Outils;
import exchange.User;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import org.zeromq.ZMsg;

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
    private JMenuBar menuBar = new JMenuBar();
    private JMenu menuItem1 = new JMenu("Fichier");
    private JMenu menuItem2 = new JMenu("Edition");
    private JMenuItem itemNewOnglet = new JMenuItem("Nouvel onglet");
    private JTextField textUser = new JTextField();
    private JLabel nickname = new JLabel("...");
    
    private User user = new User("pseud","mdp","admin");
    private ZMQ.Context context = ZMQ.context(1);
    private ModuleConnexionServer connexionServer;
    private ModuleCommandeServer commandeServer;
    private Map<String , Panneau> listeOnglet = new HashMap<>();
    private String ongletCurrent;
    public CardLayout card = new CardLayout(0,0);
    private boolean stop=false;
    
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
        connexionServer = new ModuleConnexionServer("tcp://localhost:22333",context,user);
        if(connexionServer.connectServer(user)){
            (listeOnglet.get(ongletCurrent)).displayTextInfo("Connexion au server etablie");
            connexionServer.start();
            commandeServer=new ModuleCommandeServer("tcp://localhost:22333",context,user);
            new ModuleReceptionServer().start();
        }
        else {
            (listeOnglet.get(ongletCurrent)).displayTextInfo("Connexion au server refusé");
            stop=true;
        }
        
    }
    
    public void initialisation(){
        
        
        //Parametrage du Haut
        panelLeft.setBackground(Color.LIGHT_GRAY);
        panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
        panelRight.setBackground(Color.red);
        setContentPane(panelRight);
        panelRight.setLayout(card);
        
        
        newTab("Demarrage");
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
        Panneau onglet = new Panneau(title, context,user);
        JButton bouton = new JButton(title);
        bouton.addActionListener(new OngletListener());
        panelLeft.add(bouton);
        listeOnglet.put(title,onglet);//ajout de l'onglet dans la map d'onglet
        ongletCurrent=title;
        
        panelRight.add(onglet ,title);
        
        card.show(panelRight, title);
    }
    
    //fonction suppression d'un onglet ******A FINIR******
    public void removeTab(String title){
        panelRight.remove(this);//enlever le bouton de la liste des boutons
        listeOnglet.remove(title);
        repaint();
    }
    
    
    
    //fonction chagement de pseudo
    public void changeNickname(String pseudo){
        nickname.setText(pseudo);
        user.setNick(pseudo);
    }
    
    public void traitmentEv(String message){
        (listeOnglet.get(ongletCurrent)).displayTextMessage(message, user.getNick());//affichage du text dans la fenetre
        commandeServer.sendMessage(message, user, ongletCurrent);//traitement et envoi du message au server
        this.traitementRetourServer(commandeServer.parseCommandeServer(commandeServer.getRetour()));//affichage de la réponse du server
    }
    
    
    public void traitementRetourServer(Map<String,String> message){
        String retour=message.get("retour").toLowerCase();
        String commande=message.get("command");
        String codeErreur=message.get("codeErr");
        
        int argc = Integer.parseInt(message.get("argc"));
        List<String> args = new ArrayList<>();
        for(int i = 0; i < argc; i++)
        {
            args.add(i, message.get("arg" + i));
        }
        
        if(!retour.equals("default")) System.out.println("la réponse n'est pas un default !!!");
        switch (commande.toLowerCase())
        {
            case "say":
            {
                switch (codeErreur)
                {
                case "200":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Message bien envoyé");
                    break;
                case "402":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur "+codeErreur);
                    break;
                default:
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+codeErreur);
                    break;
                }
            }
            break;
                
            case "join":
            {
            switch (codeErreur)
            {
                case "200":
                    newTab(args.get(0));
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Commande OK, vous avez rejoin le chan "+args.get(0));
                    break;
                case "402":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo(codeErreur);
                    break;
                default:
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+codeErreur);
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
                switch (codeErreur)
                {
                    case "200":
                        if(listeOnglet.containsKey(args.get(0)))
                        {
                            (listeOnglet.get(args.get(0))).displayTextMessage(args.get(1), args.get(0));
                        }
                        else
                        {
                            newTab(args.get(0));
                            (listeOnglet.get(args.get(0))).displayTextInfo("Conversation privé avec "+args.get(0));
                            (listeOnglet.get(args.get(0))).displayTextMessage(args.get(1), args.get(0));
                        }
                    break;
                    case "402":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur "+codeErreur);
                    break;
                    case "406":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur "+codeErreur);
                    break;
                }
            }
            break;
                
            case "mode":
            {
                
            }
            break;
        }
        
    }
    public void traitmentRv(Map<String,String> message){
        String commande=message.get("command");
        int argc = Integer.parseInt(message.get("argc"));
        List<String> args = new ArrayList<>();
        for(int i = 0; i < argc; i++)
        {
            args.add(i, message.get("arg" + i));
        }
        
        //on verifie que le cannal demande existe bien deja
        if (!(commande.toLowerCase()).equals("message")&&!listeOnglet.containsKey(args.get(0))){
            
        }
        
        switch (commande.toLowerCase())
        {
            case "say":
            {
                (listeOnglet.get(args.get(0))).displayTextMessage(args.get(1), message.get("user"));
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
                if(listeOnglet.containsKey(args.get(0)))
                {
                    (listeOnglet.get(args.get(0))).displayTextMessage(args.get(1), args.get(0));
                }
                else
                {
                    newTab(args.get(0));
                    (listeOnglet.get(args.get(0))).displayTextInfo("Conversation privé avec "+args.get(0));
                    (listeOnglet.get(args.get(0))).displayTextMessage(args.get(1), args.get(0));
                }
            }
            break;
                
            case "mode":
            {
                
            }
            break;
                
            case "join":
            {
                newTab(args.get(0));
                (listeOnglet.get(args.get(0))).displayTextInfo(message.get("user")+" a rejoin le canal.");
            }
            break;
                
            case "part":
            {
                (listeOnglet.get(args.get(0))).displayTextInfo(message.get("user")+" a quitter le canal.");
            }
            break;
        }
                
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {

        traitmentEv(textUser.getText());
        textUser.setText(null);//on vide le champ de texte
        
    }
    
    class OngletListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent event){
            ongletCurrent=((JButton) event.getSource()).getText();
            System.out.println(ongletCurrent);
            card.show(panelRight,ongletCurrent);
        }
    }
    
    class ModuleReceptionServer extends Thread{
        private ZMQ.Socket reception;
        private Outils outils;
        @Override
        public void run(){
        reception = context.socket(ZMQ.PULL);
        reception.setIdentity(connexionServer.getPgpKey().getBytes());
        reception.connect(connexionServer.getAdresse());
        
        while(!stop){
            ZMsg msg = ZMsg.recvMsg(reception);
            Map<String,String>  msgMap;
            if(msg.size()!=2) System.out.println("ZMsg trop long ou trop court");
            System.out.println("Recu frame1: "+ new String(msg.getFirst().getData()));
            System.out.println("Recu frame2: "+ new String(msg.getLast().getData()));
            msgMap=outils.parseMessage(new String(msg.getLast().getData()));
            msgMap.put("cible", new String(msg.getFirst().getData()));
            traitmentRv(msgMap);
        }
        reception.close();
        connexionServer.close();
        
    }
    }
    
}

