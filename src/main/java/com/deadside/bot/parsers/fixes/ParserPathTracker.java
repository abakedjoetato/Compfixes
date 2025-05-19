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
 * Tracks successful parser paths to improve reliability over time
 * This class caches successful path resolutions for better performance and reliability
 */
public class ParserPathTracker {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathTracker.class);
    
    // Path categories
    public static final String CATEGORY_CSV = "csv";
    public static final String CATEGORY_LOG = "log";
    
    // Singleton instance
    private static ParserPathTracker instance;
    
    // Cache for successful paths
    private Map<String, Map<String, List<String>>> serverPathCache;
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private ParserPathTracker() {
        serverPathCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized ParserPathTracker getInstance() {
        if (instance == null) {
            instance = new ParserPathTracker();
        }
        return instance;
    }
    
    /**
     * Record a successful path resolution
     * @param server The game server
     * @param category The path category (csv or log)
     * @param path The successful path
     */
    public void recordSuccessfulPath(GameServer server, String category, String path) {
        recordPath(server, category, path);
    }
    
    /**
     * Record a successful path resolution (internal implementation)
     * @param server The game server
     * @param category The path category (csv or log)
     * @param path The successful path
     */
    private void recordPath(GameServer server, String category, String path) {
        if (server == null || category == null || path == null) {
            return;
        }
        
        String serverId = server.getServerId();
        
        if (serverId == null || serverId.isEmpty()) {
            serverId = String.valueOf(server.getId());
        }
        
        String cacheKey = server.getGuildId() + "_" + serverId;
        
        serverPathCache.computeIfAbsent(cacheKey, k -> new HashMap<>())
            .computeIfAbsent(category, k -> new ArrayList<>())
            .add(path);
        
        logger.debug("Recorded successful {} path for server {}: {}", 
            category, server.getName(), path);
    }
    
    /**
     * Get recommended paths for a server and category
     * @param server The game server
     * @param category The path category
     * @return List of recommended paths
     */
    public List<String> getRecommendedPaths(GameServer server, String category) {
        if (server == null || category == null) {
            return new ArrayList<>();
        }
        
        String serverId = server.getServerId();
        
        if (serverId == null || serverId.isEmpty()) {
            serverId = String.valueOf(server.getId());
        }
        
        String cacheKey = server.getGuildId() + "_" + serverId;
        
        Map<String, List<String>> categoryMap = serverPathCache.get(cacheKey);
        
        if (categoryMap == null) {
            return new ArrayList<>();
        }
        
        List<String> paths = categoryMap.get(category);
        
        if (paths == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(paths);
    }
    
    /**
     * Clear the path cache for a server
     * @param server The game server
     */
    public void clearCache(GameServer server) {
        if (server == null) {
            return;
        }
        
        String serverId = server.getServerId();
        
        if (serverId == null || serverId.isEmpty()) {
            serverId = String.valueOf(server.getId());
        }
        
        String cacheKey = server.getGuildId() + "_" + serverId;
        
        serverPathCache.remove(cacheKey);
        
        logger.debug("Cleared path cache for server {}", server.getName());
    }
    
    /**
     * Clear the entire path cache
     */
    public void clearAllCaches() {
        serverPathCache.clear();
        logger.debug("Cleared all path caches");
    }
}