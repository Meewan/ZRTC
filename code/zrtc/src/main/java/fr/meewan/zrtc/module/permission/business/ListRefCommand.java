/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.business;

import fr.meewan.zrtc.module.permission.entity.ListRefCommandEntity;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Meewan
 */
public class ListRefCommand extends ListRefCommandEntity{

    public ListRefCommand(ResultSet rs) throws SQLException
    {
        super(rs);
    }
    public ListRefCommand(String commandLabel, String defaultRight) 
    {
        super(commandLabel, defaultRight);
    }
    
    
    
}
