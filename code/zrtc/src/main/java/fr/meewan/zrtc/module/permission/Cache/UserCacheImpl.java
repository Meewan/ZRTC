/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.Cache;

import fr.meewan.zrtc.module.permission.business.User;
import fr.meewan.zrtc.module.permission.business.UserRight;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation du cache d'utilisateurs
 * 
 * @author Meewan
 */
public class UserCacheImpl implements UserCache{
    private Map<String, UserObject> cache;
    private final String adminUser;
    private final String adminPassword;
    private final Connection connection;
    
    public UserCacheImpl(String adminUser, String adminPassword, Connection connection)
    {
        cache = new ConcurrentHashMap<>();
        this.adminPassword = adminPassword;
        this.adminUser = adminUser;
        this.connection = connection;
    }

    
    @Override
    public Boolean getUserPermission(String user, String chan, String command) 
    {
        if(cache.containsKey(user))
            return cache.get(user).getRight(command, chan);
        else
            return null;
    }

    @Override
    public Boolean getUserPermission(String user, String command) 
    {
        return getUserPermission(user, "", command);
    }

    @Override
    public Boolean checkPassword(String user, String password) 
    {
        if(cache.containsKey(user))
        {
            if(password.equals(cache.get(user).getPassword()) || cache.get(user).getPassword() == null)
            {
                cache.get(user).setRegistered(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPgpKey(String user) 
    {
        if(cache.containsKey(user))
            return cache.get(user).getPgpKey();
        else
            return null;
    }

    @Override
    public Boolean ConnectUser(String user, String pgpKey) 
    {
        if(!cache.containsKey(user) && user.length() > 0)
        {
            try 
            {
                cache.put(user, new UserObject(user, pgpKey,adminUser, adminPassword, connection));
                return true;
            } 
            catch (SQLException ex) 
            {
                Logger.getLogger(UserCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public Boolean UnconnectUser(String user) 
    {
        if(cache.containsKey(user))
        {
            cache.remove(user);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public Boolean registerUser(String user, String password) 
    {
        if(user.equals(adminUser))
        {
            return false;
        }
        try 
        {
            if(cache.containsKey(user) && password != null && password.length() > 3 && (new User(user)).exist(connection))
            {
                cache.get(user).setUserName(user);
                cache.get(user).setPassword(password);
                cache.get(user).setFromBase(true);
                cache.get(user).getUser().persist(connection);
                cache.get(user).registerRights(connection);
                cache.get(user).setRegistered(true);
                return true;
            }
        } 
        catch (SQLException ex) 
        {
            Logger.getLogger(UserCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return false;
    }

    @Override
    public Boolean setUserCommand(String user, String command, Boolean right) 
    {
        return setUserCommand(user, command, "", right);
    }

    @Override
    public Boolean setUserCommand(String user, String command, String chan, Boolean right) 
    {
        if(cache.containsKey(user))
            return cache.get(user).setCommandRight(command, chan, right, connection);
        else
            return false;
    }

    @Override
    public Boolean changeUserName(String oldName, String newName) 
    {
        if(cache.containsKey(oldName))
        {
            try 
            {
                if((new User(newName)).exist(connection))
                {
                    cache.get(newName).setRegistered(false);
                    cache.get(newName).setPassword((new User(newName, connection)).getPassowrd());
                }
            } 
            catch (SQLException ex) 
            {
                Logger.getLogger(UserCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            cache.put(newName, cache.get(oldName));
            cache.remove(oldName);
            return true;
        }
        return false;
    }

    @Override
    public boolean isConnected(String user) 
    {
        return cache.containsKey(user);
    }

    @Override
    public String getUid(String user) 
    {
        if(cache.containsKey(user))
        {
            return cache.get(user).getUid();
        }
        return null;
    }
    
    @Override
    public boolean addUserTochan(String user, String chan)
    {
        if(cache.containsKey(user))
        {
            return cache.get(user).joinChan(chan);
        }
        else
        {
            return false;
        }
    }
    
    @Override
    public boolean removeUserFromchan(String user, String chan)
    {
        if(cache.containsKey(user))
        {
            return cache.get(user).partChan(chan);
        }
        else
        {
            return false;
        }
    }
    
    @Override
    public List<String> getAllChanForUser(String user)
    {
        if(cache.containsKey(user))
        {
            return cache.get(user).getChans();
        }
        else
        {
            return null;
        }
    }
    
    private class UserObject
    {
        private boolean fromBase = false;
        private User user;
        private Map <String, Map<String, String>> rightMap;
        private List<String> chans;
        private final String pgpKey;
        private final String uid;
        //indique si la personne a entré son password ou non
        private boolean registered = false;
        //flag indiquant si l'utilisateur est l'admin (root) de l'application
        private boolean admin;

        public UserObject(String userName, String pgpKey,String adminUser, String adminPassword, Connection connection) throws SQLException 
        {
            this.uid = genrateUid(userName);
            this.pgpKey = pgpKey;
            chans = new CopyOnWriteArrayList<>();
            if(userName.equals(adminUser))//cas particulier ou l'administrateur (root) se connecte
            {
                admin = true;
                registered = false;
                user = new User(userName);
                user.setPassowrd(adminPassword);
                rightMap = new ConcurrentHashMap<>();
                fromBase = false;
                return;
            }
            else
            {
                admin = false;
            }
            //chargement du profil utilisateur
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM user WHERE user = ? LIMIT 1");
            preparedStatement.setString(1, userName);
            ResultSet rs = preparedStatement.executeQuery();
            if(rs.next())
            {
                user = new User(rs);
                fromBase = true;
                registered = false;
            }
            else
            {
                user = new User(userName);
            }
            
            if (fromBase)// on change le profil de droits
            {
                preparedStatement = connection.prepareStatement("SELECT lrc.command_label, c.chan, ur.right FROM user_right AS ur, chan AS c, user AS u, list_ref_command as lrc WHERE u.user = ? AND u.user_id=ur.user_id AND c.chan_id=ur.chan_id AND lrc.command_id=ur.command_id");
                preparedStatement.setString(1, userName);
                rs = preparedStatement.executeQuery();
                rightMap = new ConcurrentHashMap<>();
                while(rs.next())
                {
                    String command = rs.getString("command_label");
                    String chan = rs.getString("chan");
                    String right = rs.getString("right");
                    if( !rightMap.containsKey(command))
                    {
                        rightMap.put(command, new HashMap<String, String>());
                    }
                    rightMap.get(command).put(chan, right);
                }
            }
        }

        /**
         * retourne les droits pour une commande et un chan (si c'est une 
         * commande générique l'argument chan est une string vide)
         * @param command
         * @param chan
         * @return 
         */
        public Boolean getRight(String command, String chan) 
        {
            if(admin && registered)//si c'est l'admin et qu'il a entré son password alors il a tout les droits
            {
                return true;
            }
            if(!this.rightMap.containsKey(command))
            {
                return null;
            }
            String right = this.rightMap.get(command).get(chan);
            if (right != null && right.equals("true") && ((fromBase && registered) || !fromBase))
            {
                return true;
            }
            else if(right != null && right.equals("false"))
            {
                return false;
            }
            else
            {
                return null;
            }
        }

        /**
         * change les droits d'un utilisateur et les commits dans la bdd si 
         * l'utilisateur est enregistré
         * @param command
         * @param chan
         * @param right
         * @param connection
         * @return true si tout s'est bien passé, false sinon
         */
        public Boolean setCommandRight(String command, String chan, Boolean right, Connection connection) 
        {
            if(admin)
            {
                return false;
            }
            if (fromBase && right != null)
            {
                if(!updateRight(chan, command, right, connection))
                    return false;
            }
            else if(fromBase && right == null)
            {
                if(!deleteRight(chan, command, right, connection))
                    return false;
            }
            if(right != null)
            {
                this.rightMap.get(command).put(chan, right ? "true" : "false");
            }
            else
            {
                this.rightMap.get(command).remove(chan);
            }
            return true;
        }
        /**
         * genere une chaine aléatoire unique par utilisateur
         * @param userName
         * @return 
         */
        public String genrateUid(String userName) 
        {
            SecureRandom random = new SecureRandom();
            return userName+"-"+ new BigInteger(130, random).toString(32);
        }
        
        /**
         * enregistre tour l'arbre des droits en base
         * @param connection 
         */
        public void registerRights(Connection connection) 
        {
            for(String command : rightMap.keySet())
            {
                for(String chan : rightMap.get(command).keySet())
                {
                    updateRight(chan, command, rightMap.get(command).get(chan).toLowerCase().equals("true"), connection);
                }
            }
        }
        
        private boolean updateRight(String chan, String command, Boolean right, Connection connection)
        {
            try 
            {
                UserRight userRight = new UserRight(this.user.getUser(), chan, command, right ? "true" : "false", connection);
                userRight.persist(connection);
            } 
            catch (SQLException ex) 
            {
                Logger.getLogger(UserCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
        
        private boolean deleteRight(String chan, String command, Boolean right, Connection connection)
        {
            PreparedStatement preparedStatement;
            try 
            {
                UserRight userRight = new UserRight(this.user.getUser(), chan, command, right ? "true" : "false", connection);
                preparedStatement = connection.prepareStatement("DELETE FROM user_right WHERE command_id = ?, chan_id = ? user_id = ?");
                preparedStatement.setInt(1, userRight.getCommandId());
                preparedStatement.setInt(2, userRight.getChanId());
                preparedStatement.setInt(3, userRight.getUserId());
                preparedStatement.executeUpdate();
            } catch (SQLException ex) {
                Logger.getLogger(UserCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
        
        public boolean joinChan(String chan)
        {
            chans.add(chan);
            return true;
        }
        
        public boolean partChan(String chan)
        {
            if(!chans.contains(chan))
            {
                return false;
            }
            else
            {
                chans.remove(chan);
            return true;
            }
        }
        public String getPassword() 
        {
            return this.user.getPassowrd();
        }
        public void setPassword(String password) 
        {
            this.user.setPassowrd(password);
        }
        public String getPgpKey()
        {
            return this.pgpKey;
        }
        public User getUser()
        {
            return this.user;
        }
        public void setUserName(String user)
        {
            this.user.setUser(user);
        }
        public void setFromBase(boolean fromBase) 
        {
            this.fromBase = fromBase;
        }

        public void setRegistered(boolean registered) 
        {
            this.registered = registered;
        }
        public boolean getAdmin()
        {
            return admin;
        }

        public String getUid() 
        {
            return uid;
        }

        public List<String> getChans() {
            return chans;
        }
        
    }
}
