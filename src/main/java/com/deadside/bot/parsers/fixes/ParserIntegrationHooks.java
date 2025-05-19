package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hooks for parser integration
 * This class provides a central point for parser integrations
 */
public class ParserIntegrationHooks {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationHooks.class);
    
    // Path caches
    private static final Map<String, Map<String, String>> serverPathCache = new ConcurrentHashMap<>();
    
    /**
     * Get the registered CSV path for a server
     * @param server The game server
     * @return The registered CSV path, or null if not found
     */
    public static String getRegisteredCsvPath(GameServer server) {
        return getCachedCsvPath(server);
    }
    
    /**
     * Get the registered log path for a server
     * @param server The game server
     * @return The registered log path, or null if not found
     */
    public static String getRegisteredLogPath(GameServer server) {
        return getCachedLogPath(server);
    }
    
    /**
     * Get recommended CSV paths for a server
     * @param server The game server
     * @return List of recommended CSV paths
     */
    public static List<String> getRecommendedCsvPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
        try {
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
            
        } catch (Exception e) {
            logger.error("Error getting recommended CSV paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
        
        return paths;
    }
    
    /**
     * Get recommended log paths for a server
     * @param server The game server
     * @return List of recommended log paths
     */
    public static List<String> getRecommendedLogPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
        try {
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
            
        } catch (Exception e) {
            logger.error("Error getting recommended log paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
        
        return paths;
    }
    
    /**
     * Record a successful CSV path for a server
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulCsvPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return;
        }
        
        String serverId = server.getId().toString();
        
        // Get or create server cache
        Map<String, String> serverCache = serverPathCache.computeIfAbsent(serverId, k -> new HashMap<>());
        
        // Store path
        serverCache.put("csvPath", path);
        
        logger.info("Recorded successful CSV path for server {}: {}", server.getName(), path);
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
        
        String serverId = server.getId().toString();
        
        // Get or create server cache
        Map<String, String> serverCache = serverPathCache.computeIfAbsent(serverId, k -> new HashMap<>());
        
        // Store path
        serverCache.put("logPath", path);
        
        logger.info("Recorded successful log path for server {}: {}", server.getName(), path);
    }
    
    /**
     * Check if a server has a valid path
     * @param server The game server
     * @return True if the server has a valid path
     */
    public static boolean isValidPath(GameServer server) {
        if (server == null) {
            return false;
        }
        
        try {
            // Check deathlog directory
            String deathlogsDir = server.getDeathlogsDirectory();
            if (deathlogsDir == null || deathlogsDir.isEmpty()) {
                return false;
            }
            
            // Check log directory
            String logDir = server.getLogDirectory();
            if (logDir == null || logDir.isEmpty()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error checking if server {} has valid path: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if a server has a cached path
     * @param server The game server
     * @return True if the server has a cached path
     */
    public static boolean hasCachedPath(GameServer server) {
        if (server == null) {
            return false;
        }
        
        String serverId = server.getId().toString();
        
        // Check if server is in cache
        if (!serverPathCache.containsKey(serverId)) {
            return false;
        }
        
        // Get server cache
        Map<String, String> serverCache = serverPathCache.get(serverId);
        
        // Check if paths are in cache
        return serverCache.containsKey("csvPath") && serverCache.containsKey("logPath");
    }
    
    /**
     * Get cached CSV path for a server
     * @param server The game server
     * @return The cached CSV path, or null if not found
     */
    public static String getCachedCsvPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        String serverId = server.getId().toString();
        
        // Check if server is in cache
        if (!serverPathCache.containsKey(serverId)) {
            return null;
        }
        
        // Get server cache
        Map<String, String> serverCache = serverPathCache.get(serverId);
        
        // Return CSV path
        return serverCache.get("csvPath");
    }
    
    /**
     * Get cached log path for a server
     * @param server The game server
     * @return The cached log path, or null if not found
     */
    public static String getCachedLogPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        String serverId = server.getId().toString();
        
        // Check if server is in cache
        if (!serverPathCache.containsKey(serverId)) {
            return null;
        }
        
        // Get server cache
        Map<String, String> serverCache = serverPathCache.get(serverId);
        
        // Return log path
        return serverCache.get("logPath");
    }
    
    /**
     * Clear path cache for a server
     * @param server The game server
     */
    public static void clearPathCache(GameServer server) {
        if (server == null) {
            return;
        }
        
        String serverId = server.getId().toString();
        
        // Remove server from cache
        serverPathCache.remove(serverId);
        
        logger.info("Cleared path cache for server: {}", server.getName());
    }
    
    /**
     * Clear all path caches
     */
    public static void clearAllPathCaches() {
        // Clear all caches
        serverPathCache.clear();
        
        logger.info("Cleared all path caches");
    }
    
    /**
     * Check if hooks are registered
     * @return True if hooks are registered
     */
    public static boolean areHooksRegistered() {
        // We consider hooks registered if we have at least one server in the cache
        boolean registered = !serverPathCache.isEmpty();
        logger.debug("Hooks registered: {}", registered);
        return registered;
    }
    
    // Static flag to track hook registration status
    private static boolean hooksRegistered = false;
    
    /**
     * Register hooks with JDA and parsers
     * @param jda The JDA instance
     * @param csvParser The CSV parser
     * @param logParser The log parser
     * @param sftpConnector The SFTP connector
     */
    public static void registerHooks(
            net.dv8tion.jda.api.JDA jda, 
            com.deadside.bot.parsers.DeadsideCsvParser csvParser,
            com.deadside.bot.parsers.DeadsideLogParser logParser,
            com.deadside.bot.sftp.SftpConnector sftpConnector) {
            
        logger.info("Registering parser integration hooks");
        
        // Register event listeners if needed
        jda.addEventListener(new ParserEventListener(csvParser, logParser));
        
        // Set hooks as registered
        hooksRegistered = true;
        
        logger.info("Parser integration hooks registered successfully");
    }
}