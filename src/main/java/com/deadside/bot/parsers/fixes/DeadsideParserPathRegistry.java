package com.deadside.bot.parsers.fixes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for Deadside parser paths
 * This class provides a registry for parser paths
 */
public class DeadsideParserPathRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserPathRegistry.class);
    
    // Path type constants
    public static final String PATH_TYPE_CSV = "csv";
    public static final String PATH_TYPE_LOGS = "logs";
    
    // Singleton instance
    private static DeadsideParserPathRegistry instance;
    
    // Path registry data
    private final Map<String, Map<String, String>> pathRegistry = new ConcurrentHashMap<>();
    
    // Initialization flag
    private boolean initialized = false;
    
    /**
     * Private constructor for singleton
     */
    private DeadsideParserPathRegistry() {
        // Initialize the registry
        initialize();
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
     * Initialize the registry
     */
    public void initialize() {
        if (initialized) {
            logger.debug("Registry already initialized");
            return;
        }
        
        logger.info("Initializing path registry");
        
        // Clear any existing data
        pathRegistry.clear();
        
        // Set initialization flag
        initialized = true;
        
        logger.info("Path registry initialized");
    }
    
    /**
     * Initialize with additional components
     * This overload exists for compatibility with calling code
     */
    public void initialize(
            com.deadside.bot.db.repositories.GameServerRepository serverRepository, 
            com.deadside.bot.sftp.SftpConnector connector,
            com.deadside.bot.parsers.DeadsideCsvParser csvParser,
            com.deadside.bot.parsers.DeadsideLogParser logParser) {
        // Call basic initialization
        initialize();
        
        // Additional initialization can be added here if needed
        logger.info("Path registry initialized with components");
    }
    
    /**
     * Check if the registry is initialized
     * @return True if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Register a path for a GameServer
     * @param server The game server
     * @param pathType The path type (csv, log)
     * @param path The path
     */
    public void registerPath(com.deadside.bot.db.models.GameServer server, String pathType, String path) {
        if (server == null) {
            logger.warn("Cannot register path for null server");
            return;
        }
        
        String serverId = server.getServerId();
        if (serverId == null || serverId.isEmpty()) {
            serverId = String.valueOf(server.getId());
        }
        
        registerPath(serverId, pathType, path);
    }

    /**
     * Register a path
     * @param serverId The server ID
     * @param pathType The path type (csv, log)
     * @param path The path
     */
    public void registerPath(String serverId, String pathType, String path) {
        if (serverId == null || serverId.isEmpty() || pathType == null || pathType.isEmpty() || path == null || path.isEmpty()) {
            logger.warn("Invalid parameters for registerPath: serverId={}, pathType={}, path={}", serverId, pathType, path);
            return;
        }
        
        // Get or create server registry
        Map<String, String> serverRegistry = pathRegistry.computeIfAbsent(serverId, k -> new HashMap<>());
        
        // Store path
        serverRegistry.put(pathType, path);
        
        logger.debug("Registered path for server {}: {}={}", serverId, pathType, path);
    }
    
    /**
     * Get a registered path for a GameServer
     * @param server The game server
     * @param pathType The path type (csv, log)
     * @return The registered path, or null if not found
     */
    public String getPath(com.deadside.bot.db.models.GameServer server, String pathType) {
        if (server == null) {
            logger.warn("Cannot get path for null server");
            return null;
        }
        
        String serverId = server.getServerId();
        if (serverId == null || serverId.isEmpty()) {
            serverId = String.valueOf(server.getId());
        }
        
        return getPath(serverId, pathType);
    }
    
    /**
     * Get a registered path
     * @param serverId The server ID
     * @param pathType The path type (csv, log)
     * @return The registered path, or null if not found
     */
    public String getPath(String serverId, String pathType) {
        if (serverId == null || serverId.isEmpty() || pathType == null || pathType.isEmpty()) {
            logger.warn("Invalid parameters for getPath: serverId={}, pathType={}", serverId, pathType);
            return null;
        }
        
        // Check if server is in registry
        if (!pathRegistry.containsKey(serverId)) {
            logger.debug("Server {} not found in registry", serverId);
            return null;
        }
        
        // Get server registry
        Map<String, String> serverRegistry = pathRegistry.get(serverId);
        
        // Return path
        return serverRegistry.get(pathType);
    }
    
    /**
     * Check if a path is registered
     * @param serverId The server ID
     * @param pathType The path type (csv, log)
     * @return True if the path is registered
     */
    public boolean hasPath(String serverId, String pathType) {
        if (serverId == null || serverId.isEmpty() || pathType == null || pathType.isEmpty()) {
            logger.warn("Invalid parameters for hasPath: serverId={}, pathType={}", serverId, pathType);
            return false;
        }
        
        // Check if server is in registry
        if (!pathRegistry.containsKey(serverId)) {
            logger.debug("Server {} not found in registry", serverId);
            return false;
        }
        
        // Get server registry
        Map<String, String> serverRegistry = pathRegistry.get(serverId);
        
        // Check if path is in registry
        return serverRegistry.containsKey(pathType);
    }
}