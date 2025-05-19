package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hook for automatically repairing parser paths during execution
 * This class intercepts parser path access and resolves paths on demand
 */
public class ParserPathRepairHook {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathRepairHook.class);
    
    // SFTP connector reference
    private static SftpConnector sftpConnector;
    
    // Flag to control automatic repair
    private static boolean autoRepairEnabled = true;
    
    /**
     * Initialize the hook with dependencies
     * @param sftpConnector SFTP connector
     */
    public static void initialize(SftpConnector connector) {
        sftpConnector = connector;
        logger.info("ParserPathRepairHook initialized");
    }
    
    /**
     * Enable or disable automatic path repair
     * @param enabled Whether to enable auto repair
     */
    public static void setAutoRepairEnabled(boolean enabled) {
        autoRepairEnabled = enabled;
        logger.info("Automatic path repair {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Hook for getting CSV path
     * This method intercepts CSV path access and resolves the path if needed
     * @param server The game server
     * @param originalPath The original path
     * @return The resolved path, or original if resolution failed
     */
    public static String getCsvPathHook(GameServer server, String originalPath) {
        if (!autoRepairEnabled || server == null || sftpConnector == null) {
            return originalPath;
        }
        
        try {
            // Only try to resolve if the original path seems invalid
            if (originalPath == null || originalPath.isEmpty() || 
                (!originalPath.contains("/actual1/deathlogs") && 
                 !originalPath.contains("\\actual1\\deathlogs") &&
                 !originalPath.contains("/actual/deathlogs") && 
                 !originalPath.contains("\\actual\\deathlogs"))) {
                
                logger.info("CSV path for server {} appears to be invalid, attempting to resolve", 
                    server.getName());
                
                String resolvedPath = CsvParsingFix.resolveServerCsvPath(server, sftpConnector);
                
                if (resolvedPath != null && !resolvedPath.isEmpty() && 
                    !resolvedPath.equals(originalPath)) {
                    
                    logger.info("Resolved CSV path during hook: {} -> {}", 
                        originalPath, resolvedPath);
                    
                    return resolvedPath;
                }
            }
            
            return originalPath;
        } catch (Exception e) {
            logger.error("Error in CSV path hook: {}", e.getMessage(), e);
            return originalPath;
        }
    }
    
    /**
     * Hook for getting log path
     * This method intercepts log path access and resolves the path if needed
     * @param server The game server
     * @param originalPath The original path
     * @return The resolved path, or original if resolution failed
     */
    public static String getLogPathHook(GameServer server, String originalPath) {
        if (!autoRepairEnabled || server == null || sftpConnector == null) {
            return originalPath;
        }
        
        try {
            // Only try to resolve if the original path seems invalid
            if (originalPath == null || originalPath.isEmpty() || 
                (!originalPath.contains("/Logs") && !originalPath.contains("\\Logs"))) {
                
                logger.info("Log path for server {} appears to be invalid, attempting to resolve", 
                    server.getName());
                
                String resolvedPath = LogParserFix.resolveServerLogPath(server, sftpConnector);
                
                if (resolvedPath != null && !resolvedPath.isEmpty() && 
                    !resolvedPath.equals(originalPath)) {
                    
                    logger.info("Resolved log path during hook: {} -> {}", 
                        originalPath, resolvedPath);
                    
                    return resolvedPath;
                }
            }
            
            return originalPath;
        } catch (Exception e) {
            logger.error("Error in log path hook: {}", e.getMessage(), e);
            return originalPath;
        }
    }
    
    /**
     * Record a successful CSV path
     * This method is called when a CSV path is successfully used
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulCsvPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return;
        }
        
        try {
            String serverKey = getServerKey(server);
            
            // Add to CsvParsingFix cache
            CsvParsingFix.resolveServerCsvPath(server, sftpConnector);
            
            logger.debug("Recorded successful CSV path for server {}: {}", 
                server.getName(), path);
        } catch (Exception e) {
            logger.error("Error recording successful CSV path: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Record a successful log path
     * This method is called when a log path is successfully used
     * @param server The game server
     * @param path The successful path
     */
    public static void recordSuccessfulLogPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return;
        }
        
        try {
            String serverKey = getServerKey(server);
            
            // Add to LogParserFix cache
            LogParserFix.resolveServerLogPath(server, sftpConnector);
            
            logger.debug("Recorded successful log path for server {}: {}", 
                server.getName(), path);
        } catch (Exception e) {
            logger.error("Error recording successful log path: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get a unique key for a server
     * @param server The server
     * @return The key
     */
    private static String getServerKey(GameServer server) {
        return server.getGuildId() + ":" + server.getId();
    }
}