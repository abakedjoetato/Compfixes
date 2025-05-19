package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.fixes.ParserIntegrationHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for SFTP path operations
 * This class provides utilities for working with SFTP paths
 */
public class SftpPathUtils {
    private static final Logger logger = LoggerFactory.getLogger(SftpPathUtils.class);
    
    /**
     * Find CSV path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or null if not found
     */
    public static String findCsvPath(GameServer server, SftpConnector connector) {
        try {
            logger.debug("Finding CSV path for server: {}", server.getName());
            
            // Try registered path first
            String registeredPath = ParserIntegrationHooks.getRegisteredCsvPath(server);
            
            if (registeredPath != null && !registeredPath.isEmpty()) {
                logger.debug("Using registered CSV path: {}", registeredPath);
                return registeredPath;
            }
            
            // Try current path
            String currentPath = server.getDeathlogsDirectory();
            
            if (currentPath != null && !currentPath.isEmpty()) {
                // Save original path
                String originalPath = server.getDeathlogsDirectory();
                
                // Test the path
                List<String> files = connector.findDeathlogFiles(server);
                
                if (files != null && !files.isEmpty()) {
                    logger.debug("Current CSV path is valid: {}", currentPath);
                    return currentPath;
                }
            }
            
            // Try alternative paths
            List<String> alternativePaths = ParserIntegrationHooks.getRecommendedCsvPaths(server);
            
            for (String path : alternativePaths) {
                logger.debug("Trying alternative CSV path: {}", path);
                
                // Save original path
                String originalPath = server.getDeathlogsDirectory();
                
                // Set path to try
                server.setDeathlogsDirectory(path);
                
                // Test the path
                List<String> files = connector.findDeathlogFiles(server);
                
                // Restore original path
                server.setDeathlogsDirectory(originalPath);
                
                if (files != null && !files.isEmpty()) {
                    logger.debug("Found valid alternative CSV path: {}", path);
                    return path;
                }
            }
            
            logger.warn("Could not find valid CSV path for server: {}", server.getName());
            return null;
        } catch (Exception e) {
            logger.error("Error finding CSV path: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Find log path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or null if not found
     */
    public static String findLogPath(GameServer server, SftpConnector connector) {
        try {
            logger.debug("Finding log path for server: {}", server.getName());
            
            // Try registered path first
            String registeredPath = ParserIntegrationHooks.getRegisteredLogPath(server);
            
            if (registeredPath != null && !registeredPath.isEmpty()) {
                logger.debug("Using registered log path: {}", registeredPath);
                return registeredPath;
            }
            
            // Try current path
            String currentPath = server.getLogDirectory();
            
            if (currentPath != null && !currentPath.isEmpty()) {
                // Save original path
                String originalPath = server.getLogDirectory();
                
                // Test the path
                String logFile = connector.findLogFile(server);
                
                if (logFile != null && !logFile.isEmpty()) {
                    logger.debug("Current log path is valid: {}", currentPath);
                    return currentPath;
                }
            }
            
            // Try alternative paths
            List<String> alternativePaths = ParserIntegrationHooks.getRecommendedLogPaths(server);
            
            for (String path : alternativePaths) {
                logger.debug("Trying alternative log path: {}", path);
                
                // Save original path
                String originalPath = server.getLogDirectory();
                
                // Set path to try
                server.setLogDirectory(path);
                
                // Test the path
                String logFile = connector.findLogFile(server);
                
                // Restore original path
                server.setLogDirectory(originalPath);
                
                if (logFile != null && !logFile.isEmpty()) {
                    logger.debug("Found valid alternative log path: {}", path);
                    return path;
                }
            }
            
            logger.warn("Could not find valid log path for server: {}", server.getName());
            return null;
        } catch (Exception e) {
            logger.error("Error finding log path: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate alternative CSV paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    public static List<String> generateAlternativeCsvPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
        try {
            // Get server properties
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            // Add alternative paths
            paths.add(host + "_" + serverName + "/actual1/deathlogs");
            paths.add(host + "_" + serverName + "/actual/deathlogs");
            paths.add(host + "/" + serverName + "/actual1/deathlogs");
            paths.add(host + "/" + serverName + "/actual/deathlogs");
            paths.add(serverName + "/actual1/deathlogs");
            paths.add(serverName + "/actual/deathlogs");
        } catch (Exception e) {
            logger.error("Error generating alternative CSV paths: {}", e.getMessage(), e);
        }
        
        return paths;
    }
    
    /**
     * Generate alternative log paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    public static List<String> generateAlternativeLogPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
        try {
            // Get server properties
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            // Add alternative paths
            paths.add(host + "_" + serverName + "/Logs");
            paths.add(host + "_" + serverName + "/Deadside/Logs");
            paths.add(host + "/" + serverName + "/Logs");
            paths.add(host + "/" + serverName + "/Deadside/Logs");
            paths.add(serverName + "/Logs");
            paths.add(serverName + "/Deadside/Logs");
        } catch (Exception e) {
            logger.error("Error generating alternative log paths: {}", e.getMessage(), e);
        }
        
        return paths;
    }
}