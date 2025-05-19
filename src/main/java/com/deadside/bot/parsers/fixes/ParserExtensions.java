package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser extensions for Deadside
 * This class provides extensions and utilities for the parsers
 */
public class ParserExtensions {
    private static final Logger logger = LoggerFactory.getLogger(ParserExtensions.class);
    private static boolean initialized = false;
    
    /**
     * Check if the parser extensions are initialized
     * @return True if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Initialize the parser extensions
     */
    public static void initialize() {
        initialized = true;
        logger.info("Parser extensions initialized");
    }
    
    /**
     * Process CSV path for a server
     * @param server The game server
     * @param path The CSV path
     * @return The processed path
     */
    public static String processCsvPath(GameServer server, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Normalize path
        String normalizedPath = path.replace('\\', '/');
        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }
        
        logger.debug("Processed CSV path for server {}: {} -> {}", 
            server.getName(), path, normalizedPath);
        
        return normalizedPath;
    }
    
    /**
     * Process log path for a server
     * @param server The game server
     * @param path The log path
     * @return The processed path
     */
    public static String processLogPath(GameServer server, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Normalize path
        String normalizedPath = path.replace('\\', '/');
        if (!normalizedPath.endsWith("/")) {
            normalizedPath += "/";
        }
        
        logger.debug("Processed log path for server {}: {} -> {}", 
            server.getName(), path, normalizedPath);
        
        return normalizedPath;
    }
    
    /**
     * Resolve and update CSV path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return True if successful
     */
    public static boolean resolveAndUpdateCsvPath(GameServer server, SftpConnector connector) {
        try {
            logger.info("Resolving and updating CSV path for server: {}", server.getName());
            
            // Find valid path
            String csvPath = SftpPathUtils.findCsvPath(server, connector);
            
            if (csvPath == null) {
                logger.warn("Could not find valid CSV path for server: {}", server.getName());
                return false;
            }
            
            // Update server
            String originalPath = server.getDeathlogsDirectory();
            server.setDeathlogsDirectory(csvPath);
            
            // Register path
            ParserIntegrationHooks.recordSuccessfulCsvPath(server, csvPath);
            
            logger.info("Updated CSV path for server {}: {} -> {}", 
                server.getName(), originalPath, csvPath);
            
            return true;
        } catch (Exception e) {
            logger.error("Error resolving and updating CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Resolve and update log path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return True if successful
     */
    public static boolean resolveAndUpdateLogPath(GameServer server, SftpConnector connector) {
        try {
            logger.info("Resolving and updating log path for server: {}", server.getName());
            
            // Find valid path
            String logPath = SftpPathUtils.findLogPath(server, connector);
            
            if (logPath == null) {
                logger.warn("Could not find valid log path for server: {}", server.getName());
                return false;
            }
            
            // Update server
            String originalPath = server.getLogDirectory();
            server.setLogDirectory(logPath);
            
            // Register path
            ParserIntegrationHooks.recordSuccessfulLogPath(server, logPath);
            
            logger.info("Updated log path for server {}: {} -> {}", 
                server.getName(), originalPath, logPath);
            
            return true;
        } catch (Exception e) {
            logger.error("Error resolving and updating log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if paths need resolution
     * @param server The game server
     * @param connector The SFTP connector
     * @return True if paths need resolution
     */
    public static boolean needsPathResolution(GameServer server, SftpConnector connector) {
        try {
            logger.debug("Checking if paths need resolution for server: {}", server.getName());
            
            // Check if CSV files can be found
            boolean csvFilesFound = !connector.findDeathlogFiles(server).isEmpty();
            
            // Check if log file can be found
            boolean logFileFound = connector.findLogFile(server) != null;
            
            boolean needsResolution = !csvFilesFound || !logFileFound;
            
            logger.debug("Server {} needs path resolution: {}", server.getName(), needsResolution);
            
            return needsResolution;
        } catch (Exception e) {
            logger.error("Error checking if paths need resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return true;
        }
    }
}