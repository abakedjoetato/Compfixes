package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Hook for parser path repair integration
 */
public class ParserPathRepairHook {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathRepairHook.class);
    
    private final SftpConnector connector;
    private final GameServerRepository repository;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param repository The game server repository
     */
    public ParserPathRepairHook(SftpConnector connector, GameServerRepository repository) {
        this.connector = connector;
        this.repository = repository;
    }
    
    /**
     * Repair CSV paths for a server
     * @param server The game server
     * @return Repair results
     */
    public Map<String, Object> repairCsvPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Repairing CSV paths for server: {}", server.getName());
            
            // Validate connection
            if (!connector.testConnection(server)) {
                logger.error("Connection test failed for server: {}", server.getName());
                results.put("error", "Connection test failed");
                return results;
            }
            
            // Current path
            String currentPath = server.getDeathlogsDirectory();
            results.put("originalPath", currentPath);
            
            // Try to resolve path
            String resolvedPath = CsvParsingFix.resolveServerCsvPath(server, connector);
            
            if (resolvedPath == null) {
                logger.warn("Could not resolve CSV path for server: {}", server.getName());
                results.put("resolved", false);
                return results;
            }
            
            // Update server
            boolean updated = CsvParsingFix.updateServerCsvPath(server, resolvedPath);
            
            if (updated) {
                logger.info("Updated CSV path for server {}: {} -> {}", 
                    server.getName(), currentPath, resolvedPath);
                
                results.put("resolved", true);
                results.put("path", resolvedPath);
                
                // Save server
                repository.save(server);
                results.put("saved", true);
            } else {
                logger.warn("Failed to update CSV path for server: {}", server.getName());
                results.put("resolved", false);
                results.put("saved", false);
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Error repairing CSV paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            
            results.put("error", e.getMessage());
            results.put("resolved", false);
            results.put("saved", false);
            
            return results;
        }
    }
    
    /**
     * Repair log paths for a server
     * @param server The game server
     * @return Repair results
     */
    public Map<String, Object> repairLogPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Repairing log paths for server: {}", server.getName());
            
            // Validate connection
            if (!connector.testConnection(server)) {
                logger.error("Connection test failed for server: {}", server.getName());
                results.put("error", "Connection test failed");
                return results;
            }
            
            // Current path
            String currentPath = server.getLogDirectory();
            results.put("originalPath", currentPath);
            
            // Try to resolve path
            String resolvedPath = LogParserFix.resolveServerLogPath(server, connector);
            
            if (resolvedPath == null) {
                logger.warn("Could not resolve log path for server: {}", server.getName());
                results.put("resolved", false);
                return results;
            }
            
            // Update server
            boolean updated = LogParserFix.updateServerLogPath(server, resolvedPath);
            
            if (updated) {
                logger.info("Updated log path for server {}: {} -> {}", 
                    server.getName(), currentPath, resolvedPath);
                
                results.put("resolved", true);
                results.put("path", resolvedPath);
                
                // Save server
                repository.save(server);
                results.put("saved", true);
            } else {
                logger.warn("Failed to update log path for server: {}", server.getName());
                results.put("resolved", false);
                results.put("saved", false);
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Error repairing log paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            
            results.put("error", e.getMessage());
            results.put("resolved", false);
            results.put("saved", false);
            
            return results;
        }
    }
    
    /**
     * Quick check for CSV path
     * @param server The game server
     * @return True if path is valid
     */
    public boolean isValidCsvPath(GameServer server) {
        try {
            // Try to resolve path
            String resolvedPath = CsvParsingFix.resolveServerCsvPath(server, connector);
            return resolvedPath != null;
        } catch (Exception e) {
            logger.error("Error checking CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Quick check for log path
     * @param server The game server
     * @return True if path is valid
     */
    public boolean isValidLogPath(GameServer server) {
        try {
            // Try to resolve path
            String resolvedPath = LogParserFix.resolveServerLogPath(server, connector);
            return resolvedPath != null;
        } catch (Exception e) {
            logger.error("Error checking log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
}