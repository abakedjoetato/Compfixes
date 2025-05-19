package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.PathResolutionFix;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration handler for path fixes
 * This class manages applying path fixes to all servers or specific servers
 */
public class PathFixIntegration {
    private static final Logger logger = LoggerFactory.getLogger(PathFixIntegration.class);
    
    private final GameServerRepository gameServerRepository;
    private final SftpConnector sftpConnector;
    
    /**
     * Create a new PathFixIntegration instance
     * @param gameServerRepository The server repository
     * @param sftpConnector The SFTP connector
     */
    public PathFixIntegration(GameServerRepository gameServerRepository, SftpConnector sftpConnector) {
        this.gameServerRepository = gameServerRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Fix paths for all servers in the database
     * @return Number of servers fixed
     */
    public int fixAllServerPaths() {
        int fixedCount = 0;
        
        try {
            logger.info("Starting path fix for all servers");
            
            // Fetch all distinct guild IDs
            List<Long> guildIds = gameServerRepository.getDistinctGuildIds();
            List<GameServer> allServers = new ArrayList<>();
            
            // Get servers with proper isolation context
            for (Long guildId : guildIds) {
                // Set isolation context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
                try {
                    // Get servers for this guild
                    List<GameServer> guildServers = gameServerRepository.findAllByGuildId(guildId);
                    allServers.addAll(guildServers);
                } finally {
                    // Clear isolation context
                    com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
                }
            }
            
            // Fix paths for each server
            for (GameServer server : allServers) {
                try {
                    // Set isolation context for this server
                    com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(server.getGuildId(), null);
                    try {
                        // Apply path fixes
                        boolean fixed = fixServerPaths(server);
                        if (fixed) {
                            fixedCount++;
                        }
                    } finally {
                        // Clear isolation context
                        com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
                    }
                } catch (Exception e) {
                    logger.error("Error fixing paths for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                }
            }
            
            logger.info("Completed path fix for all servers: fixed {}/{} servers", 
                fixedCount, allServers.size());
            
        } catch (Exception e) {
            logger.error("Error in fixAllServerPaths: {}", e.getMessage(), e);
        }
        
        return fixedCount;
    }
    
    /**
     * Fix paths for a specific server
     * @param server The server to fix
     * @return True if any paths were fixed
     */
    public boolean fixServerPaths(GameServer server) {
        if (server == null) {
            return false;
        }
        
        try {
            // Skip servers with restricted isolation
            if (server.hasRestrictedIsolation()) {
                logger.info("Skipping path fix for server {} (restricted isolation)", server.getName());
                return false;
            }
            
            // Apply path resolution fix
            return PathResolutionFix.fixServerPaths(server, sftpConnector, gameServerRepository);
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fix paths for servers in a specific guild
     * @param guildId The guild ID
     * @return Number of servers fixed
     */
    public int fixGuildServerPaths(long guildId) {
        int fixedCount = 0;
        
        try {
            logger.info("Starting path fix for guild {}", guildId);
            
            // Set isolation context
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            try {
                // Get servers for this guild
                List<GameServer> servers = gameServerRepository.findAllByGuildId(guildId);
                
                // Fix paths for each server
                for (GameServer server : servers) {
                    try {
                        boolean fixed = fixServerPaths(server);
                        if (fixed) {
                            fixedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("Error fixing paths for server {}: {}", 
                            server.getName(), e.getMessage(), e);
                    }
                }
                
                logger.info("Completed path fix for guild {}: fixed {}/{} servers", 
                    guildId, fixedCount, servers.size());
            } finally {
                // Clear isolation context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error in fixGuildServerPaths for guild {}: {}", 
                guildId, e.getMessage(), e);
        }
        
        return fixedCount;
    }
}