package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration module for parser fixes
 * This class provides integration between the parser fix components
 */
public class ParserIntegrationModule {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationModule.class);
    
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
    public ParserIntegrationModule(
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
     * Initialize the integration module
     */
    public void initialize() {
        logger.info("Initializing parser integration module");
        
        try {
            // Initialize path resolution
            PathFixIntegration pathFixIntegration = new PathFixIntegration(connector, serverRepository);
            pathFixIntegration.initialize();
            
            logger.info("Parser integration module initialized");
        } catch (Exception e) {
            logger.error("Error initializing parser integration module: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Run an immediate path fix for all servers
     */
    public void runImmediatePathFix() {
        logger.info("Running immediate path fix for all servers");
        
        try {
            // Initialize path resolution
            PathFixIntegration pathFixIntegration = new PathFixIntegration(connector, serverRepository);
            pathFixIntegration.initialize();
            
            // Run fix
            pathFixIntegration.fixAllServerPaths();
            
            logger.info("Immediate path fix completed");
        } catch (Exception e) {
            logger.error("Error running immediate path fix: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get a parser adapter with path resolution capabilities
     * @return The parser adapter
     */
    public DeadsideParserAdapter getParserAdapter() {
        return new DeadsideParserAdapter(connector, csvParser, logParser);
    }
    
    /**
     * Get a path finder for parsers
     * @return The path finder
     */
    public ParserPathFinder getPathFinder() {
        return new ParserPathFinder(connector);
    }
}