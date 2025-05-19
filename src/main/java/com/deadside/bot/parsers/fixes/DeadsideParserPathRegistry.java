package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for Deadside parser paths
 * This class maintains a registry of successful parser paths for servers
 */
public class DeadsideParserPathRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserPathRegistry.class);
    
    // Singleton instance
    private static DeadsideParserPathRegistry instance;
    
    // Path types
    public static final String PATH_TYPE_CSV = "csv";
    public static final String PATH_TYPE_LOGS = "logs";
    
    // Path registry
    private final Map<Long, Map<String, String>> serverPaths;
    
    /**
     * Constructor (private for singleton)
     */
    private DeadsideParserPathRegistry() {
        serverPaths = new ConcurrentHashMap<>();
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized DeadsideParserPathRegistry getInstance() {
        if (instance == null) {
            instance = new DeadsideParserPathRegistry();
        }
        
        return instance;
    }
    
    /**
     * Initialize the registry with required components
     * @param repository The game server repository
     * @param connector The SFTP connector
     * @param csvParser The CSV parser
     * @param logParser The log parser
     * @return True if initialized successfully
     */
    public boolean initialize(
            GameServerRepository repository,
            SftpConnector connector,
            DeadsideCsvParser csvParser,
            DeadsideLogParser logParser) {
        try {
            logger.info("Initializing DeadsideParserPathRegistry");
            // No actual initialization needed in this implementation
            return true;
        } catch (Exception e) {
            logger.error("Error initializing DeadsideParserPathRegistry: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Register a successful path for a server
     * @param server The game server
     * @param pathType The path type (csv or logs)
     * @param path The path
     */
    public void registerPath(GameServer server, String pathType, String path) {
        if (server == null || pathType == null || path == null) {
            return;
        }
        
        try {
            String serverIdStr = server.getId().toString();
            Long serverId = Long.parseLong(serverIdStr);
            
            if (serverId == null) {
                logger.warn("Cannot register path for server with null ID: {}", server.getName());
                return;
            }
            
            // Get or create server paths
            Map<String, String> paths = serverPaths.computeIfAbsent(serverId, k -> new HashMap<>());
            
            // Register path
            paths.put(pathType, path);
            
            logger.info("Registered {} path for server {}: {}", 
                pathType, server.getName(), path);
        } catch (Exception e) {
            logger.error("Error registering path for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Get a registered path for a server
     * @param server The game server
     * @param pathType The path type (csv or logs)
     * @return The path, or null if not registered
     */
    public String getPath(GameServer server, String pathType) {
        if (server == null || pathType == null) {
            return null;
        }
        
        try {
            String serverIdStr = server.getId().toString();
            Long serverId = Long.parseLong(serverIdStr);
            
            if (serverId == null) {
                return null;
            }
            
            // Get server paths
            Map<String, String> paths = serverPaths.get(serverId);
            
            if (paths == null) {
                return null;
            }
            
            // Get path
            return paths.get(pathType);
        } catch (Exception e) {
            logger.error("Error getting path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Clear paths for a server
     * @param server The game server
     */
    public void clearPaths(GameServer server) {
        if (server == null) {
            return;
        }
        
        try {
            String serverIdStr = server.getId().toString();
            Long serverId = Long.parseLong(serverIdStr);
            
            if (serverId == null) {
                return;
            }
            
            // Remove server paths
            serverPaths.remove(serverId);
            
            logger.info("Cleared paths for server {}", server.getName());
        } catch (Exception e) {
            logger.error("Error clearing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
    }
}