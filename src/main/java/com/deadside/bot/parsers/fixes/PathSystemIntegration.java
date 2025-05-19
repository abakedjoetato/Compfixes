package com.deadside.bot.parsers.fixes;

import com.deadside.bot.Bot;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration point for the parser path resolution system with the main Bot
 * This class provides methods to integrate the parser path resolution system
 * into the main Bot application
 */
public class PathSystemIntegration {
    private static final Logger logger = LoggerFactory.getLogger(PathSystemIntegration.class);
    
    /**
     * Initialize the path resolution system with the Bot
     * @param bot The Bot instance
     * @return True if initialization was successful
     */
    public static boolean initialize(Bot bot) {
        try {
            logger.info("Initializing path resolution system");
            
            // Check if the Bot has the required dependencies
            if (bot == null) {
                logger.error("Cannot initialize path resolution system: Bot is null");
                return false;
            }
            
            // Get dependencies from the Bot
            GameServerRepository gameServerRepository = bot.getGameServerRepository();
            SftpConnector sftpConnector = bot.getSftpConnector();
            DeadsideCsvParser csvParser = bot.getCsvParser();
            DeadsideLogParser logParser = bot.getLogParser();
            
            if (gameServerRepository == null) {
                logger.error("Cannot initialize path resolution system: GameServerRepository is null");
                return false;
            }
            
            if (sftpConnector == null) {
                logger.error("Cannot initialize path resolution system: SftpConnector is null");
                return false;
            }
            
            if (csvParser == null) {
                logger.error("Cannot initialize path resolution system: DeadsideCsvParser is null");
                return false;
            }
            
            if (logParser == null) {
                logger.error("Cannot initialize path resolution system: DeadsideLogParser is null");
                return false;
            }
            
            // Initialize the registry
            DeadsideParserPathRegistry.getInstance().initialize(
                gameServerRepository, sftpConnector, csvParser, logParser);
            boolean success = DeadsideParserPathRegistry.getInstance().isInitialized();
            
            // Create integration components
            BotParserIntegration integration = new BotParserIntegration(
                bot.getJda(), csvParser, logParser, sftpConnector);
            integration.initialize();
            
            // Path monitoring is implemented elsewhere
            logger.info("Path monitoring service has been disabled temporarily");
            
            if (success) {
                logger.info("Path resolution system initialized successfully");
                
                // Run initial path validation and repair
                runInitialPathValidation(gameServerRepository, sftpConnector);
                
                return true;
            } else {
                logger.error("Failed to initialize path resolution system");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error initializing path resolution system: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Run initial path validation and repair
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     */
    private static void runInitialPathValidation(GameServerRepository gameServerRepository, 
                                              SftpConnector sftpConnector) {
        try {
            logger.info("Running initial path validation and repair");
            
            // Get all guild IDs
            List<Long> guildIds = gameServerRepository.getDistinctGuildIds();
            int totalFixed = 0;
            
            for (Long guildId : guildIds) {
                try {
                    // Set context for guild
                    com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
                    
                    // Check and repair paths for all servers in this guild
                    totalFixed += PathResolutionManager.getInstance().fixPathsForGuild(guildId);
                } finally {
                    // Always clear context
                    com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
                }
            }
            
            logger.info("Initial path validation complete. Fixed {} server paths", totalFixed);
        } catch (Exception e) {
            logger.error("Error during initial path validation: {}", e.getMessage(), e);
        }
    }
}