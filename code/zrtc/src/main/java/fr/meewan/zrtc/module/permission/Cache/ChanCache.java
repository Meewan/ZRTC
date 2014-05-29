/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.Cache;

import java.sql.Connection;
import java.util.List;

/**
 *
 * @author Meewan
 */
public interface ChanCache {
    /**
     * méthode retournant true si la commande est générallement autorisé sur ce 
     * chan, false si elle est générallement interdite et null si il n'y a 
     * aucune donnée a ce propos.
     * @param chan
     * @param command
     * @param connection une connection a la bdd en lecture
     * @return 
     */
    public Boolean getChanPermission(String chan, String command, Connection connection);
    /**
     * Méthode insérant ou modifiant une permission pour la chan, si la permission
     * est "null" la ligne ets suprimmé
     * @param chan
     * @param command
     * @param right
     * @param connection une connection a la bdd en lecture
     * @return 
     */
    public Boolean setChanPermission(String chan, String command,String right, Connection connection);
    
    /**
     * méthode enregistrant la connection d'un utilisateur a un chan. et 
     * retournant true si tout va bien et false sinon
     * @param user
     * @param chan
     * @return 
     */
    public Boolean addUserToChan(String user, String chan);
    
    /**
     * Méthode retournant la liste des utilisateurs connecté a un chan.
     * @param chan
     * @return 
     */
    public List<String> getUsersOnChan(String chan);
    
    /**
     * Méthode changeant le droit (right) d'une commande sur le chan (chan) 
     * passé en argument et persiste cette donnée.
     * @param command
     * @param chan
     * @param connection une connection a la bdd en écriture
     * @return truee si la commande a été modifié, false sinon
     */
    public Boolean setChanCommand(String command, String chan, Connection connection);
    
    /**
     * Méthode créant un nouveau chan dans la bdd
     * @param chan le nom du chan a créer
     * @param connection
     * @return true si le chan a été créé, false sinon
     */
    public Boolean createNewChan(String chan, Connection connection);
    
    /**
     * Méthode retirant un utilisateur(user) du salon  (chan)
     * @param user
     * @param chan
     * @return true si tout s'est bien passé false sinon
     */
    public Boolean revoveUserFromChan(String user, String chan);
    
    
}
