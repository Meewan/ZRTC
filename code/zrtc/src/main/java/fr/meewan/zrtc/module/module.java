/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module;

/**
 *
 * @author rpaoloni
 */
public interface module
{
    /**
     * méthode disant si le module doit être chargé en local
     * @return 
     */
    public boolean isActive();
    
    /**
     * méthode lançant le module dans un nouveau thread et gérant la 
     * configurationdu module
     */
    public void startModule();
    
    /**
     * méthode killant le module et le relançant de 0
     */
    public void restartModule();
    
    /**
     * Méthode rechargeant la configuration du module
     */
    public void reload();
}
