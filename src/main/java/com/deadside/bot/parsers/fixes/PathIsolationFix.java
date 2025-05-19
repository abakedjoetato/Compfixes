package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Path isolation fix for servers
 */
public class PathIsolationFix {
    private static final Logger logger = LoggerFactory.getLogger(PathIsolationFix.class);
    
    private final SftpConnector connector;
    private final GameServerRepository repository;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param repository The game server repository
     */
    public PathIsolationFix(SftpConnector connector, GameServerRepository repository) {
        this.connector = connector;
        this.repository = repository;
    }
    
    /**
     * Fix paths for a specific server
     * @param server The game server
     * @return Fix results
     */
    public Map<String, Object> fixPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for server: {}", server.getName());
            
            // Fix CSV path
            Map<String, Object> csvResults = fixCsvPath(server);
            boolean csvPathFixed = csvResults.containsKey("resolved") && (boolean)csvResults.get("resolved");
            
            results.put("csvPathFixed", csvPathFixed);
            results.put("csvResults", csvResults);
            
            // Fix log path
            Map<String, Object> logResults = fixLogPath(server);
            boolean logPathFixed = logResults.containsKey("resolved") && (boolean)logResults.get("resolved");
            
            results.put("logPathFixed", logPathFixed);
            results.put("logResults", logResults);
            
            // Overall status
            results.put("pathsFixed", csvPathFixed || logPathFixed);
            
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            
            results.put("error", e.getMessage());
            results.put("csvPathFixed", false);
            results.put("logPathFixed", false);
            results.put("pathsFixed", false);
            
            return results;
        }
    }
    
    /**
     * Fix paths for guild servers
     * @param guildId The guild ID
     * @param repository The game server repository
     * @param connector The SFTP connector
     * @return Fix results
     */
    public Map<String, Object> fixGuildServerPaths(Long guildId, GameServerRepository repository, SftpConnector connector) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for guild: {}", guildId);
            
            // Find servers for guild
            List<GameServer> servers = new ArrayList<>();
            GameServer server = repository.findByGuildId(guildId);
            if (server != null) {
                servers.add(server);
            };
            
            if (servers == null || servers.isEmpty()) {
                logger.warn("No servers found for guild: {}", guildId);
                results.put("error", "No servers found");
                return results;
            }
            
            // Track statistics
            int total = servers.size();
            int fixed = 0;
            int failed = 0;
            
            // Fix each server
            List<Map<String, Object>> serverResults = new ArrayList<>();
            
            for (GameServer gameServer : servers) {
                try {
                    Map<String, Object> serverResult = fixPaths(gameServer);
                    serverResults.add(serverResult);
                    
                    boolean pathsFixed = serverResult.containsKey("pathsFixed") 
                        && (boolean)serverResult.get("pathsFixed");
                    
                    if (pathsFixed) {
                        fixed++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    logger.error("Error fixing paths for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                    failed++;
                }
            }
            
            // Set results
            results.put("total", total);
            results.put("fixed", fixed);
            results.put("failed", failed);
            results.put("serverResults", serverResults);
            
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths for guild {}: {}", guildId, e.getMessage(), e);
            
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Fix CSV path for a server
     * @param server The game server
     * @return Fix results
     */
    private Map<String, Object> fixCsvPath(GameServer server) {
        // Create repair hook
        ParserPathRepairHook repairHook = new ParserPathRepairHook(connector, repository);
        
        // Repair CSV paths
        return repairHook.repairCsvPaths(server);
    }
    
    /**
     * Fix log path for a server
     * @param server The game server
     * @return Fix results
     */
    private Map<String, Object> fixLogPath(GameServer server) {
        // Create repair hook
        ParserPathRepairHook repairHook = new ParserPathRepairHook(connector, repository);
        
        // Repair log paths
        return repairHook.repairLogPaths(server);
    }
}