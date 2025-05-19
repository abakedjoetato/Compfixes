package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced SFTP connector with improved path resolution capabilities
 */
public class EnhancedSftpConnector extends SftpConnector {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedSftpConnector.class);
    
    /**
     * Constructor
     */
    public EnhancedSftpConnector() {
        super();
    }
    
    /**
     * Find log file with advanced path resolution
     * @param server The game server
     * @return The log file path
     */
    public String findLogFileEnhanced(GameServer server) {
        try {
            logger.debug("Finding log file with enhanced resolution for server: {}", server.getName());
            
            // First try standard method
            String logFile = findLogFile(server);
            
            if (logFile != null && !logFile.isEmpty()) {
                return logFile;
            }
            
            // Enhanced resolution
            List<String> alternativePaths = generateAlternativeLogPaths(server);
            
            for (String path : alternativePaths) {
                String originalPath = server.getLogDirectory();
                server.setLogDirectory(path);
                
                logFile = findLogFile(server);
                
                // Restore original path
                server.setLogDirectory(originalPath);
                
                if (logFile != null && !logFile.isEmpty()) {
                    logger.info("Found log file with alternative path: {}", path);
                    return logFile;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error finding log file with enhanced resolution: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Find CSV files with advanced path resolution
     * @param server The game server
     * @return List of CSV files
     */
    public List<String> findCsvFilesEnhanced(GameServer server) {
        try {
            logger.debug("Finding CSV files with enhanced resolution for server: {}", server.getName());
            
            // First try standard method
            List<String> csvFiles = findDeathlogFiles(server);
            
            if (csvFiles != null && !csvFiles.isEmpty()) {
                return csvFiles;
            }
            
            // Enhanced resolution
            List<String> alternativePaths = generateAlternativeCsvPaths(server);
            
            for (String path : alternativePaths) {
                String originalPath = server.getDeathlogsDirectory();
                server.setDeathlogsDirectory(path);
                
                csvFiles = findDeathlogFiles(server);
                
                // Restore original path
                server.setDeathlogsDirectory(originalPath);
                
                if (csvFiles != null && !csvFiles.isEmpty()) {
                    logger.info("Found CSV files with alternative path: {}", path);
                    return csvFiles;
                }
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error finding CSV files with enhanced resolution: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Generate alternative log paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private List<String> generateAlternativeLogPaths(GameServer server) {
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
    
    /**
     * Generate alternative CSV paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private List<String> generateAlternativeCsvPaths(GameServer server) {
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
}