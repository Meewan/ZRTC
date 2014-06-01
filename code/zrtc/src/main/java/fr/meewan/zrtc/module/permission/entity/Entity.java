/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission.entity;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author Meewan
 */
public interface Entity 
{
    /**
     * enregistre l'entity en base 
     * @param connection
     * @throws Exception 
     */
    public void persist(Connection connection) throws SQLException;
    /**
     * regarde si un objet equivalent existe en base
     * @param connection
     * @return
     * @throws Exception 
     */
    public boolean exist(Connection connection) throws SQLException;
}
