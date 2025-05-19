package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Direct path resolution fix for Deadside parser issues
 * This class provides direct path resolution for servers
 */
public class DirectPathResolutionFix {
    private static final Logger logger = LoggerFactory.getLogger(DirectPathResolutionFix.class);
    
    private final SftpConnector connector;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     */
    public DirectPathResolutionFix(SftpConnector connector) {
        this.connector = connector;
    }
    
    /**
     * Fix paths for a server
     * @param server The game server
     * @return Map of results
     */
    public Map<String, Object> fixServerPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for server: {}", server.getName());
            
            // Test connection
            boolean connectionOk = connector.testConnection(server);
            
            if (!connectionOk) {
                logger.error("Connection test failed for server: {}", server.getName());
                results.put("error", "Connection test failed");
                return results;
            }
            
            // Try to find valid CSV path
            boolean csvPathFixed = false;
            String csvPath = SftpPathUtils.findCsvPath(server, connector);
            
            if (csvPath != null) {
                logger.info("Found valid CSV path for server {}: {}", 
                    server.getName(), csvPath);
                
                // Update server
                String originalCsvPath = server.getDeathlogsDirectory();
                server.setDeathlogsDirectory(csvPath);
                
                // Register path
                ParserIntegrationHooks.recordSuccessfulCsvPath(server, csvPath);
                
                csvPathFixed = true;
                results.put("csvPath", csvPath);
                results.put("originalCsvPath", originalCsvPath);
            } else {
                logger.warn("Could not find valid CSV path for server: {}", 
                    server.getName());
            }
            
            // Try to find valid log path
            boolean logPathFixed = false;
            String logPath = SftpPathUtils.findLogPath(server, connector);
            
            if (logPath != null) {
                logger.info("Found valid log path for server {}: {}", 
                    server.getName(), logPath);
                
                // Update server
                String originalLogPath = server.getLogDirectory();
                server.setLogDirectory(logPath);
                
                // Register path
                ParserIntegrationHooks.recordSuccessfulLogPath(server, logPath);
                
                logPathFixed = true;
                results.put("logPath", logPath);
                results.put("originalLogPath", originalLogPath);
            } else {
                logger.warn("Could not find valid log path for server: {}", 
                    server.getName());
            }
            
            results.put("csvPathFixed", csvPathFixed);
            results.put("logPathFixed", logPathFixed);
            results.put("pathsFixed", csvPathFixed || logPathFixed);
            
            return results;
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            
            results.put("error", e.getMessage());
            return results;
        }
    }
    
    /**
     * Apply server path updates
     * @param server The game server
     * @param results The results map
     * @return Updated server
     */
    public GameServer applyServerUpdates(GameServer server, Map<String, Object> results) {
        try {
            // Check if updates needed
            boolean pathsFixed = results.containsKey("pathsFixed") && 
                (boolean)results.get("pathsFixed");
            
            if (!pathsFixed) {
                logger.warn("No paths fixed for server: {}", server.getName());
                return server;
            }
            
            // Apply CSV path update
            boolean csvPathFixed = results.containsKey("csvPathFixed") && 
                (boolean)results.get("csvPathFixed");
            
            if (csvPathFixed && results.containsKey("csvPath")) {
                String csvPath = (String)results.get("csvPath");
                server.setDeathlogsDirectory(csvPath);
                
                logger.info("Updated CSV path for server {}: {}", 
                    server.getName(), csvPath);
            }
            
            // Apply log path update
            boolean logPathFixed = results.containsKey("logPathFixed") && 
                (boolean)results.get("logPathFixed");
            
            if (logPathFixed && results.containsKey("logPath")) {
                String logPath = (String)results.get("logPath");
                server.setLogDirectory(logPath);
                
                logger.info("Updated log path for server {}: {}", 
                    server.getName(), logPath);
            }
            
            return server;
        } catch (Exception e) {
            logger.error("Error applying server updates for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server;
        }
    }
}