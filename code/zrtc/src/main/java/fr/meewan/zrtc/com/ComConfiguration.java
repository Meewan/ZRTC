/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.com;

import java.util.Map;

/**
 *Objet immutable destiné a la sérialisation permettant de connaitre toutes les informations du réseau
 * @author Meewan
 */
public class ComConfiguration 
{
    private Map<String, String> modulList;

    public ComConfiguration(Map<String, String> modulList) 
    {
        this.modulList = modulList;
    }
}
