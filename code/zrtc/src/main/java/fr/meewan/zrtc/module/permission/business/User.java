/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.business;

import fr.meewan.zrtc.module.permission.entity.UserEntity;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Meewan
 */
public class User extends UserEntity{
    
    public User(ResultSet rs) throws SQLException
    {
        super(rs);
    }
    
    public User(String user)
    {
        super(user);
    }
    public User(String user, Connection connection) throws SQLException
    {
        super(user, connection);
    }
    
}
