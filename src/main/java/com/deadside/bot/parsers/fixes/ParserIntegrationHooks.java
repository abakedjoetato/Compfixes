package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration hooks for parsers
 * This class provides integration hooks for the CSV and Log parsers
 * to use the path resolution system
 */
public class ParserIntegrationHooks {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationHooks.class);
    
    /**
     * Get the CSV file path for a server
     * This method is called by the CSV parser to get the path
     * @param server The game server
     * @param originalPath The original path from the server
     * @return The resolved path
     */
    public static String getCsvFilePath(GameServer server, String originalPath) {
        try {
            // Call the integration manager if available
            if (ParserPathIntegrationManager.getInstance().isInitialized()) {
                return ParserPathIntegrationManager.getInstance().onCsvParserGetPath(server, originalPath);
            }
            
            // Otherwise, just return the original path
            return originalPath;
        } catch (Exception e) {
            logger.error("Error in CSV path hook: {}", e.getMessage(), e);
            return originalPath;
        }
    }
    
    /**
     * Get the Log file path for a server
     * This method is called by the Log parser to get the path
     * @param server The game server
     * @param originalPath The original path from the server
     * @return The resolved path
     */
    public static String getLogFilePath(GameServer server, String originalPath) {
        try {
            // Call the integration manager if available
            if (ParserPathIntegrationManager.getInstance().isInitialized()) {
                return ParserPathIntegrationManager.getInstance().onLogParserGetPath(server, originalPath);
            }
            
            // Otherwise, just return the original path
            return originalPath;
        } catch (Exception e) {
            logger.error("Error in Log path hook: {}", e.getMessage(), e);
            return originalPath;
        }
    }
    
    /**
     * Record a successful CSV path for a server
     * This method should be called by the CSV parser when a path is successful
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulCsvPath(GameServer server, String path) {
        try {
            // Record the successful path
            ParserPathTracker.getInstance().recordSuccessfulPath(
                server, ParserPathTracker.CATEGORY_CSV, path);
        } catch (Exception e) {
            logger.error("Error recording successful CSV path: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Record a successful Log path for a server
     * This method should be called by the Log parser when a path is successful
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulLogPath(GameServer server, String path) {
        try {
            // Record the successful path
            ParserPathTracker.getInstance().recordSuccessfulPath(
                server, ParserPathTracker.CATEGORY_LOG, path);
        } catch (Exception e) {
            logger.error("Error recording successful Log path: {}", e.getMessage(), e);
        }
    }
}