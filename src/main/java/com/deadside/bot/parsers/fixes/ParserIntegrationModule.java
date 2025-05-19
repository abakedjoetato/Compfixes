package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central module for parser path resolution fix
 * This provides a single entry point for the DeadsideBot to initialize the path resolution system
 */
public class ParserIntegrationModule {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationModule.class);
    
    // Flag to indicate initialization
    private static boolean initialized = false;
    
    /**
     * Initialize the parser integration module
     * @param gameServerRepository Game server repository
     * @param sftpConnector SFTP connector
     * @return True if initialization was successful
     */
    public static synchronized boolean initialize(
            GameServerRepository gameServerRepository, 
            SftpConnector sftpConnector) {
        
        if (initialized) {
            logger.info("Parser integration module already initialized");
            return true;
        }
        
        try {
            logger.info("Initializing parser integration module");
            
            // Initialize ParserExtensions
            ParserExtensions.initialize(gameServerRepository, sftpConnector);
            
            // Create and initialize PathFixIntegration
            PathFixIntegration pathFixIntegration = new PathFixIntegration(gameServerRepository, sftpConnector);
            
            // Fix paths for all servers
            int fixed = pathFixIntegration.fixAllServerPaths();
            logger.info("Parser integration module initialized: fixed {} server paths", fixed);
            
            initialized = true;
            return true;
        } catch (Exception e) {
            logger.error("Error initializing parser integration module: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Run path fix for all servers
     * @param gameServerRepository Game server repository
     * @param sftpConnector SFTP connector
     * @return Number of servers fixed
     */
    public static int runPathFix(
            GameServerRepository gameServerRepository,
            SftpConnector sftpConnector) {
        
        try {
            if (!initialized) {
                // Initialize if not already
                initialize(gameServerRepository, sftpConnector);
            }
            
            // Create PathFixIntegration
            PathFixIntegration pathFixIntegration = new PathFixIntegration(gameServerRepository, sftpConnector);
            
            // Fix paths for all servers
            return pathFixIntegration.fixAllServerPaths();
        } catch (Exception e) {
            logger.error("Error running path fix: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Check if the module is initialized
     * @return True if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}