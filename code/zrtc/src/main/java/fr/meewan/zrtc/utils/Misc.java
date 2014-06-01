/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Meewan
 */
public class Misc 
{
    /**
     * retourne une liste des element présents dans les deux sets passé en argument
     * @param set1 le est a testeer
     * @param set2 le set de reference
     * @return la liste des elements présents dans les deux sets passé en argument
     */
    public static List<?> compareSet(Set<?> set1, Set<?> set2)
    {
        List <Object> output = new ArrayList<>();
        //sécurité pour éviter les NPE
        if(set1 == null || set2 == null)
        {
            return output;
        }
        for(Object o1 : set1 )
        {
            boolean flag = false;
            for(Object o2 : set2 )
            {
                if(o1 != null && o1.equals(o2))
                {
                    output.add(o2);
                }
            }
        }
        return output;
    }
}
