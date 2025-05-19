package com.deadside.bot.parsers.fixes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Path tracker for parser operations
 * Maintains a cache of successful path resolutions
 */
public class ParserPathTracker {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathTracker.class);
    
    // Cache maps for CSV and log file paths
    private static final Map<String, String> csvPathCache = new ConcurrentHashMap<>();
    private static final Map<String, String> logPathCache = new ConcurrentHashMap<>();
    
    /**
     * Track a successful CSV path resolution
     * @param serverId The unique server identifier
     * @param path The resolved path
     */
    public static void trackCsvPath(String serverId, String path) {
        if (serverId != null && path != null) {
            csvPathCache.put(serverId, path);
            logger.debug("Tracked CSV path for server {}: {}", serverId, path);
        }
    }
    
    /**
     * Track a successful log path resolution
     * @param serverId The unique server identifier
     * @param path The resolved path
     */
    public static void trackLogPath(String serverId, String path) {
        if (serverId != null && path != null) {
            logPathCache.put(serverId, path);
            logger.debug("Tracked log path for server {}: {}", serverId, path);
        }
    }
    
    /**
     * Get a tracked CSV path
     * @param serverId The unique server identifier
     * @return The tracked path, or null if not found
     */
    public static String getCsvPath(String serverId) {
        return serverId != null ? csvPathCache.get(serverId) : null;
    }
    
    /**
     * Get a tracked log path
     * @param serverId The unique server identifier
     * @return The tracked path, or null if not found
     */
    public static String getLogPath(String serverId) {
        return serverId != null ? logPathCache.get(serverId) : null;
    }
    
    /**
     * Clear the path cache for testing
     */
    public static void clearCache() {
        csvPathCache.clear();
        logPathCache.clear();
        logger.info("Path tracker cache cleared");
    }
    
    /**
     * Clear path cache for a specific server
     * @param serverId The unique server identifier
     */
    public static void clearServerCache(String serverId) {
        if (serverId != null) {
            csvPathCache.remove(serverId);
            logPathCache.remove(serverId);
            logger.info("Path tracker cache cleared for server {}", serverId);
        }
    }
    
    /**
     * Get a unique server identifier
     * @param guildId Guild ID
     * @param serverId Server ID
     * @return Unique identifier
     */
    public static String getUniqueId(long guildId, String serverId) {
        return guildId + ":" + serverId;
    }
}