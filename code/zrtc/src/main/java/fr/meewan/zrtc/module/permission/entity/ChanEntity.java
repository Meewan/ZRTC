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
public class ChanEntity implements Entity
{
    protected int chanId;
    protected String chan;

    public ChanEntity() {
    }
    
    
    public ChanEntity(ResultSet rs) throws SQLException
    {
        chanId = rs.getInt("chan_id");
        chan = rs.getString("chan");
        
    }

    public ChanEntity(String chan) 
    {
        chanId = 0;
        this.chan = chan;
    }

    public ChanEntity(String chan, Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM chan WHERE chan = ? LIMIT 1");
        preparedStatement.setString(1, chan);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            this.chanId = rs.getInt("user_id");
            this.chan = rs.getString("user");
        }
    }
    
    
    @Override
    public void persist(Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        if(!exist(connection))
        {
            preparedStatement = connection.prepareStatement("INSERT INTO chan (chan) VALUES (?)");
            preparedStatement.setString(1, chan);
            preparedStatement.executeUpdate();
        }
        
    }

    @Override
    public boolean exist(Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT chan_id FROM chan WHERE chan = ? LIMIT 1");
        preparedStatement.setString(1, chan);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            return true;
        }
        return false;
    }

    public int getChanId() {
        return chanId;
    }

    public void setChanId(int chanId) {
        this.chanId = chanId;
    }

    public String getChan() {
        return chan;
    }

    public void setChan(String chan) {
        this.chan = chan;
    }
    
}