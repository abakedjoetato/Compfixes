package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.fixes.ParserIntegrationHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapted SFTP connector that adds improved path resolution
 * This class provides enhanced path resolution capabilities
 * for server log and CSV files
 */
public class SftpAdaptedConnector {
    private static final Logger logger = LoggerFactory.getLogger(SftpAdaptedConnector.class);
    
    private final SftpConnector connector;
    
    /**
     * Constructor
     * @param connector The SFTP connector to adapt
     */
    public SftpAdaptedConnector(SftpConnector connector) {
        this.connector = connector;
    }
    
    /**
     * Find CSV files with fallback mechanisms
     * @param server The game server
     * @return List of CSV files
     */
    public List<String> findCsvFilesWithFallback(GameServer server) {
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
                logger.debug("Trying alternative CSV path for server {}: {}", 
                    server.getName(), path);
                
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
     * @return The log file path
     */
    public String findLogFileWithFallback(GameServer server) {
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
                logger.debug("Trying alternative log path for server {}: {}", 
                    server.getName(), path);
                
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
     * Test connection to a server
     * @param server The game server
     * @return True if connected successfully
     */
    public boolean testConnection(GameServer server) {
        try {
            return connector.testConnection(server);
        } catch (Exception e) {
            logger.error("Error testing connection: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Read a file from the server
     * @param server The game server
     * @param filePath The file path
     * @return The file content
     */
    public String readFile(GameServer server, String filePath) {
        try {
            return connector.readFile(server, filePath);
        } catch (Exception e) {
            logger.error("Error reading file: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get alternative CSV paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private List<String> getAlternativeCsvPaths(GameServer server) {
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
    private List<String> getAlternativeLogPaths(GameServer server) {
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