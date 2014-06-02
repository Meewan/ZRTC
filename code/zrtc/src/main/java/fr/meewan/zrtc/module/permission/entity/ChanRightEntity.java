/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Meewan
 */
public class ChanRightEntity implements Entity
{
    protected int chanId;
    protected int commandId;
    protected String right;

    public ChanRightEntity() {
    }
    
    
    public ChanRightEntity(ResultSet rs) throws SQLException
    {
        chanId = rs.getInt("chan_id");
        commandId = rs.getInt("command_id");
        right = rs.getString("right");
        
    }

    public ChanRightEntity(int chanId, int commandId, String right) {
        this.chanId = chanId;
        this.commandId = commandId;
        this.right = right;
    }

    public ChanRightEntity(String chan, String command, String right, Connection connection) throws SQLException
    {
        this(chan, command, connection);
        this.right = right;
    }

    public ChanRightEntity(String chan, String command, Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT cr.* FROM chan_right AS cr, chan AS c, list_command_ref AS lrc WHERE c.chan = ? AND lrc.command_label = ? AND lrc.command_id = cr.command_id AND c.chan_id=cr.chan_id");
        preparedStatement.setString(1, chan);
        preparedStatement.setString(2, command);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            chanId = rs.getInt("chan_id");
            commandId = rs.getInt("command_id");
            right = rs.getString("right");
        }
        else
        {
            right="default";
        }
    }
    
    
    @Override
    public void persist(Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        if(exist(connection))
        {
            preparedStatement = connection.prepareStatement("UPDATE chan_right SET right = ? WHERE chan_id = ? AND command_id = ?");
            preparedStatement.setInt(2, chanId);
            preparedStatement.setInt(3, commandId);
            preparedStatement.setString(1, right);
        }
        else
        {  
            preparedStatement = connection.prepareStatement("INSERT INTO chan_right ( chan_id, comman_id, right) VALUES (4 ?, ?, ?)");
            preparedStatement.setInt(1, chanId);
            preparedStatement.setInt(2, commandId);
            preparedStatement.setString(3, right);
        }
        preparedStatement.executeUpdate();
        
    }

    @Override
    public boolean exist(Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT right FROM chan_right WHERE chan_id = ? AND command_id = ? LIMIT 1");
        preparedStatement.setInt(1, chanId);
        preparedStatement.setInt(1, commandId);
        ResultSet rs = preparedStatement.executeQuery();
        return rs.next();
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