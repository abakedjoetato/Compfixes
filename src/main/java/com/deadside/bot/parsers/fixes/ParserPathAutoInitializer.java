package com.deadside.bot.parsers.fixes;

import com.deadside.bot.Bot;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.services.PathMonitoringService;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic initializer for the parser path resolution system
 * This class provides automatic initialization of the path resolution system
 * with the Bot's dependencies
 */
public class ParserPathAutoInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathAutoInitializer.class);
    
    // Whether the system has been initialized
    private static boolean initialized = false;
    
    /**
     * Initialize the path resolution system with the Bot's dependencies
     * @param bot The Bot instance
     * @return True if initialization was successful
     */
    public static synchronized boolean initializeWithBot(Bot bot) {
        if (initialized) {
            logger.info("Parser path resolution system already initialized");
            return true;
        }
        
        if (bot == null) {
            logger.error("Cannot initialize parser path resolution system with null Bot instance");
            return false;
        }
        
        try {
            logger.info("Initializing parser path resolution system");
            
            // Get dependencies from the Bot
            GameServerRepository gameServerRepository = bot.getGameServerRepository();
            SftpConnector sftpConnector = bot.getSftpConnector();
            
            if (gameServerRepository == null) {
                logger.error("Cannot initialize parser path resolution system: GameServerRepository is null");
                return false;
            }
            
            if (sftpConnector == null) {
                logger.error("Cannot initialize parser path resolution system: SftpConnector is null");
                return false;
            }
            
            // Initialize the parser path tracker
            ParserPathTracker.getInstance();
            
            // Initialize the path resolution manager
            PathResolutionManager pathResolutionManager = PathResolutionManager.getInstance();
            pathResolutionManager.initialize(gameServerRepository, sftpConnector);
            
            // Initialize the path monitoring service
            PathMonitoringService pathMonitoringService = 
                PathMonitoringService.getInstance(gameServerRepository, sftpConnector);
            pathMonitoringService.start();
            
            // Initialize the parser path integration manager
            ParserPathIntegrationManager.getInstance().initialize(pathResolutionManager);
            
            // Initialize the parser fix initializer
            ParserFixInitializer.initialize(gameServerRepository, sftpConnector);
            
            // Mark as initialized
            initialized = true;
            
            logger.info("Parser path resolution system initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error initializing parser path resolution system: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if the system has been initialized
     * @return True if the system has been initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}