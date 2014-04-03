/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.zmq;

import fr.meewan.zrtc.utils.Listener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author Meewan
 */
public final class ZmqInterface
{
    //liste des écouteur type REP classé par ports
    protected Map<Integer, ZmqREP> REP;
    //liste des classes écoutant les evenements sur chaque connexion selon 
    //l'id de la connexion écouté
    protected Map<Integer, List<Listener>> REPListeners;
    
    public ZmqInterface()
    {
        REP = new HashMap<>();
        REPListeners = new HashMap<>();
    }
    
    /**
     * Méthode créant un contexte et un socket de type rep
     * @param port port a écouter
     * @param observer classe voulant s'abonner a l'evenement
     * @return true si tout c'est bien passé, false sinon
     */
    public boolean REP(int port, Listener observer)
    {
        return false;
    }
}
