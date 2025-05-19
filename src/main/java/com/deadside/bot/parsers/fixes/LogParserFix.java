package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixes for log parser issues
 */
public class LogParserFix {
    private static final Logger logger = LoggerFactory.getLogger(LogParserFix.class);
    
    /**
     * Resolve log path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or null if not found
     */
    public static String resolveServerLogPath(GameServer server, SftpConnector connector) {
        return SftpPathUtils.findLogPath(server, connector);
    }
    
    /**
     * Update server with resolved log path
     * @param server The game server
     * @param path The resolved path
     * @return True if successful
     */
    public static boolean updateServerLogPath(GameServer server, String path) {
        try {
            if (path == null || path.isEmpty()) {
                logger.warn("Cannot update server {} with empty log path", server.getName());
                return false;
            }
            
            // Update server path
            String originalPath = server.getLogDirectory();
            server.setLogDirectory(path);
            
            // Register path
            ParserIntegrationHooks.recordSuccessfulLogPath(server, path);
            
            logger.info("Updated log path for server {}: {} -> {}", 
                server.getName(), originalPath, path);
            
            return true;
        } catch (Exception e) {
            logger.error("Error updating log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
}