package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.fixes.DeadsideParserPathRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Patch for path resolution issues
 * This class provides methods for fixing path resolution issues
 */
public class PathResolutionPatch {
    private static final Logger logger = LoggerFactory.getLogger(PathResolutionPatch.class);
    
    private final SftpConnector connector;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     */
    public PathResolutionPatch(SftpConnector connector) {
        this.connector = connector;
    }
    
    /**
     * Fix path for a server
     * @param server The game server
     * @return True if the path was fixed successfully
     */
    public boolean fixPath(GameServer server) {
        try {
            logger.info("Fixing path for server {}", server.getName());
            
            // Test connection
            boolean connectionOk = connector.testConnection(server);
            
            if (!connectionOk) {
                logger.warn("Connection test failed for server {}", server.getName());
                return false;
            }
            
            // Try to fix CSV path
            boolean csvPathFixed = fixCsvPath(server);
            
            // Try to fix log path
            boolean logPathFixed = fixLogPath(server);
            
            return csvPathFixed || logPathFixed;
        } catch (Exception e) {
            logger.error("Error fixing path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fix CSV path for a server
     * @param server The game server
     * @return True if the CSV path was fixed successfully
     */
    private boolean fixCsvPath(GameServer server) {
        try {
            logger.info("Fixing CSV path for server {}", server.getName());
            
            // Get current CSV path
            String currentPath = server.getDeathlogsDirectory();
            
            if (currentPath == null || currentPath.isEmpty()) {
                logger.warn("Empty CSV path for server {}", server.getName());
                return false;
            }
            
            // Try current path
            List<String> csvFiles = tryCsvPath(server, currentPath);
            
            if (csvFiles != null && !csvFiles.isEmpty()) {
                logger.info("Current CSV path is working for server {}: {}", 
                    server.getName(), currentPath);
                return true;
            }
            
            // Try alternative paths
            List<String> alternativePaths = getAlternativeCsvPaths(server);
            
            for (String path : alternativePaths) {
                csvFiles = tryCsvPath(server, path);
                
                if (csvFiles != null && !csvFiles.isEmpty()) {
                    logger.info("Found working CSV path for server {}: {}", 
                        server.getName(), path);
                    
                    // Update server path
                    server.setDeathlogsDirectory(path);
                    
                    // Register path
                    DeadsideParserPathRegistry.getInstance().registerPath(
                        server, DeadsideParserPathRegistry.PATH_TYPE_CSV, path);
                    
                    return true;
                }
            }
            
            logger.warn("Could not find working CSV path for server {}", 
                server.getName());
            return false;
        } catch (Exception e) {
            logger.error("Error fixing CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fix log path for a server
     * @param server The game server
     * @return True if the log path was fixed successfully
     */
    private boolean fixLogPath(GameServer server) {
        try {
            logger.info("Fixing log path for server {}", server.getName());
            
            // Get current log path
            String currentPath = server.getLogDirectory();
            
            if (currentPath == null || currentPath.isEmpty()) {
                logger.warn("Empty log path for server {}", server.getName());
                return false;
            }
            
            // Try current path
            String logFile = tryLogPath(server, currentPath);
            
            if (logFile != null && !logFile.isEmpty()) {
                logger.info("Current log path is working for server {}: {}", 
                    server.getName(), currentPath);
                return true;
            }
            
            // Try alternative paths
            List<String> alternativePaths = getAlternativeLogPaths(server);
            
            for (String path : alternativePaths) {
                logFile = tryLogPath(server, path);
                
                if (logFile != null && !logFile.isEmpty()) {
                    logger.info("Found working log path for server {}: {}", 
                        server.getName(), path);
                    
                    // Update server path
                    server.setLogDirectory(path);
                    
                    // Register path
                    DeadsideParserPathRegistry.getInstance().registerPath(
                        server, DeadsideParserPathRegistry.PATH_TYPE_LOGS, path);
                    
                    return true;
                }
            }
            
            logger.warn("Could not find working log path for server {}", 
                server.getName());
            return false;
        } catch (Exception e) {
            logger.error("Error fixing log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Try a CSV path for a server
     * @param server The game server
     * @param path The path to try
     * @return List of CSV files, or null if the path doesn't work
     */
    private List<String> tryCsvPath(GameServer server, String path) {
        try {
            // Save current path
            String currentPath = server.getDeathlogsDirectory();
            
            // Set the path to try
            server.setDeathlogsDirectory(path);
            
            // Try to find CSV files
            List<String> csvFiles = connector.findDeathlogFiles(server);
            
            // Restore current path
            server.setDeathlogsDirectory(currentPath);
            
            return csvFiles;
        } catch (Exception e) {
            logger.debug("Error trying CSV path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Try a log path for a server
     * @param server The game server
     * @param path The path to try
     * @return The log file, or null if the path doesn't work
     */
    private String tryLogPath(GameServer server, String path) {
        try {
            // Save current path
            String currentPath = server.getLogDirectory();
            
            // Set the path to try
            server.setLogDirectory(path);
            
            // Try to find log file
            String logFile = connector.findLogFile(server);
            
            // Restore current path
            server.setLogDirectory(currentPath);
            
            return logFile;
        } catch (Exception e) {
            logger.debug("Error trying log path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
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