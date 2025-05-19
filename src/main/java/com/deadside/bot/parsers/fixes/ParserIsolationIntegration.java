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
 * Integration for parser isolation fixes
 */
public class ParserIsolationIntegration {
    private static final Logger logger = LoggerFactory.getLogger(ParserIsolationIntegration.class);
    
    private final SftpConnector connector;
    private final GameServerRepository repository;
    private final PathIsolationFix pathFix;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param repository The game server repository
     */
    public ParserIsolationIntegration(SftpConnector connector, GameServerRepository repository) {
        this.connector = connector;
        this.repository = repository;
        this.pathFix = new PathIsolationFix(connector, repository);
    }
    
    /**
     * Fix paths for a guild
     * @param guildId The guild ID
     * @return Results map
     */
    public Map<String, Object> fixGuildPaths(long guildId) {
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
            
            // Store per-server results
            Map<String, Map<String, Object>> serverResults = new HashMap<>();
            
            // Fix paths for each server
            for (GameServer gameServer : servers) {
                try {
                    Map<String, Object> serverResult = pathFix.fixPaths(gameServer);
                    serverResults.put(gameServer.getId().toString(), serverResult);
                    
                    boolean csvPathFixed = serverResult.containsKey("csvPathFixed") 
                        && (boolean) serverResult.get("csvPathFixed");
                    boolean logPathFixed = serverResult.containsKey("logPathFixed") 
                        && (boolean) serverResult.get("logPathFixed");
                    
                    if (csvPathFixed || logPathFixed) {
                        fixed++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    logger.error("Error fixing paths for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                    
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", e.getMessage());
                    serverResults.put(server.getId().toString(), errorResult);
                    
                    failed++;
                }
            }
            
            // Set overall results
            results.put("total", total);
            results.put("fixed", fixed);
            results.put("failed", failed);
            results.put("serverResults", serverResults);
            
            logger.info("Fixed paths for guild {}: {}/{} servers", 
                guildId, fixed, total);
            
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths for guild {}: {}", 
                guildId, e.getMessage(), e);
            
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Fix paths for a specific server in a guild
     * @param guildId The guild ID
     * @param serverName The server name
     * @return Results map
     */
    public Map<String, Object> fixGuildServerPaths(long guildId, String serverName) {
        try {
            logger.info("Fixing paths for server {} in guild {}", serverName, guildId);
            
            // Find server
            GameServer server = repository.findByGuildIdAndName(guildId, serverName);
            
            if (server == null) {
                logger.warn("Server {} not found in guild {}", serverName, guildId);
                
                Map<String, Object> results = new HashMap<>();
                results.put("error", "Server not found");
                return results;
            }
            
            // Fix paths
            return pathFix.fixPaths(server);
        } catch (Exception e) {
            logger.error("Error fixing paths for server {} in guild {}: {}", 
                serverName, guildId, e.getMessage(), e);
            
            Map<String, Object> results = new HashMap<>();
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Get overall path health for a guild
     * @param guildId The guild ID
     * @return Health status map
     */
    public Map<String, Object> getGuildPathHealth(long guildId) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Checking path health for guild: {}", guildId);
            
            // Find servers for guild
            List<GameServer> servers = new ArrayList<>();
            GameServer server = repository.findByGuildId(guildId);
            if (server != null) {
                servers.add(server);
            }
            
            if (servers == null || servers.isEmpty()) {
                logger.warn("No servers found for guild: {}", guildId);
                results.put("error", "No servers found");
                return results;
            }
            
            // Track statistics
            int total = servers.size();
            int healthy = 0;
            int unhealthy = 0;
            
            // Store per-server results
            Map<String, Object> serverResults = new HashMap<>();
            
            // Check health for each server
            for (GameServer gameServer : servers) {
                try {
                    ParserPathRepairHook repairHook = new ParserPathRepairHook(connector, repository);
                    
                    boolean csvPathOk = repairHook.isValidCsvPath(gameServer);
                    boolean logPathOk = repairHook.isValidLogPath(gameServer);
                    
                    Map<String, Object> serverResult = new HashMap<>();
                    serverResult.put("csvPathOk", csvPathOk);
                    serverResult.put("logPathOk", logPathOk);
                    serverResult.put("healthy", csvPathOk && logPathOk);
                    
                    serverResults.put(server.getId().toString(), serverResult);
                    
                    if (csvPathOk && logPathOk) {
                        healthy++;
                    } else {
                        unhealthy++;
                    }
                } catch (Exception e) {
                    logger.error("Error checking health for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                    
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", e.getMessage());
                    errorResult.put("healthy", false);
                    serverResults.put(server.getId().toString(), errorResult);
                    
                    unhealthy++;
                }
            }
            
            // Set overall results
            results.put("total", total);
            results.put("healthy", healthy);
            results.put("unhealthy", unhealthy);
            results.put("healthPercentage", (double) healthy / total * 100);
            results.put("serverResults", serverResults);
            
            logger.info("Path health for guild {}: {}/{} servers healthy", 
                guildId, healthy, total);
            
            return results;
        } catch (Exception e) {
            logger.error("Error checking path health for guild {}: {}", 
                guildId, e.getMessage(), e);
            
            results.put("error", e.getMessage());
            return results;
        }
    }
}