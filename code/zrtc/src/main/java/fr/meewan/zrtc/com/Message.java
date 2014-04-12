/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.com;

import java.util.List;

/**
 *Simple sutructure de données a faire passer par le réseau, aps d'intelligence
 * @author Meewan
 */
public class Message 
{
    public String raw;
    public String command;
    public List<String> args;
    public boolean authorized;
    public List<String> lifeCycle;
    public int state;   
}
