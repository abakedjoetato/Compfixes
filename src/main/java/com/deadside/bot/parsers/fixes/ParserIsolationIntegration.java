package com.deadside.bot.parsers.fixes;

import com.deadside.bot.Bot;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration class for parser isolation fixes
 * This class provides an integration point for the parser isolation fixes
 * with the main Bot class
 */
public class ParserIsolationIntegration {
    private static final Logger logger = LoggerFactory.getLogger(ParserIsolationIntegration.class);
    
    // Dependencies
    private final Bot bot;
    private final GameServerRepository gameServerRepository;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    private final SftpConnector sftpConnector;
    
    // Wrapper for parsers
    private ParserPathWrapper parserWrapper;
    
    /**
     * Constructor
     * @param bot The Bot instance
     */
    public ParserIsolationIntegration(Bot bot) {
        this.bot = bot;
        this.gameServerRepository = bot.getGameServerRepository();
        this.csvParser = bot.getCsvParser();
        this.logParser = bot.getLogParser();
        this.sftpConnector = bot.getSftpConnector();
        
        // Initialize the path wrapper
        this.parserWrapper = new ParserPathWrapper(
            csvParser, logParser, sftpConnector, gameServerRepository);
        
        logger.info("ParserIsolationIntegration initialized with Bot dependencies");
    }
    
    /**
     * Fix paths for all servers in all guilds
     * @return Total number of servers fixed
     */
    public int fixAllServerPaths() {
        int totalFixed = 0;
        
        try {
            // Get all guild IDs
            java.util.List<Long> guildIds = gameServerRepository.getDistinctGuildIds();
            
            for (Long guildId : guildIds) {
                totalFixed += PathIsolationFix.fixGuildServerPaths(
                    guildId, gameServerRepository, sftpConnector);
            }
            
            logger.info("Fixed paths for {} servers across all guilds", totalFixed);
        } catch (Exception e) {
            logger.error("Error fixing paths for all servers: {}", e.getMessage(), e);
        }
        
        return totalFixed;
    }
    
    /**
     * Get the parser wrapper
     * @return The parser wrapper
     */
    public ParserPathWrapper getParserWrapper() {
        return parserWrapper;
    }
}