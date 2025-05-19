package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Path isolation fix for Deadside parser issues
 * This class provides functionality to isolate and fix path-related issues
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
     * Fix paths for a server
     * @param server The game server
     * @return Map of results
     */
    public Map<String, Object> fixPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for server: {}", server.getName());
            
            // Find valid CSV path
            ParserPathFinder pathFinder = new ParserPathFinder(connector);
            String csvPath = pathFinder.findValidCsvPath(server);
            
            if (csvPath != null) {
                logger.info("Found valid CSV path: {}", csvPath);
                server.setDeathlogsDirectory(csvPath);
                results.put("csvPath", csvPath);
                results.put("csvPathFixed", true);
            } else {
                logger.warn("Could not find valid CSV path");
                results.put("csvPathFixed", false);
            }
            
            // Find valid log path
            String logPath = pathFinder.findValidLogPath(server);
            
            if (logPath != null) {
                logger.info("Found valid log path: {}", logPath);
                server.setLogDirectory(logPath);
                results.put("logPath", logPath);
                results.put("logPathFixed", true);
            } else {
                logger.warn("Could not find valid log path");
                results.put("logPathFixed", false);
            }
            
            // Save server if paths were fixed
            boolean csvPathFixed = results.containsKey("csvPathFixed") && (boolean)results.get("csvPathFixed");
            boolean logPathFixed = results.containsKey("logPathFixed") && (boolean)results.get("logPathFixed");
            
            if (csvPathFixed || logPathFixed) {
                repository.save(server);
                results.put("saved", true);
                logger.info("Server saved with fixed paths");
            } else {
                results.put("saved", false);
                logger.warn("No paths fixed, server not saved");
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths: {}", e.getMessage(), e);
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Fix paths for all servers
     * @return Map of server IDs to results
     */
    public Map<String, Map<String, Object>> fixAllPaths() {
        Map<String, Map<String, Object>> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for all servers");
            
            // Get all servers
            List<GameServer> servers = repository.findAll();
            
            if (servers == null || servers.isEmpty()) {
                logger.warn("No servers found");
                return results;
            }
            
            // Fix paths for each server
            for (GameServer server : servers) {
                try {
                    Map<String, Object> serverResults = fixPaths(server);
                    results.put(server.getId().toString(), serverResults);
                } catch (Exception e) {
                    logger.error("Error fixing paths for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                    
                    Map<String, Object> errorResults = new HashMap<>();
                    errorResults.put("error", e.getMessage());
                    results.put(server.getId().toString(), errorResults);
                }
            }
            
            logger.info("Fixed paths for {} servers", servers.size());
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths for all servers: {}", e.getMessage(), e);
            return results;
        }
    }
}