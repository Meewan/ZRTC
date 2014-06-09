/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.meewan.zrtc.module.permission;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;

/**
 *
 * @author Meewan
 */
public class PermissionConfiguration {
    private final int maxWorkers;
    private final String coreAddress;
    private final int corePort;
    private final String configAddress;
    private final int configPort;
    private final int listeningPort;
    private final boolean useSqlCache;
    
    private final String sqlUser;
    private final String sqlPassword;
    private final String sqlAdress;
    private final int sqlPort;
    private final String sqlDb;
    
    private String sqlCacheUser;
    private String sqlCachePassword;
    private String sqlCacheAdress;
    private int sqlCachePort;
    private String sqlCacheDb;
    
    private final String adminUser;
    private final String adminPassword;
    
    private final Map<String, String> commands;
    
    public PermissionConfiguration(String path) throws IOException
    {
        Ini config = getIni(path);
        Profile.Section mainSection = config.get("main");
        maxWorkers = Integer.parseInt(mainSection.get("workers"));
        listeningPort = Integer.parseInt(mainSection.get("listeningPort"));
        coreAddress = mainSection.get("coreAddress");
        corePort = Integer.parseInt(mainSection.get("corePort"));
        configAddress = mainSection.get("configAddress");
        configPort = Integer.parseInt(mainSection.get("configPort"));
        useSqlCache = Boolean.getBoolean(mainSection.get("usesqlcache"));
        
        Profile.Section sql = config.get("sql");
        sqlPort = Integer.parseInt(sql.get("sqlport"));
        sqlUser = sql.get("sqluser");
        sqlPassword = sql.get("sqlpassword");
        sqlAdress = sql.get("sqladress");
        sqlDb = sql.get("sqldb");
        
        if(useSqlCache)
        {
            sql = config.get("sqlcache");
            sqlCachePort = Integer.parseInt(sql.get("sqlport"));
            sqlCacheUser = sql.get("sqluser");
            sqlCachePassword = sql.get("sqlpassword");
            sqlCacheAdress = sql.get("sqladress");
            sqlCacheDb = sql.get("sqldb");
        }
        
        Profile.Section networkAdmin = config.get("networkadmin");
        adminUser = networkAdmin.get("adminuser");
        adminPassword = networkAdmin.get("adminpassword");
        
        Profile.Section command = config.get("command");
        commands = new HashMap<>();
        Set<String> commandSet = command.keySet();
        for(String commandName : commandSet)
        {
            commands.put(commandName, command.get(commandName));
        }
        
    }
    
    private Ini getIni(String path) throws IOException
    {
        if(File.separator.equals("\\"))
        {
            return (Ini) new Wini(new File(path));
        }
        else
        {
            return new Ini(new File(path));
        }
    }

    public int getMaxWorkers() {
        return maxWorkers;
    }

    public String getCoreAddress() {
        return coreAddress;
    }

    public int getCorePort() {
        return corePort;
    }

    public String getConfigAddress() {
        return configAddress;
    }

    public int getConfigPort() {
        return configPort;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getSqlUser() {
        return sqlUser;
    }

    public String getSqlPassword() {
        return sqlPassword;
    }

    public String getSqlAdress() {
        return sqlAdress;
    }

    public int getSqlPort() {
        return sqlPort;
    }

    public Map<String, String> getCommands() {
        return commands;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public boolean isUseSqlCache() {
        return useSqlCache;
    }

    public String getSqlDb() {
        return sqlDb;
    }

    public String getSqlCacheUser() {
        return sqlCacheUser;
    }

    public String getSqlCachePassword() {
        return sqlCachePassword;
    }

    public String getSqlCacheAdress() {
        return sqlCacheAdress;
    }

    public int getSqlCachePort() {
        return sqlCachePort;
    }

    public String getSqlCacheDb() {
        return sqlCacheDb;
    }
    
}
