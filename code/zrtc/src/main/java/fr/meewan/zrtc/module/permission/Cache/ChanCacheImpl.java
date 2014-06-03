/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.Cache;

import fr.meewan.zrtc.module.permission.business.ChanRight;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meewan
 */
public class ChanCacheImpl implements ChanCache
{
    Map<String, ChanObject> cache;
    
    public ChanCacheImpl()
    {
        cache = new ConcurrentHashMap<>();
    }

    @Override
    public Boolean getChanPermission(String chan, String command, Connection connection) 
    {
        if(cache.containsKey(chan))
        {
            return cache.get(chan).getPermission(command);
        }
        else
        {
            return new ChanObject(chan, connection).getPermission(command);
        }
    }

    @Override
    public Boolean setChanPermission(String chan, String command, Boolean right, Connection connection) 
    {
        if(cache.containsKey(chan))
        {
            try 
            {
                return cache.get(chan).setPermission(command, right, connection);
            } 
            catch (SQLException ex) 
            {
                Logger.getLogger(ChanCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return false;
    }

    @Override
    public Boolean addUserToChan(String user, String chan, Connection connection) 
    {
        if(cache.containsKey(chan))
        {
            return cache.get(chan).addUser(user);
        }
        else
        {
            this.createNewChan(chan, connection);
            return cache.get(chan).addUser(user);
        }
    }

    @Override
    public List<String> getUsersOnChan(String chan) 
    {
        if(cache.containsKey(chan))
        {
            return cache.get(chan).getUsers();
        }
        return null;
    }

    @Override
    public Boolean createNewChan(String chan, Connection connection) 
    {
        if(!cache.containsKey(chan))
        {
            cache.put(chan, new ChanObject(chan, connection));
            return true;
        }
        return false;
    }

    @Override
    public Boolean revoveUserFromChan(String user, String chan) 
    {
        if(cache.containsKey(chan))
        {
            return cache.get(chan).removeUser(user);
        }
        return false;
    }
    
    private class ChanObject
    {
        private final List<String> users;
        private final String name;
        private int chanId;//id du chan, -1 si le chan est pas en base
        private final Map<String, String> rights;
        private boolean fromBase;

        public ChanObject(String name, Connection connection) 
        {
            this.name = name;
            this.fromBase = false;
            this.users = new CopyOnWriteArrayList<>();
            this.rights = new ConcurrentHashMap<>();
            try 
            {
                loadRightMap(connection);
            } 
            catch (SQLException ex) 
            {
                Logger.getLogger(ChanCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /**
         * Méthode chargent un chan depuis la base de donnée si il existe
         * @param connection
         * @throws SQLException 
         */
        private void loadRightMap(Connection connection) throws SQLException
        {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT c.chan_id FROM chan AS c WHERE c.chan = ? LIMIT 1");
            preparedStatement.setString(1, this.name);
            ResultSet rs = preparedStatement.executeQuery();
            if(rs.next())
            {
                this.fromBase = true;
                this.chanId = rs.getInt("chan_id");
            }
            if(fromBase)
            {
                preparedStatement = connection.prepareStatement("SELECT cr.right, lrf.command_label FROM chan_right AS cr, list_ref_command AS lrf WHERE cr.chan_id = ? AND lrf.command_id = ur.command_id ");
                preparedStatement.setInt(1, this.chanId);
                rs = preparedStatement.executeQuery();
                while(rs.next())
                {
                    rights.put(rs.getString("command_label"), rs.getString("right"));
                }
            }
        }

        public Boolean getPermission(String command) 
        {
            String right = this.rights.get(command);
            if (right == null)
            {
                return null;
            }
            if(right.toLowerCase().equals("true"))
            {
                return true;
            }
            if(right.toLowerCase().equals("false"))
            {
                return false;
            }
            return null;
        }

        public Boolean setPermission(String command, Boolean right, Connection connection) throws SQLException 
        {
            if(right == null)
            {
                this.rights.remove(command);
            }
            else
            {
                this.rights.put(command, right.toString());
            }
            if(fromBase && right != null)
            {
                ChanRight cr = new ChanRight(name, command, right.toString(), connection);
                cr.persist(connection);
                return true;
            }
            else if(fromBase && right == null)
            {
                ChanRight chanRight = new ChanRight(name, command, connection);
                PreparedStatement preparedStatement;
                preparedStatement = connection.prepareStatement("DELETE FROM chan_right WHERE command_id = ?, chan_id = ? ");
                preparedStatement.setInt(1, chanRight.getCommandId());
                preparedStatement.setInt(2, chanRight.getChanId());
                preparedStatement.executeUpdate();
                return true;
            }
            else
            {
                return true;
            }
        }
        public Boolean addUser(String user)
        {
            this.users.add(user);
            return true;
        }

        public List<String> getUsers() 
        {
            return users;
        }
        
        public boolean removeUser(String user)
        {
            return this.users.remove(user);
        }
        
    }
}
