package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracker for parser path resolution issues and successes
 * This class helps diagnose file path issues by maintaining a record of successful
 * and failed path resolutions, which aids in debugging and self-healing.
 */
public class ParserPathTracker {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathTracker.class);
    private static final ParserPathTracker INSTANCE = new ParserPathTracker();
    
    // Track successful paths for each server by category
    private final Map<String, Map<String, String>> successfulPaths = new ConcurrentHashMap<>();
    
    // Track failed paths for each server by category
    private final Map<String, Map<String, String>> failedPaths = new ConcurrentHashMap<>();
    
    // Track path resolution attempts for diagnostics
    private final Map<String, Integer> resolutionAttempts = new ConcurrentHashMap<>();
    
    // Categories
    public static final String CATEGORY_CSV = "csv";
    public static final String CATEGORY_LOG = "log";
    
    private ParserPathTracker() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance
     */
    public static ParserPathTracker getInstance() {
        return INSTANCE;
    }
    
    /**
     * Record a successful path resolution
     * @param server The game server
     * @param category Path category (csv or log)
     * @param path The successful path
     */
    public void recordSuccessfulPath(GameServer server, String category, String path) {
        String serverKey = getServerKey(server);
        
        // Initialize maps if needed
        successfulPaths.computeIfAbsent(serverKey, k -> new HashMap<>());
        
        // Record successful path
        successfulPaths.get(serverKey).put(category, path);
        
        // Update attempt counter
        incrementResolutionAttempts(serverKey, category, true);
        
        logger.debug("Recorded successful {} path for server {}: {}", 
            category, server.getName(), path);
    }
    
    /**
     * Record a failed path resolution
     * @param server The game server
     * @param category Path category (csv or log)
     * @param path The failed path
     */
    public void recordFailedPath(GameServer server, String category, String path) {
        String serverKey = getServerKey(server);
        
        // Initialize maps if needed
        failedPaths.computeIfAbsent(serverKey, k -> new HashMap<>());
        
        // Record failed path
        failedPaths.get(serverKey).put(category, path);
        
        // Update attempt counter
        incrementResolutionAttempts(serverKey, category, false);
        
        logger.debug("Recorded failed {} path for server {}: {}", 
            category, server.getName(), path);
    }
    
    /**
     * Get the most recent successful path for a server/category
     * @param server The game server
     * @param category Path category (csv or log)
     * @return The most recent successful path, or null if none
     */
    public String getSuccessfulPath(GameServer server, String category) {
        String serverKey = getServerKey(server);
        
        if (successfulPaths.containsKey(serverKey) && 
            successfulPaths.get(serverKey).containsKey(category)) {
            return successfulPaths.get(serverKey).get(category);
        }
        
        return null;
    }
    
    /**
     * Check if a server has previously succeeded with a path for a category
     * @param server The game server
     * @param category Path category (csv or log)
     * @return True if a successful path exists
     */
    public boolean hasSuccessfulPath(GameServer server, String category) {
        return getSuccessfulPath(server, category) != null;
    }
    
    /**
     * Get a unique key for a server
     */
    private String getServerKey(GameServer server) {
        return server.getGuildId() + ":" + server.getName();
    }
    
    /**
     * Increment resolution attempts counter
     */
    private void incrementResolutionAttempts(String serverKey, String category, boolean success) {
        String key = serverKey + ":" + category + ":" + (success ? "success" : "failure");
        resolutionAttempts.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
    }
    
    /**
     * Get path resolution stats for diagnostics
     */
    public Map<String, Object> getPathResolutionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("successfulPathCount", successfulPaths.size());
        stats.put("failedPathCount", failedPaths.size());
        stats.put("resolutionAttempts", new HashMap<>(resolutionAttempts));
        
        return stats;
    }
    
    /**
     * Clear all tracking data
     * Use this when you want to reset the tracker state
     */
    public void clearAll() {
        successfulPaths.clear();
        failedPaths.clear();
        resolutionAttempts.clear();
        logger.info("Path tracker data cleared");
    }
}