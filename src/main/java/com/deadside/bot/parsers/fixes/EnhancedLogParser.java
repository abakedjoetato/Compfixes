package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Enhanced log parser that integrates with the path resolution system
 * This class extends the DeadsideLogParser to provide improved path resolution
 */
public class EnhancedLogParser {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedLogParser.class);
    
    // The original log parser
    private final DeadsideLogParser originalParser;
    
    // The SFTP connector
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param originalParser The original log parser
     * @param sftpConnector The SFTP connector
     */
    public EnhancedLogParser(DeadsideLogParser originalParser, SftpConnector sftpConnector) {
        this.originalParser = originalParser;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Process logs for a server with path resolution
     * @param server The game server
     * @return Number of log lines processed
     */
    public int processLogsWithPathResolution(GameServer server) {
        if (server == null) {
            logger.warn("Cannot process logs for null server");
            return 0;
        }
        
        try {
            // First try to resolve the path if it's invalid
            if (!isValidLogPath(server)) {
                logger.info("Log path for server {} appears to be invalid, attempting to resolve", 
                    server.getName());
                
                resolveLogPath(server);
            }
            
            // Forward to the original parser
            return originalParser.processLogs(server);
        } catch (Exception e) {
            logger.error("Error processing logs with path resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Check if a server has a valid log path
     * @param server The game server
     * @return True if the path is valid
     */
    private boolean isValidLogPath(GameServer server) {
        if (server == null) {
            return false;
        }
        
        String path = server.getLogDirectory();
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Check if the path has the expected structure
        boolean hasExpectedStructure = path.contains("/Logs") || 
                                      path.contains("\\Logs");
        
        if (!hasExpectedStructure) {
            return false;
        }
        
        // Try to test the path
        try {
            String logFile = sftpConnector.findLogFile(server);
            return logFile != null && !logFile.isEmpty();
        } catch (Exception e) {
            logger.debug("Error testing log path for server {}: {}", 
                server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Resolve the log path for a server
     * @param server The game server
     * @return True if the path was resolved
     */
    private boolean resolveLogPath(GameServer server) {
        try {
            // Use ParserPathIntegrationManager to resolve the path
            String originalPath = server.getLogDirectory();
            
            String resolvedPath = ParserPathIntegrationManager.getInstance()
                .resolveLogPath(server);
            
            if (resolvedPath != null && !resolvedPath.equals(originalPath)) {
                logger.info("Resolved log path for server {}: {} -> {}", 
                    server.getName(), originalPath, resolvedPath);
                
                // Path was resolved
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error resolving log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process a log file content directly with path resolution
     * @param server The game server
     * @param content The file content
     * @return Number of log lines processed
     */
    public int processLogContentWithPathResolution(GameServer server, String content) {
        if (server == null || content == null) {
            return 0;
        }
        
        try {
            // First try to resolve the path if it's invalid
            if (!isValidLogPath(server)) {
                logger.info("Log path for server {} appears to be invalid, attempting to resolve", 
                    server.getName());
                
                resolveLogPath(server);
            }
            
            // Forward to the original parser
            return originalParser.processLogContent(server, content);
        } catch (Exception e) {
            logger.error("Error processing log content with path resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
}