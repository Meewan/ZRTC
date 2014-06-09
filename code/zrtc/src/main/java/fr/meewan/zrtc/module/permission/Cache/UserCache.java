/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.Cache;

import java.util.List;

/**
 * Interface explicitant l'api pour le cache utilisateur
 * @author Meewan
 */
public interface UserCache 
{
    /**
     * Méthode retournant true si l'opération est authorisé par l'utilisateur 
     * "user" sur la canal "chan", false si elle ne l'est pas et null si il n'y a pas de data
     * @param user utilisateur concerné
     * @param chan Chan concerné
     * @param command Commande concerné
     * @return true/false/null
     */
    public Boolean getUserPermission(String user, String chan, String command);
    
    /**
     * Méthode retournant true si l'opération est authorisé par l'utilisateur 
     * "user" , false si elle ne l'est pas et null si il n'y a pas de data
     * @param user utilisateur concerné
     * @param command Commande concerné
     * @return true/false/null
     */
    public Boolean getUserPermission(String user, String command);
    
    /**
     * Méthode vérifiant le password d'un utilisateur est correcte et 
     * charges ses données si c'est le cas
     * @param user le unsername
     * @param password son password a tester
     * @return true si le password est le même, false sinon
     */
    public Boolean checkPassword(String user, String password);
    
    /**
     * retourne la clef pgp de l'utilisateur passé en argument
     * @param user l'utilisateur dont on veut la clef pgp
     * @return la clef pgp de l'utilisateur passé en argument
     */
    public String getPgpKey(String user);
    
    /**
     * Ineser un utilisateur en tant que connecté
     * @param user l'utilisateur a connecter
     * @param pgpKey la clef pgp que soummet cet utilisateur
     * @return true si cl'utilisateur a été connecté et false sinon
     */
    public Boolean ConnectUser(String user, String pgpKey);
    
    /**
     * Déconnecte un utilisateur 
     * @param user l'utilisateur a déconnecter
     * @return true si cl'utilisateur a été déconnecté et false sinon
     */
    public Boolean UnconnectUser(String user);
    
    /**
     * Méthode enregistrant et persistant un utilisateur dans la bdd
     * @param user l'utilisateur a enregistrer
     * @param password son password
     * @return 
     */
    public Boolean registerUser(String user, String password);
    
    /**
     * Méthode modifiant les droits d'un utilisateur et les persistant si 
     * l'utilisateur est enregistré
     * @param user utilisateur 
     * @param command commande concerné
     * @param right les droits a appliquer (null pour revenir aux droits par defaut)
     * @return true si l'opération a réussit et false sinon
     */
    public Boolean setUserCommand(String user, String command, Boolean right);
    
    /**
     * Méthode modifiant les droits d'un utilisateur et les persistant si 
     * l'utilisateur est enregistré
     * @param user utilisateur 
     * @param command commande concerné
     * @param chan le salon concerné
     * @param right les droits a appliquer (null pour revenir aux droits par defaut)
     * @return true si l'opération a réussit et false sinon
     */
    public Boolean setUserCommand(String user, String command, String chan,Boolean right);
    
    /**
     * Change le nom d'un utilisateur, il perd ses droits non présent dans le cache
     * @param oldName ancien nom 
     * @param newName nouveau nom
     * @return true si tout c'est bnin passé et false sinon
     */
    public Boolean changeUserName(String oldName,String newName);
    
    /**
     * Méthode retournant true si user est connecté et false sinon
     * @param user
     * @return 
     */
    public boolean isConnected(String user);
    
    /**
     * Méthode retournant l'identifiant unique de l'utilisateur sur le réseau
     * @param user
     * @return 
     */
    public String getUid(String user);
    
    /**
     * Retire un chan de la liste des chans auquel est connecté l'utilisateur "user"
     * @param user
     * @param chan
     * @return 
     */
    public boolean removeUserFromchan(String user, String chan);
    
    /**
     * ajoute un chan de la liste des chans auquel est connecté l'utilisateur "user"
     * @param user
     * @param chan
     * @return 
     */
    public boolean addUserTochan(String user, String chan);
    
    /**
     * retourne la liste des chans auxquels est enregistré l'utilisateur
     * @param user
     * @return 
     */
    public List<String> getAllChanForUser(String user);
}