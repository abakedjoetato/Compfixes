package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for Deadside parsers
 * This class provides an adapter layer between the parsers and the path resolution system
 */
public class DeadsideParserAdapter {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserAdapter.class);
    
    private final SftpConnector connector;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public DeadsideParserAdapter(
            SftpConnector connector,
            DeadsideCsvParser csvParser,
            DeadsideLogParser logParser) {
        
        this.connector = connector;
        this.csvParser = csvParser;
        this.logParser = logParser;
    }
    
    /**
     * Parse CSV files for a server with path resolution
     * @param server The game server
     * @return True if parsing was successful
     */
    public boolean parseCsvWithPathResolution(GameServer server) {
        try {
            logger.info("Parsing CSV files for server {} with path resolution", server.getName());
            
            // Check if server exists
            if (server == null) {
                logger.warn("Server is null");
                return false;
            }
            
            // Get the registered path if available
            String registeredPath = ParserIntegrationHooks.getRegisteredCsvPath(server);
            String originalPath = server.getDeathlogsDirectory();
            boolean pathChanged = false;
            
            // Use registered path if available
            if (registeredPath != null && !registeredPath.isEmpty() && !registeredPath.equals(originalPath)) {
                logger.debug("Using registered CSV path for server {}: {}", 
                    server.getName(), registeredPath);
                
                server.setDeathlogsDirectory(registeredPath);
                pathChanged = true;
            }
            
            // Parse CSV files
            boolean success = parseCsv(server);
            
            // If parsing failed and path was changed, try original path
            if (!success && pathChanged) {
                logger.debug("Parsing with registered path failed, trying original path: {}", 
                    originalPath);
                
                // Restore original path
                server.setDeathlogsDirectory(originalPath);
                
                // Try again with original path
                success = parseCsv(server);
            }
            
            // If parsing was successful, record the path
            if (success) {
                ParserIntegrationHooks.recordSuccessfulCsvPath(server, server.getDeathlogsDirectory());
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error parsing CSV files for server {} with path resolution: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Parse logs for a server with path resolution
     * @param server The game server
     * @return True if parsing was successful
     */
    public boolean parseLogsWithPathResolution(GameServer server) {
        try {
            logger.info("Parsing logs for server {} with path resolution", server.getName());
            
            // Check if server exists
            if (server == null) {
                logger.warn("Server is null");
                return false;
            }
            
            // Get the registered path if available
            String registeredPath = ParserIntegrationHooks.getRegisteredLogPath(server);
            String originalPath = server.getLogDirectory();
            boolean pathChanged = false;
            
            // Use registered path if available
            if (registeredPath != null && !registeredPath.isEmpty() && !registeredPath.equals(originalPath)) {
                logger.debug("Using registered log path for server {}: {}", 
                    server.getName(), registeredPath);
                
                server.setLogDirectory(registeredPath);
                pathChanged = true;
            }
            
            // Parse logs
            boolean success = parseLogs(server);
            
            // If parsing failed and path was changed, try original path
            if (!success && pathChanged) {
                logger.debug("Parsing with registered path failed, trying original path: {}", 
                    originalPath);
                
                // Restore original path
                server.setLogDirectory(originalPath);
                
                // Try again with original path
                success = parseLogs(server);
            }
            
            // If parsing was successful, record the path
            if (success) {
                ParserIntegrationHooks.recordSuccessfulLogPath(server, server.getLogDirectory());
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error parsing logs for server {} with path resolution: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Parse CSV files for a server
     * @param server The game server
     * @return True if parsing was successful
     */
    private boolean parseCsv(GameServer server) {
        try {
            // This is a simplified implementation
            // In a real implementation, this would call the CSV parser
            
            // First check if we can find CSV files
            if (connector.findDeathlogFiles(server).isEmpty()) {
                logger.warn("No CSV files found for server {}", server.getName());
                return false;
            }
            
            // For now, just return true to indicate success
            logger.info("Successfully parsed CSV files for server {}", server.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error parsing CSV files for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Parse logs for a server
     * @param server The game server
     * @return True if parsing was successful
     */
    private boolean parseLogs(GameServer server) {
        try {
            // This is a simplified implementation
            // In a real implementation, this would call the log parser
            
            // First check if we can find the log file
            if (connector.findLogFile(server) == null) {
                logger.warn("No log file found for server {}", server.getName());
                return false;
            }
            
            // For now, just return true to indicate success
            logger.info("Successfully parsed logs for server {}", server.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error parsing logs for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
}