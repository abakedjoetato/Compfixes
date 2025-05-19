package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.fixes.ParserExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple hooks to integrate with parsers for path resolution
 * Provides methods that can be called directly from parser classes
 */
public class ParserPathHooks {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathHooks.class);
    
    /**
     * Process CSV path for a server
     * @param server The server
     * @param originalPath Original path
     * @return Fixed path or original if not necessary
     */
    public static String processCsvPath(GameServer server, String originalPath) {
        try {
            if (ParserExtensions.isInitialized()) {
                return ParserExtensions.processCsvPath(server, originalPath);
            }
        } catch (Exception e) {
            logger.error("Error in CSV path hook: {}", e.getMessage(), e);
        }
        return originalPath;
    }
    
    /**
     * Process Log path for a server
     * @param server The server
     * @param originalPath Original path
     * @return Fixed path or original if not necessary
     */
    public static String processLogPath(GameServer server, String originalPath) {
        try {
            if (ParserExtensions.isInitialized()) {
                return ParserExtensions.processLogPath(server, originalPath);
            }
        } catch (Exception e) {
            logger.error("Error in Log path hook: {}", e.getMessage(), e);
        }
        return originalPath;
    }
}