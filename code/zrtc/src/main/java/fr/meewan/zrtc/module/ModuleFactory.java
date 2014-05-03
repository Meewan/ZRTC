/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module;

import fr.meewan.zrtc.module.command.CommandModule;

/**
 *
 * @author Meewan
 */
public class ModuleFactory 
{
    public static Module get(String ModuleName)
    {
        switch(ModuleName)
        {
            case "commandModule":
                return new CommandModule();
            default:
                return null;
        }
    }
}
