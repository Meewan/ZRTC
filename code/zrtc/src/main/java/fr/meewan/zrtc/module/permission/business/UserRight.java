/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.business;

import fr.meewan.zrtc.module.permission.entity.UserRightEntity;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author Meewan
 */
public class UserRight extends UserRightEntity
{
    public UserRight(String user, String chan, String command, Connection connection) throws SQLException
    {
        super(user, chan, command, connection);
    }
    public UserRight(String user, String chan, String command, String right, Connection connection) throws SQLException
    {
        super(user, chan, command, right, connection);
    }
}
