package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration manager for parser path resolution system
 * This class provides integration points for the parser path resolution system
 * with the existing parsers
 */
public class ParserPathIntegrationManager {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathIntegrationManager.class);
    
    // The singleton instance
    private static ParserPathIntegrationManager instance;
    
    // The path resolution manager
    private PathResolutionManager pathResolutionManager;
    
    // Whether we are initialized
    private boolean initialized = false;
    
    /**
     * Private constructor for singleton pattern
     */
    private ParserPathIntegrationManager() {
        logger.info("ParserPathIntegrationManager initialized");
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized ParserPathIntegrationManager getInstance() {
        if (instance == null) {
            instance = new ParserPathIntegrationManager();
        }
        return instance;
    }
    
    /**
     * Initialize the integration manager
     * @param pathResolutionManager The path resolution manager
     */
    public void initialize(PathResolutionManager pathResolutionManager) {
        this.pathResolutionManager = pathResolutionManager;
        this.initialized = true;
        
        logger.info("ParserPathIntegrationManager initialized with dependencies");
    }
    
    /**
     * Check if the integration manager is initialized
     * @return True if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Resolve the CSV path for a server
     * This method attempts to find a valid CSV path for the server
     * and updates the server if a fix is found
     * @param server The game server
     * @return The resolved path, or null if no valid path found
     */
    public String resolveCsvPath(GameServer server) {
        if (!initialized) {
            logger.warn("ParserPathIntegrationManager not initialized");
            return server.getDeathlogsDirectory();
        }
        
        if (server == null) {
            logger.warn("Cannot resolve CSV path for null server");
            return null;
        }
        
        try {
            // Get the current path
            String currentPath = server.getDeathlogsDirectory();
            
            // Check if the path is valid using the path resolution manager
            if (pathResolutionManager.fixPathsForServer(server)) {
                // If fixed, the server object will have been updated
                String newPath = server.getDeathlogsDirectory();
                
                if (!newPath.equals(currentPath)) {
                    logger.info("Resolved new CSV path for server {}: {} -> {}", 
                        server.getName(), currentPath, newPath);
                    return newPath;
                }
            }
            
            // Return the current path (which might have been updated)
            return server.getDeathlogsDirectory();
        } catch (Exception e) {
            logger.error("Error resolving CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getDeathlogsDirectory();
        }
    }
    
    /**
     * Resolve the Log path for a server
     * This method attempts to find a valid Log path for the server
     * and updates the server if a fix is found
     * @param server The game server
     * @return The resolved path, or null if no valid path found
     */
    public String resolveLogPath(GameServer server) {
        if (!initialized) {
            logger.warn("ParserPathIntegrationManager not initialized");
            return server.getLogDirectory();
        }
        
        if (server == null) {
            logger.warn("Cannot resolve Log path for null server");
            return null;
        }
        
        try {
            // Get the current path
            String currentPath = server.getLogDirectory();
            
            // Check if the path is valid using the path resolution manager
            if (pathResolutionManager.fixPathsForServer(server)) {
                // If fixed, the server object will have been updated
                String newPath = server.getLogDirectory();
                
                if (!newPath.equals(currentPath)) {
                    logger.info("Resolved new Log path for server {}: {} -> {}", 
                        server.getName(), currentPath, newPath);
                    return newPath;
                }
            }
            
            // Return the current path (which might have been updated)
            return server.getLogDirectory();
        } catch (Exception e) {
            logger.error("Error resolving Log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getLogDirectory();
        }
    }
    
    /**
     * Hook for CSV parser to get the path
     * This method is called by the CSV parser to get the path
     * @param server The game server
     * @param originalPath The original path
     * @return The resolved path
     */
    public String onCsvParserGetPath(GameServer server, String originalPath) {
        if (!initialized) {
            return originalPath;
        }
        
        try {
            // Check if the original path is valid
            if (originalPath != null && !originalPath.isEmpty()) {
                // Try to test the original path
                if (pathResolutionManager != null) {
                    String resolvedPath = resolveCsvPath(server);
                    if (resolvedPath != null && !resolvedPath.isEmpty()) {
                        return resolvedPath;
                    }
                }
            }
            
            // Get a recommendation if the original path is invalid
            String recommendedPath = ParserPathTracker.getInstance()
                .getSuccessfulPath(server, ParserPathTracker.CATEGORY_CSV);
            
            if (recommendedPath != null && !recommendedPath.isEmpty()) {
                return recommendedPath;
            }
        } catch (Exception e) {
            logger.error("Error in CSV parser hook: {}", e.getMessage(), e);
        }
        
        return originalPath;
    }
    
    /**
     * Hook for Log parser to get the path
     * This method is called by the Log parser to get the path
     * @param server The game server
     * @param originalPath The original path
     * @return The resolved path
     */
    public String onLogParserGetPath(GameServer server, String originalPath) {
        if (!initialized) {
            return originalPath;
        }
        
        try {
            // Check if the original path is valid
            if (originalPath != null && !originalPath.isEmpty()) {
                // Try to test the original path
                if (pathResolutionManager != null) {
                    String resolvedPath = resolveLogPath(server);
                    if (resolvedPath != null && !resolvedPath.isEmpty()) {
                        return resolvedPath;
                    }
                }
            }
            
            // Get a recommendation if the original path is invalid
            String recommendedPath = ParserPathTracker.getInstance()
                .getSuccessfulPath(server, ParserPathTracker.CATEGORY_LOG);
            
            if (recommendedPath != null && !recommendedPath.isEmpty()) {
                return recommendedPath;
            }
        } catch (Exception e) {
            logger.error("Error in Log parser hook: {}", e.getMessage(), e);
        }
        
        return originalPath;
    }
}