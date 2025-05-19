package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for resolving server file paths
 * Addresses path issues with Deadside.log and CSV files
 */
public class PathResolutionFix {
    private static final Logger logger = LoggerFactory.getLogger(PathResolutionFix.class);
    
    // Cache of successful paths to avoid repeated lookups
    private static final Map<String, String> pathCache = new ConcurrentHashMap<>();
    
    // Default patterns for CSV directories
    private static final List<String> CSV_PATTERNS = Arrays.asList(
        "{host}_{server}/actual1/deathlogs",
        "{host}_{server}/actual/deathlogs",
        "{host}/{server}/actual1/deathlogs",
        "{host}/{server}/actual/deathlogs",
        "{server}/actual1/deathlogs",
        "{server}/actual/deathlogs"
    );
    
    // Default patterns for log directories
    private static final List<String> LOG_PATTERNS = Arrays.asList(
        "{host}_{server}/Logs",
        "{host}_{server}/Deadside/Logs",
        "{host}/{server}/Logs",
        "{host}/{server}/Deadside/Logs",
        "{server}/Logs",
        "{server}/Deadside/Logs"
    );
    
    /**
     * Resolve the CSV directory path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or original if not resolvable
     */
    public static String resolveCsvPath(GameServer server, SftpConnector connector) {
        if (server == null) {
            return null;
        }
        
        // Get current path
        String currentPath = server.getDeathlogsDirectory();
        
        try {
            // Check if path is already valid
            if (isValidCsvPath(currentPath) && testPath(server, currentPath, connector)) {
                return currentPath;
            }
            
            // Check cache first
            String cacheKey = getCacheKey(server, "csv");
            String cachedPath = pathCache.get(cacheKey);
            
            if (cachedPath != null) {
                return cachedPath;
            }
            
            // Try to find a valid path using patterns
            String resolvedPath = findValidPathFromPatterns(server, CSV_PATTERNS, connector);
            
            if (resolvedPath != null) {
                // Cache the successful path
                pathCache.put(cacheKey, resolvedPath);
                
                logger.info("Resolved CSV path for server {}: {} -> {}", 
                    server.getName(), currentPath, resolvedPath);
                
                return resolvedPath;
            }
            
            // No resolution found, return original
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving CSV path for server {}: {}", 
                server.getName(), e.getMessage());
            return currentPath;
        }
    }
    
    /**
     * Resolve the log directory path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or original if not resolvable
     */
    public static String resolveLogPath(GameServer server, SftpConnector connector) {
        if (server == null) {
            return null;
        }
        
        // Get current path
        String currentPath = server.getLogDirectory();
        
        try {
            // Check if path is already valid
            if (isValidLogPath(currentPath) && testPath(server, currentPath, connector)) {
                return currentPath;
            }
            
            // Check cache first
            String cacheKey = getCacheKey(server, "log");
            String cachedPath = pathCache.get(cacheKey);
            
            if (cachedPath != null) {
                return cachedPath;
            }
            
            // Try to find a valid path using patterns
            String resolvedPath = findValidPathFromPatterns(server, LOG_PATTERNS, connector);
            
            if (resolvedPath != null) {
                // Cache the successful path
                pathCache.put(cacheKey, resolvedPath);
                
                logger.info("Resolved log path for server {}: {} -> {}", 
                    server.getName(), currentPath, resolvedPath);
                
                return resolvedPath;
            }
            
            // No resolution found, return original
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving log path for server {}: {}", 
                server.getName(), e.getMessage());
            return currentPath;
        }
    }
    
    /**
     * Fix paths for a specific server
     * @param server The server to fix
     * @param connector The SFTP connector
     * @param repository The game server repository
     * @return True if any paths were fixed
     */
    public static boolean fixServerPaths(GameServer server, 
                                      SftpConnector connector,
                                      GameServerRepository repository) {
        if (server == null || server.hasRestrictedIsolation()) {
            return false;
        }
        
        boolean updated = false;
        
        try {
            // Fix CSV path
            String originalCsvPath = server.getDeathlogsDirectory();
            String resolvedCsvPath = resolveCsvPath(server, connector);
            
            if (resolvedCsvPath != null && !resolvedCsvPath.equals(originalCsvPath)) {
                server.setDeathlogsDirectory(resolvedCsvPath);
                updated = true;
            }
            
            // Fix log path
            String originalLogPath = server.getLogDirectory();
            String resolvedLogPath = resolveLogPath(server, connector);
            
            if (resolvedLogPath != null && !resolvedLogPath.equals(originalLogPath)) {
                server.setLogDirectory(resolvedLogPath);
                updated = true;
            }
            
            // Save if updated
            if (updated && repository != null) {
                repository.save(server);
                logger.info("Fixed paths for server {}: CSV: {} -> {}, Log: {} -> {}", 
                    server.getName(), originalCsvPath, resolvedCsvPath, 
                    originalLogPath, resolvedLogPath);
            }
            
            return updated;
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a valid path from a list of patterns
     * @param server The game server
     * @param patterns The list of patterns
     * @param connector The SFTP connector
     * @return The valid path, or null if not found
     */
    private static String findValidPathFromPatterns(GameServer server, 
                                                List<String> patterns,
                                                SftpConnector connector) {
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
     * Check if a path is a valid CSV path
     * @param path The path to check
     * @return True if valid
     */
    private static boolean isValidCsvPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/actual1/deathlogs") || 
               path.contains("\\actual1\\deathlogs") ||
               path.contains("/actual/deathlogs") || 
               path.contains("\\actual\\deathlogs");
    }
    
    /**
     * Check if a path is a valid log path
     * @param path The path to check
     * @return True if valid
     */
    private static boolean isValidLogPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/Logs") || 
               path.contains("\\Logs");
    }
    
    /**
     * Test if a path exists
     * @param server The server
     * @param path The path to test
     * @param connector The SFTP connector
     * @return True if exists
     */
    private static boolean testPath(GameServer server, String path, SftpConnector connector) {
        try {
            return connector.testConnection(server, path);
        } catch (Exception e) {
            // Path doesn't exist or error
            return false;
        }
    }
    
    /**
     * Get a cache key for a server and type
     * @param server The server
     * @param type The path type (csv or log)
     * @return The cache key
     */
    private static String getCacheKey(GameServer server, String type) {
        return server.getGuildId() + ":" + server.getId() + ":" + type;
    }
    
    /**
     * Clear the path cache
     * Mainly for testing
     */
    public static void clearCache() {
        pathCache.clear();
    }
}