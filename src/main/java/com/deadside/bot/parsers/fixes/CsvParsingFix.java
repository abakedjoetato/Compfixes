package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fix for CSV parsing issues related to path resolution
 */
public class CsvParsingFix {
    private static final Logger logger = LoggerFactory.getLogger(CsvParsingFix.class);
    
    // Cache of successful paths
    private static final Map<String, String> successfulPaths = new ConcurrentHashMap<>();
    
    // Alternative paths to try
    private static final List<String> alternativePathPatterns = Arrays.asList(
        "{host}_{server}/actual1/deathlogs",
        "{host}_{server}/actual/deathlogs",
        "{host}/{server}/actual1/deathlogs",
        "{host}/{server}/actual/deathlogs",
        "{server}/actual1/deathlogs",
        "{server}/actual/deathlogs"
    );
    
    /**
     * Resolve a CSV path for a given server
     * @param server The server
     * @param sftpConnector SFTP connector
     * @return The resolved path, or the original if resolution failed
     */
    public static String resolveServerCsvPath(GameServer server, SftpConnector sftpConnector) {
        if (server == null) {
            return null;
        }
        
        try {
            String serverKey = getServerKey(server);
            
            // Check cache first
            String cachedPath = successfulPaths.get(serverKey);
            if (cachedPath != null) {
                logger.debug("Using cached CSV path for server {}: {}", server.getName(), cachedPath);
                return cachedPath;
            }
            
            // Check if current path works
            String currentPath = server.getDeathlogsDirectory();
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
                    server.setDeathlogsDirectory(path);
                    successfulPaths.put(serverKey, path);
                    
                    logger.info("Resolved CSV path for server {}: {} -> {}", 
                        server.getName(), currentPath, path);
                    
                    return path;
                }
            }
            
            // No valid path found, return original
            logger.warn("Could not resolve CSV path for server {}", server.getName());
            return currentPath;
        } catch (Exception e) {
            logger.error("Error resolving CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getDeathlogsDirectory();
        }
    }
    
    /**
     * Update paths for all servers in a guild
     * @param guildId The guild ID
     * @param repository The game server repository
     * @param sftpConnector The SFTP connector
     * @return Number of servers updated
     */
    public static int updateGuildServerPaths(long guildId, 
                                          GameServerRepository repository, 
                                          SftpConnector sftpConnector) {
        int updated = 0;
        
        try {
            // Set context for guild
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                List<GameServer> servers = repository.findAllByGuildId(guildId);
                
                for (GameServer server : servers) {
                    if (updateServerPaths(server, repository, sftpConnector)) {
                        updated++;
                    }
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error updating paths for guild {}: {}", guildId, e.getMessage(), e);
        }
        
        return updated;
    }
    
    /**
     * Update paths for a server
     * @param server The server
     * @param repository The game server repository
     * @param sftpConnector The SFTP connector
     * @return True if updated
     */
    public static boolean updateServerPaths(GameServer server, 
                                         GameServerRepository repository, 
                                         SftpConnector sftpConnector) {
        if (server == null) {
            return false;
        }
        
        try {
            boolean updated = false;
            
            // Resolve CSV path
            String originalCsvPath = server.getDeathlogsDirectory();
            String resolvedCsvPath = resolveServerCsvPath(server, sftpConnector);
            
            if (resolvedCsvPath != null && !resolvedCsvPath.equals(originalCsvPath)) {
                server.setDeathlogsDirectory(resolvedCsvPath);
                updated = true;
            }
            
            // Resolve Log path
            String originalLogPath = server.getLogDirectory();
            String resolvedLogPath = LogParserFix.resolveServerLogPath(server, sftpConnector);
            
            if (resolvedLogPath != null && !resolvedLogPath.equals(originalLogPath)) {
                server.setLogDirectory(resolvedLogPath);
                updated = true;
            }
            
            // Save if updated
            if (updated) {
                repository.save(server);
                logger.info("Updated paths for server {}. CSV: {} -> {}, Log: {} -> {}", 
                    server.getName(), originalCsvPath, resolvedCsvPath,
                    originalLogPath, resolvedLogPath);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error updating paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Test if a path is valid
     * @param server The server
     * @param path The path to test
     * @param sftpConnector The SFTP connector
     * @return True if valid
     */
    private static boolean testPath(GameServer server, String path, SftpConnector sftpConnector) {
        try {
            return sftpConnector.testConnection(server, path);
        } catch (Exception e) {
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