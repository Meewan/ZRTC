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
import java.sql.Statement;

/**
 *
 * @author Meewan
 */
public class UserEntity implements Entity
{
    protected int userId;
    protected String passowrd;
    protected String user;

    public UserEntity() {
    }
    
    
    public UserEntity(ResultSet rs) throws SQLException
    {
        userId = rs.getInt("user_id");
        passowrd = rs.getString("password");
        user = rs.getString("user");
        
    }
    
    public UserEntity(String user)
    {
        this.userId = 0;
        this.passowrd = null;
        this.user = user;
    }
    public UserEntity(String passowrd, String user) 
    {
        userId = 0;
        this.passowrd = passowrd;
        this.user = user;
    }

    public UserEntity(String user, Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM user WHERE user = ? LIMIT 1");
        preparedStatement.setString(1, user);
        ResultSet rs = preparedStatement.executeQuery();
        if(rs.next())
        {
            this.userId = rs.getInt("user_id");
            this.passowrd = rs.getString("password");
            this.user = rs.getString("user");
        }
    }
    
    
    @Override
    public void persist(Connection connection) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        if(exist(connection))
        {
            preparedStatement = connection.prepareStatement("UPDATE user SET password = ?, user = ? WHERE user = ?");
            preparedStatement.setString(3, user);
        }
        else
        {
            preparedStatement = connection.prepareStatement("INSERT INTO user (password, user) VALUES (?, ?)");
        }
        preparedStatement.setString(1, passowrd);
        preparedStatement.setString(2, user);
        preparedStatement.executeUpdate();
        
    }

    @Override
    public boolean exist(Connection connection) throws SQLException 
    {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT user_id FROM user WHERE user = ? LIMIT 1");
        preparedStatement.setString(1, user);
        ResultSet rs = preparedStatement.executeQuery();
        return rs.next();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPassowrd() {
        return passowrd;
    }

    public void setPassowrd(String passowrd) {
        this.passowrd = passowrd;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
}
