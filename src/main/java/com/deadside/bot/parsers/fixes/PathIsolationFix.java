package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes for path isolation issues
 * This class provides utilities for fixing path isolation issues
 * with support for guild isolation
 */
public class PathIsolationFix {
    private static final Logger logger = LoggerFactory.getLogger(PathIsolationFix.class);
    
    /**
     * Fix paths for all servers in a guild
     * @param guildId The guild ID
     * @param repository The game server repository
     * @param connector The SFTP connector
     * @return Number of servers fixed
     */
    public static int fixGuildServerPaths(long guildId, 
                                      GameServerRepository repository, 
                                      SftpConnector connector) {
        int fixed = 0;
        
        try {
            // Set context for guild isolation
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                // Get all servers for this guild
                List<GameServer> servers = repository.findAllByGuildId(guildId);
                List<GameServer> fixedServers = new ArrayList<>();
                
                for (GameServer server : servers) {
                    // Skip restricted servers
                    if (server.hasRestrictedIsolation()) {
                        continue;
                    }
                    
                    // Try to fix paths
                    if (DirectPathResolutionFix.fixServerPaths(server, connector)) {
                        fixedServers.add(server);
                        fixed++;
                    }
                }
                
                // Save all fixed servers
                if (!fixedServers.isEmpty()) {
                    repository.saveAll(fixedServers);
                    logger.info("Fixed paths for {} servers in guild {}", fixed, guildId);
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error fixing paths for guild {}: {}", guildId, e.getMessage(), e);
        }
        
        return fixed;
    }
    
    /**
     * Check if a specific path needs fixing
     * @param path The path to check
     * @param type The path type ("csv" or "log")
     * @return True if the path needs fixing
     */
    public static boolean needsFixing(String path, String type) {
        if (path == null || path.isEmpty()) {
            return true;
        }
        
        if ("csv".equalsIgnoreCase(type)) {
            return !path.contains("/actual1/deathlogs") && 
                   !path.contains("\\actual1\\deathlogs") &&
                   !path.contains("/actual/deathlogs") && 
                   !path.contains("\\actual\\deathlogs");
        } else if ("log".equalsIgnoreCase(type)) {
            return !path.contains("/Logs") && !path.contains("\\Logs");
        }
        
        return false;
    }
    
    /**
     * Check if a server needs path fixing
     * @param server The server to check
     * @return True if the server needs path fixing
     */
    public static boolean serverNeedsFixing(GameServer server) {
        if (server == null) {
            return false;
        }
        
        return needsFixing(server.getDeathlogsDirectory(), "csv") || 
               needsFixing(server.getLogDirectory(), "log");
    }
}