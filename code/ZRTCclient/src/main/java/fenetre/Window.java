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
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import org.bouncycastle.openpgp.PGPException;
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
    private JTextField textUser = new JTextField();
    private JLabel nickname = new JLabel("...");
    
    private User user;
    private ZMQ.Context context = ZMQ.context(1);
    private ModuleConnexionServer connexionServer;
    private ModuleCommandeServer commandeServer;
    private Map<String , Panneau> listeOnglet = new HashMap<>();
    private Map<String , JButton> listeButton= new ConcurrentHashMap<>();
    private String ongletCurrent;
    public CardLayout card = new CardLayout(0,0);
    private boolean stop=false;
    
    public Window()throws SignatureException, PGPException, IOException, NoSuchAlgorithmException{
        // Obligé de mettre l'init du user ici
    	user = new User("pseudo","mdp","admin");
        
        //Création de la fenètre
        this.setTitle("ZRTC - Le chat en temps réel en Java");
        this.setSize(800,700);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //parametrage du menu
        this.menuBar.add(menuItem1);
        this.setJMenuBar(menuBar);
        initialisation();
        
        //affichage du container et de la fenetre
        this.setContentPane(container);
        this.setVisible(true);
        
        JOptionPane jop = new JOptionPane(), jop2 = new JOptionPane();
        String nom = jop.showInputDialog(null, "Veuillez entrer votre pseudo", "Entrer pseudo", JOptionPane.QUESTION_MESSAGE);
        changeNickname(nom);
        //initialisation du module de connexion
        connexionServer = new ModuleConnexionServer("tcp://localhost:22333",context,user,this);
        if(connexionServer.connectServer(user)){
            (listeOnglet.get(ongletCurrent)).displayTextInfo("Connexion au server etablie");
            connexionServer.start();
            commandeServer=new ModuleCommandeServer("tcp://localhost:22333",context,user,this);
            //commandeServer.start();
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
        Panneau onglet = new Panneau(context,user);
        JButton bouton = new JButton(title);
        bouton.addActionListener(new OngletListener());
        panelLeft.add(bouton);
        listeOnglet.put(title,onglet);//ajout de l'onglet dans la map d'onglet
        listeButton.put(title, bouton);
        ongletCurrent=title;
        
        panelRight.add(onglet ,title);
        
        card.show(panelRight, title);
    }
    
    //fonction suppression d'un onglet
    public void removeTab(String title){
        System.out.println("Remove de l'onglet "+title);
        ongletCurrent="Demarrage";
        card.show(panelRight, ongletCurrent);
        panelLeft.remove(listeButton.get(title));//enlever le bouton de la liste des boutons
        listeButton.remove(title);
        listeOnglet.remove(title);
        panelLeft.revalidate();
        panelLeft.repaint();
    }
    
    
    
    //fonction chagement de pseudo
    public void changeNickname(String pseudo){
        nickname.setText(pseudo);
        user.setNick(pseudo);
    }
    
    
    /*
    fonction de traitement d'un envoi de commande au server
    affiche, envoi et appel la fonction de traitement
    */
    public void traitmentEv(String message) throws SignatureException, PGPException, IOException{
        commandeServer.sendMessage(message, user, ongletCurrent);//traitement et envoi du message au server
        //commandeServer.setRequete(true);
        this.traitementRetourServer(commandeServer.parseCommandeServer(commandeServer.getRetour()));//affichage de la réponse du server
    }
    
    /*
    fonction de traitement
    recupère la fonction traite la demande et affiche le message
    */
    public void traitementRetourServer(Map<String,String> message){
        String commandeRetour=message.get("argRetour0");
        String commande=message.get("command");
        String codeErreur = message.get("argRetour1");
        
        switch (commande.toLowerCase())
        {
            case "say":
            {
                switch (codeErreur)
                {
                case "200":
                    System.out.println("message bien envoyé");
                    break;
                case "402":
                    (listeOnglet.get(message.get("arg0"))).displayTextInfo("Erreur "+codeErreur);
                    break;
                default:
                    (listeOnglet.get(message.get("arg0"))).displayTextInfo("ERREUR "+codeErreur);
                    break;
                }
            }
            break;
                
            case "join":
            {
                switch (codeErreur)
                {
                case "200":
                    newTab(message.get("arg0"));
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Vous avez rejoint le chan "+message.get("arg0"));
                    break;
                case "402":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+ codeErreur);
                    break;
                default:
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+ codeErreur);
                    break;
                }
                
            }
            break;
                
            case "quit":
            {
                switch (codeErreur)
                {
                case "200":
                    ongletCurrent="Demarrage";
                    card.show(panelRight, ongletCurrent);
                    //*****a ajouter la suppression de tout les onglets encours*******
                    stop=true;
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Vous avez ete déconnecté du server");
                    
                break;
                case "402":
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur "+ codeErreur);
                break;
                default:
                    (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+ codeErreur);
                break;
                }
            }
            break;
                
            case "message":
            {
                
                switch (codeErreur)
                {
                    case "200":
                        (listeOnglet.get(ongletCurrent)).displayPrivatMessage(message.get("arg1"), user.getNick(),message.get("arg0"));
                    break;
                    case "402":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur "+codeErreur);
                    break;
                    case "406":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur :"+codeErreur+". Votre destinataire n'est pas reconnu");
                    break;
                }
            }
            break;
                
            case "mode":
            {
                switch (codeErreur)
                {
                    case "200":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Commande de modification de droits accepté");
                    break;
                    case "402":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Erreur :"+codeErreur);
                    break;
                    default:
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+codeErreur);
                    break;
                }
            }
            break;
                
            case "nick":
            {
                switch (codeErreur)
                {
                    case "200":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("Vous pseudo est maintenant :"+message.get("arg0"));
                        changeNickname(message.get("arg0"));
                    break;
                    case "402":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR :"+codeErreur);
                    break;
                    default:
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR "+codeErreur);
                    break;
                }
            }
            break;
                case "part":
            {
                switch (codeErreur)
                {
                    case "200":
                        removeTab(message.get("arg0"));
                    break;
                    case "402":
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR :"+codeErreur);
                    break;
                    default:
                        (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR :"+codeErreur);
                    break;
                }
            }
            break;
            default:
                (listeOnglet.get(ongletCurrent)).displayTextInfo("ERREUR :"+codeErreur);
            break;
                    
        }
        
    }
    
    //fonction de traitement et affichage d'un message de l'output
    public void traitmentRv(Map<String,String> message){
        String commande=message.get("command");
        
        switch (commande.toLowerCase())
        {
            case "message":
            {
                (listeOnglet.get(message.get("cible"))).displayTextMessage(message.get("user"), message.get("message"));
            }
            break;
                
            case "info":
            {
                (listeOnglet.get(message.get("cible"))).displayTextInfo(message.get("message"));
            }
            break;
        }
                
    }
    
    public void traitementRetourBoucle(Map<String,String> message){
        String commande=message.get("command");
        
        switch (commande.toLowerCase())
        {
            case "message":
            {
                (listeOnglet.get(ongletCurrent)).displayPrivatMessage(message.get("argument"), message.get("source"), user.getNick());
            }
            break;
            case "mode":
            {
                String tmp[]=message.get("argument").split("#");
                String messageDroit="";
                for(String element : tmp){
                    messageDroit=element+" ";
                }
                (listeOnglet.get(ongletCurrent)).displayTextInfo(message.get("source")+"vous a donné les droits : "+messageDroit);
            }
            break;
            default:
            {
                (listeOnglet.get(ongletCurrent)).displayTextInfo("Commande : "+commande+" non reconnu");
            }
        }
    }

    //action appelé quand on appuie sur enter dans le champ de text
    @Override
    public void actionPerformed(ActionEvent e) {

        //traitmentEv(textUser.getText());
        try {
			traitmentEv(textUser.getText());
            } catch (SignatureException | PGPException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
	}
        textUser.setText(null);//on vide le champ de texte
        
    }
    
    //action appelé quand on clique sur un bouton, permet d'afficher un autre chan
    class OngletListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent event){
            ongletCurrent=((JButton) event.getSource()).getText();
            System.out.println(ongletCurrent);
            card.show(panelRight,ongletCurrent);
        }
    }
    
    //class qui écoute le server et attend les messages
    class ModuleReceptionServer extends Thread{
        private ZMQ.Socket reception;
        private Outils outils= new Outils();
        @Override
        public void run(){
            System.out.println("lancement du module de reception cle :"+connexionServer.getUidKey());
        reception = context.socket(ZMQ.PULL);
        reception.setIdentity(connexionServer.getUidKey().getBytes());
        reception.connect("tcp://localhost:5566");
        
        while(!stop){
            ZMsg msg = ZMsg.recvMsg(reception);
            System.out.println("reception d'un Zmsg");
            Map<String,String>  msgMap;
            msg.pop();
            if(msg.size()!=2) System.out.println("ZMsg trop long ou trop court");
            System.out.println("Recu frame1: "+ new String(msg.getFirst().getData()));
            System.out.println("Recu frame2: "+ new String(msg.getLast().getData()));
            msgMap=outils.parseOutput(new String(msg.getLast().getData()));
            msgMap.put("cible", new String(msg.getFirst().getData()));
            traitmentRv(msgMap);
        }
        reception.close();
        connexionServer.close();
        commandeServer.close();
        context.close();
        System.out.println("Fermeture du context");
        
    }
    }
    
}

