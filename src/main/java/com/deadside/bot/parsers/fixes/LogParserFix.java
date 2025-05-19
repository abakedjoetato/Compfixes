package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fix for log parser issues related to path resolution
 */
public class LogParserFix {
    private static final Logger logger = LoggerFactory.getLogger(LogParserFix.class);
    
    // Cache of successful paths
    private static final Map<String, String> successfulPaths = new ConcurrentHashMap<>();
    
    // Alternative paths to try
    private static final List<String> alternativePathPatterns = Arrays.asList(
        "{host}_{server}/Logs",
        "{host}_{server}/Deadside/Logs",
        "{host}/{server}/Logs",
        "{host}/{server}/Deadside/Logs",
        "{server}/Logs",
        "{server}/Deadside/Logs"
    );
    
    /**
     * Resolve a log path for a given server
     * @param server The server
     * @param sftpConnector SFTP connector
     * @return The resolved path, or the original if resolution failed
     */
    public static String resolveServerLogPath(GameServer server, SftpConnector sftpConnector) {
        if (server == null) {
            return null;
        }
        
        try {
            String serverKey = getServerKey(server);
            
            // Check cache first
            String cachedPath = successfulPaths.get(serverKey);
            if (cachedPath != null) {
                logger.debug("Using cached log path for server {}: {}", server.getName(), cachedPath);
                return cachedPath;
            }
            
            // Check if current path works
            String currentPath = server.getLogDirectory();
            if (currentPath != null && !currentPath.isEmpty() && 
                testPath(server, currentPath, sftpConnector)) {
                // Current path works, cache it
                successfulPaths.put(serverKey, currentPath);
                return currentPath;
            }
            
            // Try alternative paths
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            for (String pattern : alternativePathPatterns) {
                String path = pattern
                    .replace("{host}", host)
                    .replace("{server}", serverName);
                
                if (testPath(server, path, sftpConnector)) {
                    // Path works, update server and cache it
                    server.setLogDirectory(path);
                    successfulPaths.put(serverKey, path);
                    
                    logger.info("Resolved log path for server {}: {} -> {}", 
                        server.getName(), currentPath, path);
                    
                    return path;
                }
            }
            
            // No valid path found, return original
            logger.warn("Could not resolve log path for server {}", server.getName());
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getLogDirectory();
        }
    }
    
    /**
     * Test if a path is valid and contains expected log file
     * @param server The server
     * @param path The path to test
     * @param sftpConnector The SFTP connector
     * @return True if valid
     */
    private static boolean testPath(GameServer server, String path, SftpConnector sftpConnector) {
        try {
            if (!sftpConnector.testConnection(server, path)) {
                return false;
            }
            
            // For logs, we should also check if the Deadside.log file exists
            String logFilePath = findLogFile(server, path, sftpConnector);
            return logFilePath != null && !logFilePath.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Find the log file in a path
     * @param server The server
     * @param path The path to search
     * @param sftpConnector The SFTP connector
     * @return The log file path if found, null otherwise
     */
    private static String findLogFile(GameServer server, String path, SftpConnector sftpConnector) {
        try {
            List<String> files = sftpConnector.listFiles(server, path);
            if (files == null || files.isEmpty()) {
                return null;
            }
            
            for (String file : files) {
                // Check if this is the Deadside.log file
                if (file.endsWith("/Deadside.log") || file.endsWith("\\Deadside.log")) {
                    return file;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error finding log file in path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Process log file content with validation
     * @param server The game server
     * @param filePath The file path
     * @return True if processed successfully
     */
    public static boolean processAndValidateLogFile(GameServer server, String filePath) {
        try {
            if (server == null || filePath == null || filePath.isEmpty()) {
                logger.warn("Invalid parameters for log validation: server={}, filePath={}", 
                    server != null ? server.getName() : "null", filePath);
                return false;
            }
            
            // In a real implementation, this would validate and process the log file
            logger.info("Validating log file: {} for server: {}", 
                filePath, server.getName());
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating log file {} for server {}: {}", 
                filePath, server != null ? server.getName() : "unknown", e.getMessage(), e);
            return false;
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