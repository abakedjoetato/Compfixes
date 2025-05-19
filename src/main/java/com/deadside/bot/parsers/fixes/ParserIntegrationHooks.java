package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration hooks for parser path fixes
 * Provides integration points between the enhanced SFTP connector and
 * the path tracking system
 */
public class ParserIntegrationHooks {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationHooks.class);
    
    /**
     * Record a successful CSV path for a server
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulCsvPath(GameServer server, String path) {
        try {
            if (server == null || path == null || path.isEmpty()) {
                return;
            }
            
            ParserPathTracker.getInstance().recordPath(
                server, 
                ParserPathTracker.CATEGORY_CSV, 
                path
            );
            
            logger.debug("Recorded successful CSV path for server {}: {}", 
                server.getName(), path);
        } catch (Exception e) {
            logger.error("Error recording successful CSV path: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Record a successful log path for a server
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulLogPath(GameServer server, String path) {
        try {
            if (server == null || path == null || path.isEmpty()) {
                return;
            }
            
            ParserPathTracker.getInstance().recordPath(
                server, 
                ParserPathTracker.CATEGORY_LOG, 
                path
            );
            
            logger.debug("Recorded successful log path for server {}: {}", 
                server.getName(), path);
        } catch (Exception e) {
            logger.error("Error recording successful log path: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get recommended CSV paths for a server
     * @param server The game server
     * @return The recommended paths
     */
    public static java.util.List<String> getRecommendedCsvPaths(GameServer server) {
        try {
            if (server == null) {
                return new java.util.ArrayList<>();
            }
            
            return ParserPathTracker.getInstance().getRecommendedPaths(
                server, 
                ParserPathTracker.CATEGORY_CSV
            );
        } catch (Exception e) {
            logger.error("Error getting recommended CSV paths: {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Get recommended log paths for a server
     * @param server The game server
     * @return The recommended paths
     */
    public static java.util.List<String> getRecommendedLogPaths(GameServer server) {
        try {
            if (server == null) {
                return new java.util.ArrayList<>();
            }
            
            return ParserPathTracker.getInstance().getRecommendedPaths(
                server, 
                ParserPathTracker.CATEGORY_LOG
            );
        } catch (Exception e) {
            logger.error("Error getting recommended log paths: {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
}