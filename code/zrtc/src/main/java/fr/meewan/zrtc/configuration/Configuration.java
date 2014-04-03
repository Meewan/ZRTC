/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.configuration;

import fr.meewan.zrtc.utils.FileReader;
import java.io.File;
import java.io.IOException;
import org.ini4j.Ini;
import org.ini4j.Wini;

/**
 * Classe abstraite reprenant la logique de configuration commune aux modules et au core
 * @author rpaoloni
 */
public abstract class Configuration 
{
    protected String configurationFilePath;
    protected String publicKey;
    protected int publicListeningPort;
    
    /**
     * méthode récuperant un objet ini que l'on soit sous windows ou sous unix
     * sans avoir a se soucier de l'os
     * @return le Ini de la configuration
     * @throws IOException si le fichier n'existe pas ou n'est pas lisible 
     */
    protected Ini getIni() throws IOException
    {
        if(File.separator.equals("\\"))
        {
            return (Ini) new Wini(new File(configurationFilePath));
        }
        else
        {
            return new Ini(new File(configurationFilePath));
        }
    }
    
    /**
     * surcharge pour automatiser l'utilisation du chemin vers la configuration
     * @param configurationFilePath chemin vers le fichier de configuration
     * @return un objet Ini contenant la configuration
     * @throws java.io.IOException si le fichier n'existe pas ou n'est pas lisible 
     */
    protected Ini getIni(String configurationFilePath) throws IOException
    {
        this.configurationFilePath = configurationFilePath;
        return getIni();
    }
    
    /**
     * méthode retournant la clef qu'elle soit sous la forme d'une clé ou d'un path précédé par "path:"
     * @param potentialKey
     * @return 
     */
    protected String parseKey(String potentialKey)
    {
        if (potentialKey != null && (potentialKey.substring(0, 5).equals("path:") || potentialKey.substring(0, 6).equals("path :")))
        {
            String[] tmp = potentialKey.split(":");
            String path = tmp[1];
            //on recolle les morceaux au cas ou il y ai un ":" dans le pah
            for(int i = 2; i < tmp.length; i++)
            {
                path += ":" + tmp[i];
            }
            return FileReader.readFile(path);
        }
        else
        {
            return potentialKey;
        }
    }
    
}
