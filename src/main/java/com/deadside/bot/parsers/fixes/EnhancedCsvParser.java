package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Enhanced CSV parser that integrates with the path resolution system
 * This class extends the DeadsideCsvParser to provide improved path resolution
 */
public class EnhancedCsvParser {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedCsvParser.class);
    
    // The original CSV parser
    private final DeadsideCsvParser originalParser;
    
    // The SFTP connector
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param originalParser The original CSV parser
     * @param sftpConnector The SFTP connector
     */
    public EnhancedCsvParser(DeadsideCsvParser originalParser, SftpConnector sftpConnector) {
        this.originalParser = originalParser;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Process death logs for a server with path resolution
     * @param server The game server
     * @param processHistorical Whether to process historical data
     * @return Number of deaths processed
     */
    public int processDeathLogsWithPathResolution(GameServer server, boolean processHistorical) {
        if (server == null) {
            logger.warn("Cannot process death logs for null server");
            return 0;
        }
        
        try {
            // First try to resolve the path if it's invalid
            if (!isValidCsvPath(server)) {
                logger.info("CSV path for server {} appears to be invalid, attempting to resolve", 
                    server.getName());
                
                resolveCsvPath(server);
            }
            
            // Forward to the original parser
            return originalParser.processDeathLogs(server, processHistorical);
        } catch (Exception e) {
            logger.error("Error processing death logs with path resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Process death logs for a server with path resolution (default behavior)
     * @param server The game server
     * @return Number of deaths processed
     */
    public int processDeathLogsWithPathResolution(GameServer server) {
        return processDeathLogsWithPathResolution(server, false);
    }
    
    /**
     * Check if a server has a valid CSV path
     * @param server The game server
     * @return True if the path is valid
     */
    private boolean isValidCsvPath(GameServer server) {
        if (server == null) {
            return false;
        }
        
        String path = server.getDeathlogsDirectory();
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Check if the path has the expected structure
        boolean hasExpectedStructure = path.contains("/actual1/deathlogs") || 
                                   path.contains("\\actual1\\deathlogs") ||
                                   path.contains("/actual/deathlogs") || 
                                   path.contains("\\actual\\deathlogs");
        
        if (!hasExpectedStructure) {
            return false;
        }
        
        // Try to test the path
        try {
            List<String> files = sftpConnector.findDeathlogFiles(server);
            return files != null && !files.isEmpty();
        } catch (Exception e) {
            logger.debug("Error testing CSV path for server {}: {}", 
                server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Resolve the CSV path for a server
     * @param server The game server
     * @return True if the path was resolved
     */
    private boolean resolveCsvPath(GameServer server) {
        try {
            // Use ParserPathIntegrationManager to resolve the path
            String originalPath = server.getDeathlogsDirectory();
            
            String resolvedPath = ParserPathIntegrationManager.getInstance()
                .resolveCsvPath(server);
            
            if (resolvedPath != null && !resolvedPath.equals(originalPath)) {
                logger.info("Resolved CSV path for server {}: {} -> {}", 
                    server.getName(), originalPath, resolvedPath);
                
                // Path was resolved
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error resolving CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process a death log file content directly with path resolution
     * @param server The game server
     * @param content The file content
     * @return Number of deaths processed
     */
    public int processDeathLogContentWithPathResolution(GameServer server, String content) {
        if (server == null || content == null) {
            return 0;
        }
        
        try {
            // First try to resolve the path if it's invalid
            if (!isValidCsvPath(server)) {
                logger.info("CSV path for server {} appears to be invalid, attempting to resolve", 
                    server.getName());
                
                resolveCsvPath(server);
            }
            
            // Forward to the original parser
            return originalParser.processDeathLogContent(server, content);
        } catch (Exception e) {
            logger.error("Error processing death log content with path resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
}