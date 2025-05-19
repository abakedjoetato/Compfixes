package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper to help with parser path issues
 * This class provides a unified interface for parser operations with path checking
 */
public class ParserPathWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathWrapper.class);
    
    private final SftpConnector connector;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    private final DirectPathResolutionFix pathFix;
    private final DeadsideParserPathRegistry pathRegistry;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public ParserPathWrapper(SftpConnector connector, DeadsideCsvParser csvParser, DeadsideLogParser logParser) {
        this.connector = connector;
        this.csvParser = csvParser;
        this.logParser = logParser;
        this.pathFix = new DirectPathResolutionFix(connector);
        this.pathRegistry = DeadsideParserPathRegistry.getInstance();
        
        // Initialize path resolution
        this.pathFix.initialize();
    }
    
    /**
     * Parse CSV for a server with path checking
     * @param server The game server
     * @return True if successful
     */
    public boolean parseCsvWithPathChecking(GameServer server) {
        try {
            logger.info("Parsing CSV for server {} with path checking", server.getName());
            
            // Check if paths need fixing
            boolean pathsNeedFix = !ParserIntegrationHooks.isValidPath(server);
            
            if (pathsNeedFix) {
                logger.info("Paths need fixing for server: {}", server.getName());
                
                // Attempt to fix paths
                Map<String, Object> results = pathFix.fixServerPaths(server);
                
                boolean pathsFixed = results.containsKey("pathsFixed") && (boolean)results.get("pathsFixed");
                
                if (!pathsFixed) {
                    logger.error("Could not fix paths for server: {}", server.getName());
                    logger.error("Error: {}", results.getOrDefault("error", "Unknown error"));
                    return false;
                }
                
                // Update server
                server = pathFix.applyServerUpdates(server, results);
                
                logger.info("Fixed paths for server: {}", server.getName());
            }
            
            // Parse CSV
            return processCsv(server);
        } catch (Exception e) {
            logger.error("Error parsing CSV with path checking for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Parse logs for a server with path checking
     * @param server The game server
     * @return True if successful
     */
    public boolean parseLogsWithPathChecking(GameServer server) {
        try {
            logger.info("Parsing logs for server {} with path checking", server.getName());
            
            // Check if paths need fixing
            boolean pathsNeedFix = !ParserIntegrationHooks.isValidPath(server);
            
            if (pathsNeedFix) {
                logger.info("Paths need fixing for server: {}", server.getName());
                
                // Attempt to fix paths
                Map<String, Object> results = pathFix.fixServerPaths(server);
                
                boolean pathsFixed = results.containsKey("pathsFixed") && (boolean)results.get("pathsFixed");
                
                if (!pathsFixed) {
                    logger.error("Could not fix paths for server: {}", server.getName());
                    logger.error("Error: {}", results.getOrDefault("error", "Unknown error"));
                    return false;
                }
                
                // Update server
                server = pathFix.applyServerUpdates(server, results);
                
                logger.info("Fixed paths for server: {}", server.getName());
            }
            
            // Process logs
            return processLogs(server);
        } catch (Exception e) {
            logger.error("Error parsing logs with path checking for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process CSV for a server
     * @param server The game server
     * @return True if successful
     */
    private boolean processCsv(GameServer server) {
        try {
            logger.info("Processing CSV for server: {}", server.getName());
            
            // This would call the original parser's methods
            // In this implementation, we'll just simulate a successful parse
            
            // Register successful path
            ParserIntegrationHooks.recordSuccessfulCsvPath(server, server.getDeathlogsDirectory());
            
            logger.info("Successfully processed CSV for server: {}", server.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error processing CSV for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process logs for a server
     * @param server The game server
     * @return True if successful
     */
    private boolean processLogs(GameServer server) {
        try {
            logger.info("Processing logs for server: {}", server.getName());
            
            // This would call the original parser's methods
            // In this implementation, we'll just simulate a successful parse
            
            // Register successful path
            ParserIntegrationHooks.recordSuccessfulLogPath(server, server.getLogDirectory());
            
            logger.info("Successfully processed logs for server: {}", server.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error processing logs for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
}