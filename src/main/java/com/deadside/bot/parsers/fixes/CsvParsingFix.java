package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixes for CSV parsing issues
 */
public class CsvParsingFix {
    private static final Logger logger = LoggerFactory.getLogger(CsvParsingFix.class);
    
    /**
     * Resolve CSV path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or null if not found
     */
    public static String resolveServerCsvPath(GameServer server, SftpConnector connector) {
        return SftpPathUtils.findCsvPath(server, connector);
    }
    
    /**
     * Update server with resolved CSV path
     * @param server The game server
     * @param path The resolved path
     * @return True if successful
     */
    public static boolean updateServerCsvPath(GameServer server, String path) {
        try {
            if (path == null || path.isEmpty()) {
                logger.warn("Cannot update server {} with empty CSV path", server.getName());
                return false;
            }
            
            // Update server path
            String originalPath = server.getDeathlogsDirectory();
            server.setDeathlogsDirectory(path);
            
            // Register path
            ParserIntegrationHooks.recordSuccessfulCsvPath(server, path);
            
            logger.info("Updated CSV path for server {}: {} -> {}", 
                server.getName(), originalPath, path);
            
            return true;
        } catch (Exception e) {
            logger.error("Error updating CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
}