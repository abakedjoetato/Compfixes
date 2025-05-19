package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Central initializer for parser fixes
 * This class ensures that all parser fixes, including path resolution,
 * are properly initialized
 */
public class ParserFixInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ParserFixInitializer.class);
    
    /**
     * Initialize all parser fixes
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     * @return True if initialization was successful
     */
    public static boolean initialize(GameServerRepository gameServerRepository, SftpConnector sftpConnector) {
        try {
            logger.info("Initializing parser fixes");
            
            // Initialize the path resolution system
            boolean pathSystemSuccess = initializePathResolutionSystem(gameServerRepository, sftpConnector);
            
            if (pathSystemSuccess) {
                logger.info("Parser fixes initialized successfully");
                return true;
            } else {
                logger.error("Failed to initialize parser fixes");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error initializing parser fixes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Initialize the path resolution system
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     * @return True if initialization was successful
     */
    private static boolean initializePathResolutionSystem(GameServerRepository gameServerRepository, 
                                                      SftpConnector sftpConnector) {
        try {
            logger.info("Initializing path resolution system");
            
            // Initialize the path resolution manager
            PathResolutionManager.getInstance().initialize(gameServerRepository, sftpConnector);
            
            // Check server paths and suggest fixes
            List<Long> guildIds = gameServerRepository.getDistinctGuildIds();
            int totalFixed = 0;
            
            for (Long guildId : guildIds) {
                // Set context for guild
                com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
                
                try {
                    List<GameServer> servers = gameServerRepository.findAllByGuildId(guildId);
                    
                    for (GameServer server : servers) {
                        // Skip restricted servers
                        if (server.hasRestrictedIsolation()) {
                            continue;
                        }
                        
                        if (PathResolutionManager.getInstance().fixPathsForServer(server)) {
                            totalFixed++;
                        }
                    }
                } finally {
                    // Always clear context
                    com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
                }
            }
            
            logger.info("Path resolution system initialized successfully. Fixed {} server paths", totalFixed);
            return true;
        } catch (Exception e) {
            logger.error("Error initializing path resolution system: {}", e.getMessage(), e);
            return false;
        }
    }
}