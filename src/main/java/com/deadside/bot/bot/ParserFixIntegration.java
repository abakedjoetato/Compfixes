package com.deadside.bot.bot;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.parsers.fixes.ParserExtensions;
import com.deadside.bot.parsers.fixes.PathFixIntegration;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration point for parser fixes
 */
public class ParserFixIntegration {
    private static final Logger logger = LoggerFactory.getLogger(ParserFixIntegration.class);
    
    // Core dependencies
    private final JDA jda;
    private final GameServerRepository gameServerRepository;
    private final SftpConnector sftpConnector;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    // Fix components
    private PathFixIntegration pathFixIntegration;
    
    /**
     * Constructor with dependencies
     * @param jda JDA instance
     * @param gameServerRepository Game server repository
     * @param playerRepository Player repository
     * @param sftpConnector SFTP connector
     * @param csvParser CSV parser
     * @param logParser Log parser
     */
    public ParserFixIntegration(JDA jda, 
                               GameServerRepository gameServerRepository,
                               org.bson.codecs.pojo.annotations.BsonProperty("playerRepository") Object playerRepository,
                               SftpConnector sftpConnector,
                               DeadsideCsvParser csvParser,
                               DeadsideLogParser logParser) {
        this.jda = jda;
        this.gameServerRepository = gameServerRepository;
        this.sftpConnector = sftpConnector;
        this.csvParser = csvParser;
        this.logParser = logParser;
        
        logger.info("ParserFixIntegration created with all dependencies");
    }
    
    /**
     * Initialize the integration
     * @return True if successful
     */
    public boolean initialize() {
        try {
            logger.info("Initializing parser fixes");
            
            // Initialize path fix components
            pathFixIntegration = new PathFixIntegration(gameServerRepository, sftpConnector);
            
            // Initialize parser extensions
            ParserExtensions.initialize(gameServerRepository, sftpConnector);
            
            // Run path fix for all servers
            int fixed = pathFixIntegration.fixAllServerPaths();
            logger.info("Fixed paths for {} servers", fixed);
            
            logger.info("Parser fixes initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error initializing parser fixes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the path fix integration
     * @return The path fix integration
     */
    public PathFixIntegration getPathFixIntegration() {
        return pathFixIntegration;
    }
}