package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration for path fixes
 * This class provides integration points for path fixes
 */
public class PathFixIntegration {
    private static final Logger logger = LoggerFactory.getLogger(PathFixIntegration.class);
    
    private final SftpConnector connector;
    private final GameServerRepository serverRepository;
    private boolean initialized = false;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param serverRepository The server repository
     */
    public PathFixIntegration(SftpConnector connector, GameServerRepository serverRepository) {
        this.connector = connector;
        this.serverRepository = serverRepository;
    }
    
    /**
     * Initialize path fix integration
     */
    public void initialize() {
        logger.info("Initializing path fix integration");
        
        try {
            // Initialize path tracking
            initialized = true;
            
            logger.info("Path fix integration initialized");
        } catch (Exception e) {
            logger.error("Error initializing path fix integration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Fix paths for all servers
     */
    public void fixAllServerPaths() {
        logger.info("Fixing paths for all servers");
        
        try {
            // Get all servers
            List<GameServer> servers = serverRepository.findAll();
            
            if (servers == null || servers.isEmpty()) {
                logger.warn("No servers found");
                return;
            }
            
            int total = servers.size();
            int fixed = 0;
            int failed = 0;
            
            // Process each server
            for (GameServer server : servers) {
                try {
                    Map<String, Object> results = fixServerPaths(server, connector, serverRepository);
                    boolean success = !(results.containsKey("error"));
                    
                    if (success) {
                        fixed++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    logger.error("Error fixing paths for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                    failed++;
                }
            }
            
            logger.info("Fixed paths for {}/{} servers, {} failed", fixed, total, failed);
        } catch (Exception e) {
            logger.error("Error fixing paths for all servers: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Fix paths for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @param serverRepository The server repository
     * @return Map of results
     */
    public Map<String, Object> fixServerPaths(GameServer server, SftpConnector connector, GameServerRepository serverRepository) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for server {}", server.getName());
            
            // Check if connector is available
            if (connector == null) {
                results.put("error", "SFTP connector is not available");
                return results;
            }
            
            // Check if server repository is available
            if (serverRepository == null) {
                results.put("error", "Server repository is not available");
                return results;
            }
            
            // Check if server is valid
            if (server == null) {
                results.put("error", "Server is not valid");
                return results;
            }
            
            // Test connection
            boolean connectionOk = connector.testConnection(server);
            
            if (!connectionOk) {
                results.put("error", "Connection test failed");
                return results;
            }
            
            // Try to fix CSV path
            boolean csvPathUpdated = false;
            boolean csvFilesFound = false;
            int csvFileCount = 0;
            
            try {
                // Try current path first
                List<String> csvFiles = connector.findDeathlogFiles(server);
                
                if (csvFiles != null && !csvFiles.isEmpty()) {
                    csvFilesFound = true;
                    csvFileCount = csvFiles.size();
                } else {
                    // Try alternative paths if current path failed
                    csvPathUpdated = tryAlternativeCsvPaths(server);
                    
                    if (csvPathUpdated) {
                        // Try again with updated path
                        csvFiles = connector.findDeathlogFiles(server);
                        
                        if (csvFiles != null && !csvFiles.isEmpty()) {
                            csvFilesFound = true;
                            csvFileCount = csvFiles.size();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error fixing CSV path for server {}: {}", 
                    server.getName(), e.getMessage(), e);
            }
            
            // Try to fix log path
            boolean logPathUpdated = false;
            boolean logFileFound = false;
            
            try {
                // Try current path first
                String logFile = connector.findLogFile(server);
                
                if (logFile != null && !logFile.isEmpty()) {
                    logFileFound = true;
                } else {
                    // Try alternative paths if current path failed
                    logPathUpdated = tryAlternativeLogPaths(server);
                    
                    if (logPathUpdated) {
                        // Try again with updated path
                        logFile = connector.findLogFile(server);
                        
                        if (logFile != null && !logFile.isEmpty()) {
                            logFileFound = true;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error fixing log path for server {}: {}", 
                    server.getName(), e.getMessage(), e);
            }
            
            // Save server if paths were updated
            boolean serverSaved = false;
            
            if (csvPathUpdated || logPathUpdated) {
                try {
                    serverRepository.save(server);
                    serverSaved = true;
                } catch (Exception e) {
                    logger.error("Error saving server {}: {}", 
                        server.getName(), e.getMessage(), e);
                }
            }
            
            // Set results
            results.put("csvFilesFound", csvFilesFound);
            results.put("csvFileCount", csvFileCount);
            results.put("logFileFound", logFileFound);
            results.put("csvPathUpdated", csvPathUpdated);
            results.put("logPathUpdated", logPathUpdated);
            results.put("serverSaved", serverSaved);
            
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Try alternative CSV paths for a server
     * @param server The game server
     * @return True if a working path was found and the server was updated
     */
    private boolean tryAlternativeCsvPaths(GameServer server) {
        try {
            logger.info("Trying alternative CSV paths for server {}", server.getName());
            
            // Get alternative paths
            List<String> alternativePaths = getAlternativeCsvPaths(server);
            
            for (String path : alternativePaths) {
                logger.debug("Trying alternative CSV path for server {}: {}", 
                    server.getName(), path);
                
                // Save current path
                String currentPath = server.getDeathlogsDirectory();
                
                // Set the path to try
                server.setDeathlogsDirectory(path);
                
                // Try to find CSV files
                List<String> csvFiles = null;
                
                try {
                    csvFiles = connector.findDeathlogFiles(server);
                } catch (Exception e) {
                    // Ignore errors for alternative paths
                    logger.debug("Error trying CSV path {} for server {}: {}", 
                        path, server.getName(), e.getMessage());
                }
                
                if (csvFiles != null && !csvFiles.isEmpty()) {
                    logger.info("Found working CSV path for server {}: {}", 
                        server.getName(), path);
                    
                    // Path is working, keep it
                    DeadsideParserPathRegistry.getInstance().registerPath(
                        server, DeadsideParserPathRegistry.PATH_TYPE_CSV, path);
                    
                    return true;
                } else {
                    // Path is not working, restore current path
                    server.setDeathlogsDirectory(currentPath);
                }
            }
            
            logger.warn("Could not find working CSV path for server {}", 
                server.getName());
            return false;
        } catch (Exception e) {
            logger.error("Error trying alternative CSV paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Try alternative log paths for a server
     * @param server The game server
     * @return True if a working path was found and the server was updated
     */
    private boolean tryAlternativeLogPaths(GameServer server) {
        try {
            logger.info("Trying alternative log paths for server {}", server.getName());
            
            // Get alternative paths
            List<String> alternativePaths = getAlternativeLogPaths(server);
            
            for (String path : alternativePaths) {
                logger.debug("Trying alternative log path for server {}: {}", 
                    server.getName(), path);
                
                // Save current path
                String currentPath = server.getLogDirectory();
                
                // Set the path to try
                server.setLogDirectory(path);
                
                // Try to find log file
                String logFile = null;
                
                try {
                    logFile = connector.findLogFile(server);
                } catch (Exception e) {
                    // Ignore errors for alternative paths
                    logger.debug("Error trying log path {} for server {}: {}", 
                        path, server.getName(), e.getMessage());
                }
                
                if (logFile != null && !logFile.isEmpty()) {
                    logger.info("Found working log path for server {}: {}", 
                        server.getName(), path);
                    
                    // Path is working, keep it
                    DeadsideParserPathRegistry.getInstance().registerPath(
                        server, DeadsideParserPathRegistry.PATH_TYPE_LOGS, path);
                    
                    return true;
                } else {
                    // Path is not working, restore current path
                    server.setLogDirectory(currentPath);
                }
            }
            
            logger.warn("Could not find working log path for server {}", 
                server.getName());
            return false;
        } catch (Exception e) {
            logger.error("Error trying alternative log paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
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