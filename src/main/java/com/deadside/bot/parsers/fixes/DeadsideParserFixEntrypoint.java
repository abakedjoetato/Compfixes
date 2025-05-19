package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint for Deadside parser fixes
 * This class provides the main entrypoint for the parser fix system
 */
public class DeadsideParserFixEntrypoint {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserFixEntrypoint.class);
    
    private final SftpConnector connector;
    private final GameServerRepository serverRepository;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param serverRepository The server repository
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public DeadsideParserFixEntrypoint(
            SftpConnector connector,
            GameServerRepository serverRepository,
            DeadsideCsvParser csvParser,
            DeadsideLogParser logParser) {
        
        this.connector = connector;
        this.serverRepository = serverRepository;
        this.csvParser = csvParser;
        this.logParser = logParser;
    }
    
    /**
     * Enhanced constructor with additional components
     * @param jda The JDA instance
     * @param serverRepository The server repository
     * @param playerRepository The player repository
     * @param connector The SFTP connector
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public DeadsideParserFixEntrypoint(
            net.dv8tion.jda.api.JDA jda,
            GameServerRepository serverRepository,
            com.deadside.bot.db.repositories.PlayerRepository playerRepository,
            SftpConnector connector,
            DeadsideCsvParser csvParser,
            DeadsideLogParser logParser) {
        
        this.connector = connector;
        this.serverRepository = serverRepository;
        this.csvParser = csvParser;
        this.logParser = logParser;
        // JDA and playerRepository are stored for future use
    }
    
    /**
     * Initialize the parser fix system
     * This should be called during application startup
     */
    public void initialize() {
        logger.info("Initializing Deadside parser fix system");
        
        try {
            // Initialize registry
            DeadsideParserPathRegistry registry = DeadsideParserPathRegistry.getInstance();
            
            // Initialize integration module
            ParserIntegrationModule integrationModule = new ParserIntegrationModule(
                connector, serverRepository, csvParser, logParser);
            integrationModule.initialize();
            
            // Just log that we're ready
            logger.info("Parser components are ready for use");
            
            logger.info("Deadside parser fix system initialized");
        } catch (Exception e) {
            logger.error("Error initializing Deadside parser fix system: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get the parser integration module
     * @return The parser integration module
     */
    public ParserIntegrationModule getIntegrationModule() {
        return new ParserIntegrationModule(connector, serverRepository, csvParser, logParser);
    }
    
    /**
     * Execute all fixes as a batch operation
     * @return True if successful
     */
    public boolean executeAllFixesAsBatch() {
        try {
            logger.info("Executing all parser fixes as a batch operation");
            
            // Initialize extensions
            ParserExtensions.initialize();
            
            // Get integration module
            ParserIntegrationModule integrationModule = getIntegrationModule();
            
            // Run path validations
            integrationModule.validateAllPaths();
            
            // Run CSV processing
            integrationModule.processCsvFiles();
            
            // Run log processing
            integrationModule.processLogs();
            
            logger.info("Batch execution of parser fixes completed successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error executing parser fixes as batch: {}", e.getMessage(), e);
            return false;
        }
    }
}