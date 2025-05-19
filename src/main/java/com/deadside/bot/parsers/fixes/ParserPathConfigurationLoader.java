package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration loader for the parser path resolution system
 * This class is responsible for loading and configuring the parser path resolution system
 * during application startup
 */
public class ParserPathConfigurationLoader {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathConfigurationLoader.class);
    
    // Dependencies
    private final GameServerRepository gameServerRepository;
    private final SftpConnector sftpConnector;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    /**
     * Constructor with dependencies
     */
    public ParserPathConfigurationLoader(GameServerRepository gameServerRepository,
                                     SftpConnector sftpConnector,
                                     DeadsideCsvParser csvParser,
                                     DeadsideLogParser logParser) {
        this.gameServerRepository = gameServerRepository;
        this.sftpConnector = sftpConnector;
        this.csvParser = csvParser;
        this.logParser = logParser;
    }
    
    /**
     * Load and configure the parser path resolution system
     * @return True if configuration was successful
     */
    public boolean loadConfiguration() {
        try {
            logger.info("Loading parser path resolution configuration");
            
            // Initialize the parser path registry
            boolean success = DeadsideParserPathRegistry.getInstance().initialize(
                gameServerRepository, sftpConnector, csvParser, logParser);
            
            if (success) {
                logger.info("Parser path resolution configuration loaded successfully");
                return true;
            } else {
                logger.error("Failed to load parser path resolution configuration");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error loading parser path resolution configuration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Run an immediate path resolution pass
     * @return Number of paths fixed
     */
    public int runImmediatePathResolution() {
        if (!DeadsideParserPathRegistry.getInstance().isInitialized()) {
            logger.warn("Cannot run path resolution, system not initialized");
            return -1;
        }
        
        return DeadsideParserPathRegistry.getInstance().runImmediatePathResolution();
    }
}