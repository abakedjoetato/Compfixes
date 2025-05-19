package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Path finder for parsers
 * This class provides utilities for finding valid paths for parsers
 */
public class ParserPathFinder {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathFinder.class);
    
    private final SftpConnector connector;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     */
    public ParserPathFinder(SftpConnector connector) {
        this.connector = connector;
    }
    
    /**
     * Find a valid CSV path for a server
     * @param server The game server
     * @return The valid path, or null if not found
     */
    public String findValidCsvPath(GameServer server) {
        try {
            logger.info("Finding valid CSV path for server {}", server.getName());
            
            // Try current path first
            String currentPath = server.getDeathlogsDirectory();
            
            if (isValidCsvPath(server, currentPath)) {
                logger.info("Current CSV path is valid for server {}: {}", 
                    server.getName(), currentPath);
                return currentPath;
            }
            
            // Try alternative paths
            List<String> alternativePaths = getAlternativeCsvPaths(server);
            
            for (String path : alternativePaths) {
                if (isValidCsvPath(server, path)) {
                    logger.info("Found valid CSV path for server {}: {}", 
                        server.getName(), path);
                    return path;
                }
            }
            
            logger.warn("Could not find valid CSV path for server {}", 
                server.getName());
            return null;
        } catch (Exception e) {
            logger.error("Error finding valid CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Find a valid log path for a server
     * @param server The game server
     * @return The valid path, or null if not found
     */
    public String findValidLogPath(GameServer server) {
        try {
            logger.info("Finding valid log path for server {}", server.getName());
            
            // Try current path first
            String currentPath = server.getLogDirectory();
            
            if (isValidLogPath(server, currentPath)) {
                logger.info("Current log path is valid for server {}: {}", 
                    server.getName(), currentPath);
                return currentPath;
            }
            
            // Try alternative paths
            List<String> alternativePaths = getAlternativeLogPaths(server);
            
            for (String path : alternativePaths) {
                if (isValidLogPath(server, path)) {
                    logger.info("Found valid log path for server {}: {}", 
                        server.getName(), path);
                    return path;
                }
            }
            
            logger.warn("Could not find valid log path for server {}", 
                server.getName());
            return null;
        } catch (Exception e) {
            logger.error("Error finding valid log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if a CSV path is valid for a server
     * @param server The game server
     * @param path The path to check
     * @return True if the path is valid
     */
    private boolean isValidCsvPath(GameServer server, String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        try {
            // Save current path
            String currentPath = server.getDeathlogsDirectory();
            
            // Set the path to check
            server.setDeathlogsDirectory(path);
            
            // Check if path has CSV files
            List<String> files = connector.findDeathlogFiles(server);
            
            // Restore current path
            server.setDeathlogsDirectory(currentPath);
            
            return files != null && !files.isEmpty();
        } catch (Exception e) {
            logger.debug("Error checking CSV path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a log path is valid for a server
     * @param server The game server
     * @param path The path to check
     * @return True if the path is valid
     */
    private boolean isValidLogPath(GameServer server, String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        try {
            // Save current path
            String currentPath = server.getLogDirectory();
            
            // Set the path to check
            server.setLogDirectory(path);
            
            // Check if path has log file
            String logFile = connector.findLogFile(server);
            
            // Restore current path
            server.setLogDirectory(currentPath);
            
            return logFile != null && !logFile.isEmpty();
        } catch (Exception e) {
            logger.debug("Error checking log path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return false;
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
        
        return paths;
    }
}