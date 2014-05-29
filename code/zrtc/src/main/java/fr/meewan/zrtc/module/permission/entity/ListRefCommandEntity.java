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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meewan
 */
public class ListRefCommandEntity implements Entity
{
    protected int commandId;
    protected String commandLabel;
    protected String defaultRight;

    public ListRefCommandEntity() {
    }
    
    
    public ListRefCommandEntity(ResultSet rs) 
    {
        try
        {
        commandId = rs.getInt("command_id");
        commandLabel = rs.getString("command_label");
        defaultRight = rs.getString("default_right");
        }
        catch(Exception ex)
        {
            Logger.getLogger(ListRefCommandEntity.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public ListRefCommandEntity(String commandLabel, String defaultRight) 
    {
        commandId = 0;
        this.commandLabel = commandLabel;
        this.defaultRight = defaultRight;
    }

    public ListRefCommandEntity(String commandLabel, Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM list_ref_command WHERE command_label = ? LIMIT 1");
        preparedStatement.setString(1, commandLabel);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            this.commandId = rs.getInt("command_id");
            this.commandLabel = rs.getString("command_label");
            this.defaultRight = rs.getString("default_right");
        }
        else
        {
            this.commandLabel = commandLabel;
            this.defaultRight = "false";
        }
    }
    
    
    @Override
    public void persist(Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        if(exist(connection))
        {
            preparedStatement = connection.prepareStatement("UPDATE list_ref_command SET command_label = ?, default_right = ? WHERE command_label = ?");
            preparedStatement.setString(3, commandLabel);
        }
        else
        {
            preparedStatement = connection.prepareStatement("INSERT INTO list_ref_command (command_label, default_right) VALUES (?, ?)");
        }
        preparedStatement.setString(1, commandLabel);
        preparedStatement.setString(2, defaultRight);
        preparedStatement.executeUpdate();
        
    }

    @Override
    public boolean exist(Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT command_id FROM list_ref_command WHERE command_label = ? LIMIT 1");
        preparedStatement.setString(1, commandLabel);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            return true;
        }
        return false;
    }

    public int getCommandId() {
        return commandId;
    }

    public void setCommandId(int commandId) {
        this.commandId = commandId;
    }

    public String getCommandLabel() {
        return commandLabel;
    }

    public void setCommandLabel(String commandLabel) {
        this.commandLabel = commandLabel;
    }

    public String getDefaultRight() {
        return defaultRight;
    }

    public void setDefaultRight(String defaultRight) {
        this.defaultRight = defaultRight;
    }
    
}
