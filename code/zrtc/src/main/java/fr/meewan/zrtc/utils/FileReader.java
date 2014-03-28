/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author rpaoloni
 */
public class FileReader 
{
    public static String readFile(String path)
    {
        String fileContent = "";
        try
        {
            InputStream ips=new FileInputStream(path); 
            InputStreamReader ipsr=new InputStreamReader(ips);
            BufferedReader br=new BufferedReader(ipsr);
            String line;
            while ((line=br.readLine())!=null)
            {
                    
                    fileContent += line + "\n";
            }
            br.close(); 
        }		
        catch (IOException e)
        {
                System.out.println(e.toString());
        }
        return fileContent;
    }
}
