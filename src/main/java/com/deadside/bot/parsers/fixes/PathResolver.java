package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Path resolver for CSV and Log files
 * This class provides methods for resolving path issues
 */
public class PathResolver {
    private static final Logger logger = LoggerFactory.getLogger(PathResolver.class);
    
    // Cache of successful paths
    private static final Map<String, String> pathCache = new ConcurrentHashMap<>();
    
    // Standard patterns for CSV files
    private static final List<String> CSV_PATTERNS = Arrays.asList(
        "{host}_{server}/actual1/deathlogs",
        "{host}_{server}/actual/deathlogs",
        "{host}/{server}/actual1/deathlogs",
        "{host}/{server}/actual/deathlogs",
        "{server}/actual1/deathlogs",
        "{server}/actual/deathlogs"
    );
    
    // Standard patterns for Log files
    private static final List<String> LOG_PATTERNS = Arrays.asList(
        "{host}_{server}/Logs",
        "{host}_{server}/Deadside/Logs",
        "{host}/{server}/Logs",
        "{host}/{server}/Deadside/Logs",
        "{server}/Logs",
        "{server}/Deadside/Logs"
    );
    
    /**
     * Resolve CSV path for a server
     * @param server The server
     * @param connector SFTP connector
     * @return The resolved path or original if not resolvable
     */
    public static String resolveCsvPath(GameServer server, SftpConnector connector) {
        if (server == null || connector == null) {
            return null;
        }
        
        try {
            // Check if current path is valid
            String currentPath = server.getDeathlogsDirectory();
            if (isValidCsvPath(currentPath) && testPath(server, currentPath, connector)) {
                return currentPath;
            }
            
            // Try patterns
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            for (String pattern : CSV_PATTERNS) {
                String path = pattern
                    .replace("{host}", host)
                    .replace("{server}", serverName);
                
                if (testPath(server, path, connector)) {
                    logger.info("Resolved CSV path for server {}: {} -> {}", 
                        server.getName(), currentPath, path);
                    return path;
                }
            }
            
            // No valid path found
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving CSV path: {}", e.getMessage());
            return server.getDeathlogsDirectory();
        }
    }
    
    /**
     * Resolve Log path for a server
     * @param server The server
     * @param connector SFTP connector
     * @return The resolved path or original if not resolvable
     */
    public static String resolveLogPath(GameServer server, SftpConnector connector) {
        if (server == null || connector == null) {
            return null;
        }
        
        try {
            // Check if current path is valid
            String currentPath = server.getLogDirectory();
            if (isValidLogPath(currentPath) && testPath(server, currentPath, connector)) {
                return currentPath;
            }
            
            // Try patterns
            String host = server.getSftpHost();
            if (host == null || host.isEmpty()) {
                host = server.getHost();
            }
            
            String serverName = server.getServerId();
            if (serverName == null || serverName.isEmpty()) {
                serverName = server.getName().replaceAll("\\s+", "_");
            }
            
            for (String pattern : LOG_PATTERNS) {
                String path = pattern
                    .replace("{host}", host)
                    .replace("{server}", serverName);
                
                if (testPath(server, path, connector)) {
                    logger.info("Resolved Log path for server {}: {} -> {}", 
                        server.getName(), currentPath, path);
                    return path;
                }
            }
            
            // No valid path found
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving Log path: {}", e.getMessage());
            return server.getLogDirectory();
        }
    }
    
    /**
     * Check if a CSV path is valid
     * @param path The path
     * @return True if valid
     */
    private static boolean isValidCsvPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/actual1/deathlogs") || 
               path.contains("\\actual1\\deathlogs") ||
               path.contains("/actual/deathlogs") || 
               path.contains("\\actual\\deathlogs");
    }
    
    /**
     * Check if a Log path is valid
     * @param path The path
     * @return True if valid
     */
    private static boolean isValidLogPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/Logs") || 
               path.contains("\\Logs");
    }
    
    /**
     * Test if a path exists
     * @param server The server
     * @param path The path
     * @param connector SFTP connector
     * @return True if exists
     */
    private static boolean testPath(GameServer server, String path, SftpConnector connector) {
        try {
            return connector.testConnection(server, path);
        } catch (Exception e) {
            return false;
        }
    }
}