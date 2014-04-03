/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.utils;

/**
 *  Implémentation du design pattern observer
 * @author Meewan
 */
public interface Listener 
{
    public void fireEvent(Object o, String source);
}
