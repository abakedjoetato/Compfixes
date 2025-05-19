package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for the Deadside parser path resolution system
 * This class provides a singleton entry point for accessing and initializing
 * the parser path resolution components
 */
public class DeadsideParserPathRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserPathRegistry.class);
    
    // Singleton instance
    private static DeadsideParserPathRegistry instance;
    
    // Core components
    private ParserPathTracker pathTracker;
    private PathResolutionManager resolutionManager;
    private ParserPathIntegrationManager integrationManager;
    private ParserPathFinder pathFinder;
    
    // Enhanced parsers
    private EnhancedCsvParser enhancedCsvParser;
    private EnhancedLogParser enhancedLogParser;
    
    // Dependencies
    private SftpConnector sftpConnector;
    private GameServerRepository gameServerRepository;
    private DeadsideCsvParser originalCsvParser;
    private DeadsideLogParser originalLogParser;
    
    // Initialization status
    private boolean initialized = false;
    
    /**
     * Private constructor for singleton pattern
     */
    private DeadsideParserPathRegistry() {
        logger.info("DeadsideParserPathRegistry created");
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
     * Initialize the registry with all required dependencies
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     * @param csvParser Original CSV parser
     * @param logParser Original log parser
     * @return True if initialization was successful
     */
    public boolean initialize(GameServerRepository gameServerRepository, 
                           SftpConnector sftpConnector,
                           DeadsideCsvParser csvParser,
                           DeadsideLogParser logParser) {
        if (initialized) {
            logger.info("DeadsideParserPathRegistry already initialized");
            return true;
        }
        
        try {
            logger.info("Initializing DeadsideParserPathRegistry");
            
            // Save dependencies
            this.gameServerRepository = gameServerRepository;
            this.sftpConnector = sftpConnector;
            this.originalCsvParser = csvParser;
            this.originalLogParser = logParser;
            
            // Initialize core components
            initializeCoreComponents();
            
            // Initialize enhanced parsers
            initializeEnhancedParsers();
            
            // Mark as initialized
            initialized = true;
            
            logger.info("DeadsideParserPathRegistry initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error initializing DeadsideParserPathRegistry: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Initialize core components
     */
    private void initializeCoreComponents() {
        // Initialize ParserPathTracker
        pathTracker = ParserPathTracker.getInstance();
        
        // Initialize PathResolutionManager
        resolutionManager = PathResolutionManager.getInstance();
        resolutionManager.initialize(gameServerRepository, sftpConnector);
        
        // Initialize ParserPathIntegrationManager
        integrationManager = ParserPathIntegrationManager.getInstance();
        integrationManager.initialize(resolutionManager);
        
        // Initialize ParserPathFinder
        pathFinder = ParserPathFinder.getInstance();
        pathFinder.initialize(sftpConnector);
        
        logger.info("Core path resolution components initialized");
    }
    
    /**
     * Initialize enhanced parsers
     */
    private void initializeEnhancedParsers() {
        enhancedCsvParser = new EnhancedCsvParser(originalCsvParser, sftpConnector);
        enhancedLogParser = new EnhancedLogParser(originalLogParser, sftpConnector);
        
        logger.info("Enhanced parsers initialized");
    }
    
    /**
     * Check if the registry is initialized
     * @return True if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get the enhanced CSV parser
     * @return The enhanced CSV parser
     */
    public EnhancedCsvParser getEnhancedCsvParser() {
        return enhancedCsvParser;
    }
    
    /**
     * Get the enhanced log parser
     * @return The enhanced log parser
     */
    public EnhancedLogParser getEnhancedLogParser() {
        return enhancedLogParser;
    }
    
    /**
     * Get the path resolution manager
     * @return The path resolution manager
     */
    public PathResolutionManager getPathResolutionManager() {
        return resolutionManager;
    }
    
    /**
     * Get the parser path tracker
     * @return The parser path tracker
     */
    public ParserPathTracker getPathTracker() {
        return pathTracker;
    }
    
    /**
     * Get the parser path integration manager
     * @return The parser path integration manager
     */
    public ParserPathIntegrationManager getIntegrationManager() {
        return integrationManager;
    }
    
    /**
     * Get the parser path finder
     * @return The parser path finder
     */
    public ParserPathFinder getPathFinder() {
        return pathFinder;
    }
}