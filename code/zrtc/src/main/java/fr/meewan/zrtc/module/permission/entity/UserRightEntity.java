/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Meewan
 */
public class UserRightEntity implements Entity
{
    protected int userId;
    protected int chanId;
    protected int commandId;
    protected String right;

    public UserRightEntity() {
    }
    
    
    public UserRightEntity(ResultSet rs) throws SQLException
    {
        userId = rs.getInt("user_id");
        chanId = rs.getInt("chan_id");
        commandId = rs.getInt("command_id");
        right = rs.getString("right");
        
    }

    public UserRightEntity(int userId, int chanId, int commandId, String right) {
        this.userId = userId;
        this.chanId = chanId;
        this.commandId = commandId;
        this.right = right;
    }


    public UserRightEntity(String user, String chan, String command, Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT ur.* FROM user_right AS ur, chan AS c, user AS u, list_ref_command as lrc WHERE u.user = ? AND c.chan = ? AND lrc.command_label = ? AND u.user_id=ur.user_id AND c.chan_id=ur.chan_id AND lrc.command_id=ur.command_id");
        preparedStatement.setString(1, user);
        preparedStatement.setString(2, chan);
        preparedStatement.setString(3, command);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            userId = rs.getInt("user_id");
            chanId = rs.getInt("chan_id");
            commandId = rs.getInt("command_id");
            right = rs.getString("right");
        }
        else
        {
            right = "default";
        }
    }
    public UserRightEntity(String user, String chan, String command, String right, Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT u.user_id, c.chan_id, lrc.command_id FROM chan AS c, user AS u, list_ref_command as lrc WHERE u.user = ? AND c.chan = ? AND lrc.command_label = ?");
        preparedStatement.setString(1, user);
        preparedStatement.setString(2, chan);
        preparedStatement.setString(3, command);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            userId = rs.getInt("user_id");
            chanId = rs.getInt("chan_id");
            commandId = rs.getInt("command_id");;
        }
        else
        {
            throw new SQLDataException();
        }
    }
    
    
    @Override
    public void persist(Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        if(exist(connection))
        {
            preparedStatement = connection.prepareStatement("UPDATE user_right SET right = ? WHERE user_id = ? AND command_id = ? AND chan_id = ?");
            preparedStatement.setInt(2, userId);
            preparedStatement.setInt(4, chanId);
            preparedStatement.setInt(3, commandId);
            preparedStatement.setString(1, right);
        }
        else
        {
            preparedStatement = connection.prepareStatement("INSERT INTO user_right (user_id, chan_id, comman_id, right) VALUES (?, ?, ?, ?)");
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, chanId);
            preparedStatement.setInt(3, commandId);
            preparedStatement.setString(4, right);
        }
        preparedStatement.executeUpdate();
        
    }

    @Override
    public boolean exist(Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT right FROM user_right WHERE user_id = ? AND chan_id = ? AND command_id = ? LIMIT 1");
        preparedStatement.setInt(1, userId);
        preparedStatement.setInt(2, chanId);
        preparedStatement.setInt(3, commandId);
        ResultSet rs = preparedStatement.executeQuery();
        return rs.next();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getChanId() {
        return chanId;
    }

    public void setChanId(int chanId) {
        this.chanId = chanId;
    }

    public int getCommandId() {
        return commandId;
    }

    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }
    
}