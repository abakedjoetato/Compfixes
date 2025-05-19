package com.deadside.bot.parsers.fixes;

import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration between the bot and the parser systems
 * This class provides integration points for connecting parsers with the bot
 */
public class BotParserIntegration {
    private static final Logger logger = LoggerFactory.getLogger(BotParserIntegration.class);
    
    private final JDA jda;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param jda The JDA instance
     * @param csvParser The CSV parser
     * @param logParser The log parser
     * @param sftpConnector The SFTP connector
     */
    public BotParserIntegration(JDA jda, DeadsideCsvParser csvParser, 
                              DeadsideLogParser logParser, SftpConnector sftpConnector) {
        this.jda = jda;
        this.csvParser = csvParser;
        this.logParser = logParser;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Initialize the integration
     */
    public void initialize() {
        logger.info("Initializing bot parser integration");
        
        // Initialize parser integration points
        try {
            // Register events and hooks
            ParserIntegrationHooks.registerHooks(
                jda, csvParser, logParser, sftpConnector);
            
            logger.info("Bot parser integration initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing bot parser integration: {}", e.getMessage(), e);
        }
    }
}