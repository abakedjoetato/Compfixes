package com.deadside.bot.parsers.fixes;

import com.deadside.bot.Bot;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration with the main Bot class for the parser path resolution system
 * This class provides methods to integrate the parsers with the path resolution system
 */
public class BotParserIntegration {
    private static final Logger logger = LoggerFactory.getLogger(BotParserIntegration.class);
    
    // Enhanced parsers
    private EnhancedCsvParser enhancedCsvParser;
    private EnhancedLogParser enhancedLogParser;
    
    // Original parsers
    private final DeadsideCsvParser originalCsvParser;
    private final DeadsideLogParser originalLogParser;
    
    // SFTP connector
    private final SftpConnector sftpConnector;
    
    // The Bot instance
    private final Bot bot;
    
    /**
     * Constructor
     * @param bot The Bot instance
     * @param csvParser The original CSV parser
     * @param logParser The original log parser
     * @param sftpConnector The SFTP connector
     */
    public BotParserIntegration(Bot bot, DeadsideCsvParser csvParser, 
                              DeadsideLogParser logParser, SftpConnector sftpConnector) {
        this.bot = bot;
        this.originalCsvParser = csvParser;
        this.originalLogParser = logParser;
        this.sftpConnector = sftpConnector;
        
        // Initialize enhanced parsers
        this.enhancedCsvParser = new EnhancedCsvParser(csvParser, sftpConnector);
        this.enhancedLogParser = new EnhancedLogParser(logParser, sftpConnector);
        
        logger.info("BotParserIntegration initialized");
    }
    
    /**
     * Initialize the integration with the bot
     */
    public void initialize() {
        try {
            logger.info("Initializing BotParserIntegration");
            
            // Initialize the path resolution system
            initializePathResolutionSystem();
            
            logger.info("BotParserIntegration initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing BotParserIntegration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Initialize the path resolution system
     */
    private void initializePathResolutionSystem() {
        try {
            // Initialize ParserPathFinder
            ParserPathFinder.getInstance().initialize(sftpConnector);
            
            // Initialize DeadsideParserValidator
            DeadsideParserValidator validator = new DeadsideParserValidator(sftpConnector);
            
            // Initialize PathResolutionManager
            PathResolutionManager.getInstance().initialize(
                bot.getGameServerRepository(), sftpConnector);
            
            // Initialize ParserPathIntegrationManager
            ParserPathIntegrationManager.getInstance().initialize(
                PathResolutionManager.getInstance());
            
            logger.info("Path resolution system initialized");
        } catch (Exception e) {
            logger.error("Error initializing path resolution system: {}", e.getMessage(), e);
        }
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
}