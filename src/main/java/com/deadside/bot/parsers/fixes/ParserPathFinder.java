package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for finding parser paths
 * This class provides methods for finding valid parser paths
 * with intelligent caching and fallbacks
 */
public class ParserPathFinder {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathFinder.class);
    
    // Singleton instance
    private static ParserPathFinder instance;
    
    // Cache for successful paths
    private final Map<String, Map<String, String>> pathCache;
    
    // SFTP connector
    private SftpConnector sftpConnector;
    
    // Path validator
    private DeadsideParserValidator validator;
    
    /**
     * Private constructor for singleton pattern
     */
    private ParserPathFinder() {
        this.pathCache = new ConcurrentHashMap<>();
        logger.info("ParserPathFinder initialized");
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized ParserPathFinder getInstance() {
        if (instance == null) {
            instance = new ParserPathFinder();
        }
        return instance;
    }
    
    /**
     * Initialize with dependencies
     * @param sftpConnector The SFTP connector
     */
    public void initialize(SftpConnector sftpConnector) {
        this.sftpConnector = sftpConnector;
        this.validator = new DeadsideParserValidator(sftpConnector);
        logger.info("ParserPathFinder initialized with dependencies");
    }
    
    /**
     * Find a CSV path for a server
     * @param server The game server
     * @return The found path, or null if not found
     */
    public String findCsvPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        try {
            // Check cache first
            String cachedPath = getCachedPath(server, ParserPathTracker.CATEGORY_CSV);
            if (cachedPath != null) {
                // Verify the cached path still works
                if (validator.validateCsvPath(server, cachedPath)) {
                    logger.debug("Using cached CSV path for server {}: {}", 
                        server.getName(), cachedPath);
                    return cachedPath;
                } else {
                    // Cached path no longer valid, remove from cache
                    removeCachedPath(server, ParserPathTracker.CATEGORY_CSV);
                }
            }
            
            // Get paths to try
            List<String> pathsToTry = new ArrayList<>();
            
            // Add current path first if valid
            String currentPath = server.getDeathlogsDirectory();
            if (currentPath != null && !currentPath.isEmpty()) {
                pathsToTry.add(currentPath);
            }
            
            // Add recommended paths
            List<String> recommendedPaths = 
                ParserPathTracker.getInstance().getRecommendedPaths(
                    server, ParserPathTracker.CATEGORY_CSV);
            
            for (String path : recommendedPaths) {
                if (!pathsToTry.contains(path)) {
                    pathsToTry.add(path);
                }
            }
            
            // Try each path
            for (String path : pathsToTry) {
                if (validator.validateCsvPath(server, path)) {
                    // Valid path found, cache it
                    cacheSuccessfulPath(server, ParserPathTracker.CATEGORY_CSV, path);
                    logger.debug("Found valid CSV path for server {}: {}", 
                        server.getName(), path);
                    return path;
                }
            }
            
            logger.warn("No valid CSV path found for server {}", server.getName());
            return null;
        } catch (Exception e) {
            logger.error("Error finding CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Find a Log path for a server
     * @param server The game server
     * @return The found path, or null if not found
     */
    public String findLogPath(GameServer server) {
        if (server == null) {
            return null;
        }
        
        try {
            // Check cache first
            String cachedPath = getCachedPath(server, ParserPathTracker.CATEGORY_LOG);
            if (cachedPath != null) {
                // Verify the cached path still works
                if (validator.validateLogPath(server, cachedPath)) {
                    logger.debug("Using cached Log path for server {}: {}", 
                        server.getName(), cachedPath);
                    return cachedPath;
                } else {
                    // Cached path no longer valid, remove from cache
                    removeCachedPath(server, ParserPathTracker.CATEGORY_LOG);
                }
            }
            
            // Get paths to try
            List<String> pathsToTry = new ArrayList<>();
            
            // Add current path first if valid
            String currentPath = server.getLogDirectory();
            if (currentPath != null && !currentPath.isEmpty()) {
                pathsToTry.add(currentPath);
            }
            
            // Add recommended paths
            List<String> recommendedPaths = 
                ParserPathTracker.getInstance().getRecommendedPaths(
                    server, ParserPathTracker.CATEGORY_LOG);
            
            for (String path : recommendedPaths) {
                if (!pathsToTry.contains(path)) {
                    pathsToTry.add(path);
                }
            }
            
            // Try each path
            for (String path : pathsToTry) {
                if (validator.validateLogPath(server, path)) {
                    // Valid path found, cache it
                    cacheSuccessfulPath(server, ParserPathTracker.CATEGORY_LOG, path);
                    logger.debug("Found valid Log path for server {}: {}", 
                        server.getName(), path);
                    return path;
                }
            }
            
            logger.warn("No valid Log path found for server {}", server.getName());
            return null;
        } catch (Exception e) {
            logger.error("Error finding Log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Update a server's paths using the path finder
     * @param server The game server
     * @return True if any paths were updated
     */
    public boolean updateServerPaths(GameServer server) {
        if (server == null) {
            return false;
        }
        
        boolean anyUpdated = false;
        
        try {
            // Find CSV path
            String csvPath = findCsvPath(server);
            if (csvPath != null) {
                // Update server
                server.setDeathlogsDirectory(csvPath);
                anyUpdated = true;
            }
            
            // Find Log path
            String logPath = findLogPath(server);
            if (logPath != null) {
                // Update server
                server.setLogDirectory(logPath);
                anyUpdated = true;
            }
            
            return anyUpdated;
        } catch (Exception e) {
            logger.error("Error updating paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get a cached path
     * @param server The game server
     * @param category The path category
     * @return The cached path, or null if not found
     */
    private String getCachedPath(GameServer server, String category) {
        String serverKey = getServerKey(server);
        Map<String, String> serverPaths = pathCache.get(serverKey);
        
        if (serverPaths != null) {
            return serverPaths.get(category);
        }
        
        return null;
    }
    
    /**
     * Cache a successful path
     * @param server The game server
     * @param category The path category
     * @param path The successful path
     */
    private void cacheSuccessfulPath(GameServer server, String category, String path) {
        String serverKey = getServerKey(server);
        
        pathCache.computeIfAbsent(serverKey, k -> new HashMap<>())
            .put(category, path);
        
        // Also record in the global tracker
        ParserPathTracker.getInstance().recordSuccessfulPath(server, category, path);
    }
    
    /**
     * Remove a cached path
     * @param server The game server
     * @param category The path category
     */
    private void removeCachedPath(GameServer server, String category) {
        String serverKey = getServerKey(server);
        Map<String, String> serverPaths = pathCache.get(serverKey);
        
        if (serverPaths != null) {
            serverPaths.remove(category);
            
            // Remove the map if empty
            if (serverPaths.isEmpty()) {
                pathCache.remove(serverKey);
            }
        }
    }
    
    /**
     * Get a unique key for a server
     * @param server The game server
     * @return The unique key
     */
    private String getServerKey(GameServer server) {
        return server.getGuildId() + ":" + server.getId();
    }
    
    /**
     * Clear the path cache
     * Only use this for testing
     */
    public void clearCache() {
        pathCache.clear();
        logger.info("Path cache cleared");
    }
}