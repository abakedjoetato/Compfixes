package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration hooks for parser fixes
 * This class provides static methods for integration with other components
 */
public class ParserIntegrationHooks {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationHooks.class);
    
    /**
     * Record a successful CSV path for a server
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulCsvPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return;
        }
        
        try {
            // Register the path in the registry
            DeadsideParserPathRegistry.getInstance().registerPath(
                server, DeadsideParserPathRegistry.PATH_TYPE_CSV, path);
            
            logger.info("Recorded successful CSV path for server {}: {}", 
                server.getName(), path);
        } catch (Exception e) {
            logger.error("Error recording successful CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Record a successful log path for a server
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulLogPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return;
        }
        
        try {
            // Register the path in the registry
            DeadsideParserPathRegistry.getInstance().registerPath(
                server, DeadsideParserPathRegistry.PATH_TYPE_LOGS, path);
            
            logger.info("Recorded successful log path for server {}: {}", 
                server.getName(), path);
        } catch (Exception e) {
            logger.error("Error recording successful log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Get a registered CSV path for a server
     * @param server The game server
     * @return The path, or null if not registered
     */
    public static String getRegisteredCsvPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        try {
            // Get the path from the registry
            return DeadsideParserPathRegistry.getInstance().getPath(
                server, DeadsideParserPathRegistry.PATH_TYPE_CSV);
        } catch (Exception e) {
            logger.error("Error getting registered CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get a registered log path for a server
     * @param server The game server
     * @return The path, or null if not registered
     */
    public static String getRegisteredLogPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        try {
            // Get the path from the registry
            return DeadsideParserPathRegistry.getInstance().getPath(
                server, DeadsideParserPathRegistry.PATH_TYPE_LOGS);
        } catch (Exception e) {
            logger.error("Error getting registered log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get recommended CSV paths for a server
     * @param server The game server
     * @return List of recommended paths
     */
    public static List<String> getRecommendedCsvPaths(GameServer server) {
        if (server == null) {
            return new ArrayList<>();
        }
        
        try {
            List<String> paths = new ArrayList<>();
            
            // Get server properties
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            // Add recommended paths
            paths.add(host + "_" + serverName + "/actual1/deathlogs");
            paths.add(host + "_" + serverName + "/actual/deathlogs");
            paths.add(host + "/" + serverName + "/actual1/deathlogs");
            paths.add(host + "/" + serverName + "/actual/deathlogs");
            paths.add(serverName + "/actual1/deathlogs");
            paths.add(serverName + "/actual/deathlogs");
            
            return paths;
        } catch (Exception e) {
            logger.error("Error getting recommended CSV paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recommended log paths for a server
     * @param server The game server
     * @return List of recommended paths
     */
    public static List<String> getRecommendedLogPaths(GameServer server) {
        if (server == null) {
            return new ArrayList<>();
        }
        
        try {
            List<String> paths = new ArrayList<>();
            
            // Get server properties
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            // Add recommended paths
            paths.add(host + "_" + serverName + "/Logs");
            paths.add(host + "_" + serverName + "/Deadside/Logs");
            paths.add(host + "/" + serverName + "/Logs");
            paths.add(host + "/" + serverName + "/Deadside/Logs");
            paths.add(serverName + "/Logs");
            paths.add(serverName + "/Deadside/Logs");
            
            return paths;
        } catch (Exception e) {
            logger.error("Error getting recommended log paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}