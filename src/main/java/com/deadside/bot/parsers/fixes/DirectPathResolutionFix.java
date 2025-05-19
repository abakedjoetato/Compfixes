package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct path resolution fix for CSV and Log files
 * This class provides direct path resolution for the parsers
 * with minimum overhead and maximum compatibility
 */
public class DirectPathResolutionFix {
    private static final Logger logger = LoggerFactory.getLogger(DirectPathResolutionFix.class);
    
    // Path cache - key format: "guildId:serverId:type"
    private static final Map<String, String> pathCache = new ConcurrentHashMap<>();
    
    // Standard CSV path patterns
    private static final List<String> CSV_PATH_PATTERNS = Arrays.asList(
        "{host}_{server}/actual1/deathlogs",
        "{host}_{server}/actual/deathlogs",
        "{host}/{server}/actual1/deathlogs",
        "{host}/{server}/actual/deathlogs",
        "{server}/actual1/deathlogs",
        "{server}/actual/deathlogs"
    );
    
    // Standard Log path patterns
    private static final List<String> LOG_PATH_PATTERNS = Arrays.asList(
        "{host}_{server}/Logs",
        "{host}_{server}/Deadside/Logs",
        "{host}/{server}/Logs",
        "{host}/{server}/Deadside/Logs",
        "{server}/Logs",
        "{server}/Deadside/Logs"
    );
    
    /**
     * Initialize the path resolution fix
     */
    public static void initialize() {
        logger.info("DirectPathResolutionFix initialized");
    }
    
    /**
     * Get a cached CSV path for a server
     * @param server The server
     * @return The cached path, or null if not found
     */
    public static String getCachedCsvPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        String cacheKey = getCacheKey(server, "csv");
        return pathCache.get(cacheKey);
    }
    
    /**
     * Get a cached Log path for a server
     * @param server The server
     * @return The cached path, or null if not found
     */
    public static String getCachedLogPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        String cacheKey = getCacheKey(server, "log");
        return pathCache.get(cacheKey);
    }
    
    /**
     * Resolve CSV path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or original if resolution failed
     */
    public static String resolveCsvPath(GameServer server, SftpConnector connector) {
        if (server == null) {
            return null;
        }
        
        try {
            // Check cache first
            String cacheKey = getCacheKey(server, "csv");
            String cachedPath = pathCache.get(cacheKey);
            
            if (cachedPath != null) {
                logger.debug("Using cached CSV path for server {}: {}", server.getName(), cachedPath);
                return cachedPath;
            }
            
            // Check current path
            String currentPath = server.getDeathlogsDirectory();
            if (isValidPath(currentPath) && testPath(server, currentPath, connector)) {
                cachePath(server, "csv", currentPath);
                return currentPath;
            }
            
            // Try to find a valid path
            String newPath = findValidPath(server, CSV_PATH_PATTERNS, connector);
            if (newPath != null) {
                // Update server and cache
                String originalPath = server.getDeathlogsDirectory();
                server.setDeathlogsDirectory(newPath);
                cachePath(server, "csv", newPath);
                
                logger.info("Fixed CSV path for server {}: {} -> {}", 
                    server.getName(), originalPath, newPath);
                
                return newPath;
            }
            
            // No valid path found
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getDeathlogsDirectory();
        }
    }
    
    /**
     * Resolve log path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or original if resolution failed
     */
    public static String resolveLogPath(GameServer server, SftpConnector connector) {
        if (server == null) {
            return null;
        }
        
        try {
            // Check cache first
            String cacheKey = getCacheKey(server, "log");
            String cachedPath = pathCache.get(cacheKey);
            
            if (cachedPath != null) {
                logger.debug("Using cached log path for server {}: {}", server.getName(), cachedPath);
                return cachedPath;
            }
            
            // Check current path
            String currentPath = server.getLogDirectory();
            if (isValidPath(currentPath) && testPath(server, currentPath, connector)) {
                cachePath(server, "log", currentPath);
                return currentPath;
            }
            
            // Try to find a valid path
            String newPath = findValidPath(server, LOG_PATH_PATTERNS, connector);
            if (newPath != null) {
                // Update server and cache
                String originalPath = server.getLogDirectory();
                server.setLogDirectory(newPath);
                cachePath(server, "log", newPath);
                
                logger.info("Fixed log path for server {}: {} -> {}", 
                    server.getName(), originalPath, newPath);
                
                return newPath;
            }
            
            // No valid path found
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getLogDirectory();
        }
    }
    
    /**
     * Find a valid path for a server
     * @param server The game server
     * @param patterns The path patterns to try
     * @param connector The SFTP connector
     * @return The valid path, or null if none found
     */
    private static String findValidPath(GameServer server, List<String> patterns, SftpConnector connector) {
        // Get server properties
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Try each pattern
        for (String pattern : patterns) {
            String path = pattern
                .replace("{host}", host)
                .replace("{server}", serverName);
            
            if (testPath(server, path, connector)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Check if a path is valid
     * @param path The path to check
     * @return True if valid
     */
    private static boolean isValidPath(String path) {
        return path != null && !path.isEmpty();
    }
    
    /**
     * Test if a path exists and is accessible
     * @param server The server
     * @param path The path to test
     * @param connector The SFTP connector
     * @return True if valid
     */
    private static boolean testPath(GameServer server, String path, SftpConnector connector) {
        try {
            return connector.testConnection(server, path);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get cache key for a server and type
     * @param server The server
     * @param type The path type (csv or log)
     * @return The cache key
     */
    private static String getCacheKey(GameServer server, String type) {
        return server.getGuildId() + ":" + server.getId() + ":" + type;
    }
    
    /**
     * Cache a path for a server and type
     * @param server The server
     * @param type The path type (csv or log)
     * @param path The path to cache
     */
    private static void cachePath(GameServer server, String type, String path) {
        String key = getCacheKey(server, type);
        pathCache.put(key, path);
    }
    
    /**
     * Fix paths for a server
     * @param server The server to fix
     * @param connector The SFTP connector
     * @return True if any paths were fixed
     */
    public static boolean fixServerPaths(GameServer server, SftpConnector connector) {
        boolean anyFixed = false;
        
        // Fix CSV path
        String originalCsvPath = server.getDeathlogsDirectory();
        String fixedCsvPath = resolveCsvPath(server, connector);
        
        if (fixedCsvPath != null && !fixedCsvPath.equals(originalCsvPath)) {
            server.setDeathlogsDirectory(fixedCsvPath);
            anyFixed = true;
        }
        
        // Fix log path
        String originalLogPath = server.getLogDirectory();
        String fixedLogPath = resolveLogPath(server, connector);
        
        if (fixedLogPath != null && !fixedLogPath.equals(originalLogPath)) {
            server.setLogDirectory(fixedLogPath);
            anyFixed = true;
        }
        
        return anyFixed;
    }
}