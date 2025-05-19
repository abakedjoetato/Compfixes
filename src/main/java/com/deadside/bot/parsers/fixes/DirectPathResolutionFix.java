package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Direct path resolution fix for servers
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
     * Initialize the fix
     */
    public void initialize() {
        logger.info("Initializing DirectPathResolutionFix");
        // No initialization required for now
    }
    
    /**
     * Fix paths for a server
     * @param server The game server
     * @return The resolution results
     */
    public Map<String, Object> fixServerPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Fixing paths for server: {}", server.getName());
            
            // Store original paths
            String originalCsvPath = server.getDeathlogsDirectory();
            String originalLogPath = server.getLogDirectory();
            
            results.put("originalCsvPath", originalCsvPath);
            results.put("originalLogPath", originalLogPath);
            
            // Test connection
            if (!connector.testConnection(server)) {
                logger.error("Connection test failed for server: {}", server.getName());
                results.put("error", "Connection test failed");
                results.put("pathsFixed", false);
                return results;
            }
            
            // Try to fix CSV path
            boolean csvPathFixed = fixCsvPath(server, results);
            results.put("csvPathFixed", csvPathFixed);
            
            // Try to fix log path
            boolean logPathFixed = fixLogPath(server, results);
            results.put("logPathFixed", logPathFixed);
            
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
     * Apply server updates from results
     * @param server The game server to update
     * @param results The fix results
     * @return The updated server
     */
    public GameServer applyServerUpdates(GameServer server, Map<String, Object> results) {
        try {
            // Apply CSV path
            if (results.containsKey("csvPathFixed") && (boolean)results.get("csvPathFixed")) {
                String csvPath = (String)results.get("csvPath");
                server.setDeathlogsDirectory(csvPath);
                logger.info("Updated CSV path for server {}: {}", server.getName(), csvPath);
            }
            
            // Apply log path
            if (results.containsKey("logPathFixed") && (boolean)results.get("logPathFixed")) {
                String logPath = (String)results.get("logPath");
                server.setLogDirectory(logPath);
                logger.info("Updated log path for server {}: {}", server.getName(), logPath);
            }
            
            return server;
        } catch (Exception e) {
            logger.error("Error applying server updates: {}", e.getMessage(), e);
            return server;
        }
    }
    
    /**
     * Fix CSV path for a server
     * @param server The game server
     * @param results The results map
     * @return True if the path was fixed
     */
    private boolean fixCsvPath(GameServer server, Map<String, Object> results) {
        try {
            // Current path
            String currentPath = server.getDeathlogsDirectory();
            logger.info("Current CSV path for server {}: {}", server.getName(), currentPath);
            
            // Try to resolve path
            String resolvedPath = SftpPathUtils.findCsvPath(server, connector);
            
            if (resolvedPath == null) {
                logger.warn("Could not resolve CSV path for server: {}", server.getName());
                return false;
            }
            
            // Check if path changed
            boolean pathChanged = !resolvedPath.equals(currentPath);
            
            // Store results
            results.put("csvPath", resolvedPath);
            results.put("csvPathChanged", pathChanged);
            
            logger.info("Resolved CSV path for server {}: {} (changed: {})", 
                server.getName(), resolvedPath, pathChanged);
            
            return true;
        } catch (Exception e) {
            logger.error("Error fixing CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fix log path for a server
     * @param server The game server
     * @param results The results map
     * @return True if the path was fixed
     */
    private boolean fixLogPath(GameServer server, Map<String, Object> results) {
        try {
            // Current path
            String currentPath = server.getLogDirectory();
            logger.info("Current log path for server {}: {}", server.getName(), currentPath);
            
            // Try to resolve path
            String resolvedPath = SftpPathUtils.findLogPath(server, connector);
            
            if (resolvedPath == null) {
                logger.warn("Could not resolve log path for server: {}", server.getName());
                return false;
            }
            
            // Check if path changed
            boolean pathChanged = !resolvedPath.equals(currentPath);
            
            // Store results
            results.put("logPath", resolvedPath);
            results.put("logPathChanged", pathChanged);
            
            logger.info("Resolved log path for server {}: {} (changed: {})", 
                server.getName(), resolvedPath, pathChanged);
            
            return true;
        } catch (Exception e) {
            logger.error("Error fixing log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
}