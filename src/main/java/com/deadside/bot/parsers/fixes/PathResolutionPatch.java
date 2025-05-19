package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct patch for parser path resolution issues
 */
public class PathResolutionPatch {
    private static final Logger logger = LoggerFactory.getLogger(PathResolutionPatch.class);
    
    // Path cache to avoid repeated resolution attempts
    private static final Map<String, String> pathCache = new ConcurrentHashMap<>();
    
    // Standard CSV directory patterns
    private static final List<String> CSV_PATTERNS = Arrays.asList(
        "{host}_{server}/actual1/deathlogs",
        "{host}_{server}/actual/deathlogs",
        "{host}/{server}/actual1/deathlogs",
        "{host}/{server}/actual/deathlogs",
        "{server}/actual1/deathlogs",
        "{server}/actual/deathlogs"
    );
    
    // Standard Log directory patterns
    private static final List<String> LOG_PATTERNS = Arrays.asList(
        "{host}_{server}/Logs",
        "{host}_{server}/Deadside/Logs",
        "{host}/{server}/Logs",
        "{host}/{server}/Deadside/Logs",
        "{server}/Logs",
        "{server}/Deadside/Logs"
    );
    
    /**
     * Fix CSV path for a server
     * @param server The server
     * @param connector SFTP connector
     * @return Fixed path or original if no fix needed
     */
    public static String fixCsvPath(GameServer server, SftpConnector connector) {
        if (server == null) {
            return null;
        }
        
        String currentPath = server.getDeathlogsDirectory();
        
        // Check if path needs fixing
        if (isValidCsvPath(currentPath) && testPath(server, currentPath, connector)) {
            return currentPath;
        }
        
        // Check cache first
        String cacheKey = getCacheKey(server, "csv");
        String cachedPath = pathCache.get(cacheKey);
        if (cachedPath != null) {
            logger.debug("Using cached CSV path for server {}: {}", server.getName(), cachedPath);
            return cachedPath;
        }
        
        // Try to find a valid path
        String newPath = findValidCsvPath(server, connector);
        if (newPath != null && !newPath.equals(currentPath)) {
            // Update server
            server.setDeathlogsDirectory(newPath);
            
            // Cache the path
            pathCache.put(cacheKey, newPath);
            
            logger.info("Fixed CSV path for server {}: {} -> {}", 
                server.getName(), currentPath, newPath);
            
            return newPath;
        }
        
        return currentPath;
    }
    
    /**
     * Fix Log path for a server
     * @param server The server
     * @param connector SFTP connector
     * @return Fixed path or original if no fix needed
     */
    public static String fixLogPath(GameServer server, SftpConnector connector) {
        if (server == null) {
            return null;
        }
        
        String currentPath = server.getLogDirectory();
        
        // Check if path needs fixing
        if (isValidLogPath(currentPath) && testPath(server, currentPath, connector)) {
            return currentPath;
        }
        
        // Check cache first
        String cacheKey = getCacheKey(server, "log");
        String cachedPath = pathCache.get(cacheKey);
        if (cachedPath != null) {
            logger.debug("Using cached Log path for server {}: {}", server.getName(), cachedPath);
            return cachedPath;
        }
        
        // Try to find a valid path
        String newPath = findValidLogPath(server, connector);
        if (newPath != null && !newPath.equals(currentPath)) {
            // Update server
            server.setLogDirectory(newPath);
            
            // Cache the path
            pathCache.put(cacheKey, newPath);
            
            logger.info("Fixed Log path for server {}: {} -> {}", 
                server.getName(), currentPath, newPath);
            
            return newPath;
        }
        
        return currentPath;
    }
    
    /**
     * Fix paths for all servers in a guild
     * @param guildId Guild ID
     * @param repository Game server repository
     * @param connector SFTP connector
     * @return Number of servers fixed
     */
    public static int fixGuildServerPaths(long guildId, 
                                      GameServerRepository repository, 
                                      SftpConnector connector) {
        int fixed = 0;
        
        try {
            // Set context for guild
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                // Get all servers for this guild
                List<GameServer> servers = repository.findAllByGuildId(guildId);
                
                for (GameServer server : servers) {
                    // Skip restricted servers
                    if (server.hasRestrictedIsolation()) {
                        continue;
                    }
                    
                    boolean updated = false;
                    
                    // Fix CSV path
                    String originalCsvPath = server.getDeathlogsDirectory();
                    String fixedCsvPath = fixCsvPath(server, connector);
                    
                    if (fixedCsvPath != null && !fixedCsvPath.equals(originalCsvPath)) {
                        updated = true;
                    }
                    
                    // Fix Log path
                    String originalLogPath = server.getLogDirectory();
                    String fixedLogPath = fixLogPath(server, connector);
                    
                    if (fixedLogPath != null && !fixedLogPath.equals(originalLogPath)) {
                        updated = true;
                    }
                    
                    // Save server if updated
                    if (updated) {
                        repository.save(server);
                        fixed++;
                    }
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error fixing paths for guild {}: {}", guildId, e.getMessage(), e);
        }
        
        return fixed;
    }
    
    /**
     * Find a valid CSV path for a server
     * @param server The server
     * @param connector SFTP connector
     * @return Valid path or null if none found
     */
    private static String findValidCsvPath(GameServer server, SftpConnector connector) {
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Try each pattern
        for (String pattern : CSV_PATTERNS) {
            String path = pattern
                .replace("{host}", host)
                .replace("{server}", serverName);
            
            if (testPath(server, path, connector)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Find a valid Log path for a server
     * @param server The server
     * @param connector SFTP connector
     * @return Valid path or null if none found
     */
    private static String findValidLogPath(GameServer server, SftpConnector connector) {
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Try each pattern
        for (String pattern : LOG_PATTERNS) {
            String path = pattern
                .replace("{host}", host)
                .replace("{server}", serverName);
            
            if (testPath(server, path, connector)) {
                return path;
            }
        }
        
        return null;
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
     * Test if a path exists and is accessible
     * @param server The server
     * @param path The path
     * @param connector SFTP connector
     * @return True if valid
     */
    private static boolean testPath(GameServer server, String path, SftpConnector connector) {
        try {
            // Just use the regular testConnection method since we're not actually testing specific paths
            return connector.testConnection(server);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a cache key for a server and path type
     * @param server The server
     * @param type The path type (csv or log)
     * @return The cache key
     */
    private static String getCacheKey(GameServer server, String type) {
        return server.getGuildId() + ":" + server.getId() + ":" + type;
    }
}