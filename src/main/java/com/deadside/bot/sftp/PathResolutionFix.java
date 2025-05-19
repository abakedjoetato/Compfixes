package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.fixes.ParserIntegrationHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides improved path resolution for server log and CSV files
 * This class integrates with the existing SFTP connector and adds
 * fallback paths and discovery logic to handle different server configurations
 */
public class PathResolutionFix {
    private static final Logger logger = LoggerFactory.getLogger(PathResolutionFix.class);
    
    /**
     * Find CSV files with fallback mechanisms
     * @param server The game server
     * @param connector The SFTP connector
     * @return List of CSV files
     */
    public static List<String> findCsvFilesWithFallback(GameServer server, SftpConnector connector) {
        try {
            // First try the standard method
            List<String> files = connector.findDeathlogFiles(server);
            
            if (files != null && !files.isEmpty()) {
                // Record successful path
                ParserIntegrationHooks.recordSuccessfulCsvPath(server, server.getDeathlogsDirectory());
                return files;
            }
            
            // If standard path failed, try alternative paths
            List<String> alternativePaths = getAlternativeCsvPaths(server);
            
            for (String path : alternativePaths) {
                logger.debug("Trying alternative CSV path: {}", path);
                
                // Update the server directory for the attempt
                String originalPath = server.getDeathlogsDirectory();
                server.setDeathlogsDirectory(path);
                
                try {
                    files = connector.findDeathlogFiles(server);
                    
                    if (files != null && !files.isEmpty()) {
                        // Record successful path
                        ParserIntegrationHooks.recordSuccessfulCsvPath(server, path);
                        return files;
                    }
                } catch (Exception e) {
                    logger.debug("Error with alternative path {}: {}", path, e.getMessage());
                } finally {
                    // Restore original path
                    server.setDeathlogsDirectory(originalPath);
                }
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error finding CSV files with fallback: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find log file with fallback mechanisms
     * @param server The game server
     * @param connector The SFTP connector
     * @return The log file path
     */
    public static String findLogFileWithFallback(GameServer server, SftpConnector connector) {
        try {
            // First try the standard method
            String logFile = connector.findLogFile(server);
            
            if (logFile != null && !logFile.isEmpty()) {
                // Record successful path
                ParserIntegrationHooks.recordSuccessfulLogPath(server, server.getLogDirectory());
                return logFile;
            }
            
            // If standard path failed, try alternative paths
            List<String> alternativePaths = getAlternativeLogPaths(server);
            
            for (String path : alternativePaths) {
                logger.debug("Trying alternative log path: {}", path);
                
                // Update the server directory for the attempt
                String originalPath = server.getLogDirectory();
                server.setLogDirectory(path);
                
                try {
                    logFile = connector.findLogFile(server);
                    
                    if (logFile != null && !logFile.isEmpty()) {
                        // Record successful path
                        ParserIntegrationHooks.recordSuccessfulLogPath(server, path);
                        return logFile;
                    }
                } catch (Exception e) {
                    logger.debug("Error with alternative path {}: {}", path, e.getMessage());
                } finally {
                    // Restore original path
                    server.setLogDirectory(originalPath);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error finding log file with fallback: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get alternative CSV paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private static List<String> getAlternativeCsvPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
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
        
        // Add recommended paths from cache
        List<String> recommendedPaths = ParserIntegrationHooks.getRecommendedCsvPaths(server);
        for (String path : recommendedPaths) {
            if (!paths.contains(path)) {
                paths.add(path);
            }
        }
        
        return paths;
    }
    
    /**
     * Get alternative log paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private static List<String> getAlternativeLogPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
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
        
        // Add recommended paths from cache
        List<String> recommendedPaths = ParserIntegrationHooks.getRecommendedLogPaths(server);
        for (String path : recommendedPaths) {
            if (!paths.contains(path)) {
                paths.add(path);
            }
        }
        
        return paths;
    }
}