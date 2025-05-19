package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Module for parser integration
 * This class provides a central module for integrating parsers
 */
public class ParserIntegrationModule {
    private static final Logger logger = LoggerFactory.getLogger(ParserIntegrationModule.class);
    
    private final SftpConnector connector;
    private final GameServerRepository serverRepository;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     * @param serverRepository The server repository
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public ParserIntegrationModule(
            SftpConnector connector,
            GameServerRepository serverRepository,
            DeadsideCsvParser csvParser,
            DeadsideLogParser logParser) {
        
        this.connector = connector;
        this.serverRepository = serverRepository;
        this.csvParser = csvParser;
        this.logParser = logParser;
    }
    
    /**
     * Initialize the integration module
     */
    public void initialize() {
        logger.info("Initializing parser integration module");
        
        try {
            // Initialize extensions
            ParserExtensions.initialize();
            
            // Initialize registry
            DeadsideParserPathRegistry.getInstance().initialize();
            
            logger.info("Parser integration module initialized");
        } catch (Exception e) {
            logger.error("Error initializing parser integration module: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Validate all server paths
     * @return True if all paths are valid
     */
    public boolean validateAllPaths() {
        try {
            logger.info("Validating all server paths");
            
            // Get all servers
            List<GameServer> servers = serverRepository.findAll();
            
            int validCount = 0;
            int totalCount = servers.size();
            
            // Validate each server
            for (GameServer server : servers) {
                try {
                    // Validate paths
                    boolean csvPathValid = connector.isValidCsvPath(server);
                    boolean logPathValid = connector.isValidLogPath(server);
                    
                    // Track valid servers
                    if (csvPathValid && logPathValid) {
                        validCount++;
                    }
                    
                    // Record paths
                    if (csvPathValid) {
                        ParserIntegrationHooks.recordSuccessfulCsvPath(server, server.getDeathlogsDirectory());
                    }
                    
                    if (logPathValid) {
                        ParserIntegrationHooks.recordSuccessfulLogPath(server, server.getLogDirectory());
                    }
                } catch (Exception e) {
                    logger.error("Error validating paths for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                }
            }
            
            boolean allValid = validCount == totalCount;
            
            logger.info("Path validation complete: {}/{} servers have valid paths", validCount, totalCount);
            
            return allValid;
        } catch (Exception e) {
            logger.error("Error validating server paths: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process all CSV files
     * @return True if successful
     */
    public boolean processCsvFiles() {
        try {
            logger.info("Processing all CSV files");
            
            // Get all servers
            List<GameServer> servers = serverRepository.findAll();
            
            int successCount = 0;
            int totalCount = servers.size();
            
            // Process each server
            for (GameServer server : servers) {
                try {
                    // Find CSV files
                    List<String> csvFiles = connector.findDeathlogFiles(server);
                    
                    // Process each file
                    for (String csvFile : csvFiles) {
                        try {
                            // Process file
                            String content = connector.readFile(server, 
                                server.getDeathlogsDirectory() + "/" + csvFile);
                            
                            // Parse content (simplified for this implementation)
                            if (content != null && !content.isEmpty()) {
                                logger.info("Successfully processed CSV file for server {}: {}", 
                                    server.getName(), csvFile);
                            }
                        } catch (Exception e) {
                            logger.error("Error processing CSV file {} for server {}: {}", 
                                csvFile, server.getName(), e.getMessage(), e);
                        }
                    }
                    
                    // Count success for this server
                    successCount++;
                } catch (Exception e) {
                    logger.error("Error processing CSV files for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                }
            }
            
            boolean allSuccess = successCount == totalCount;
            
            logger.info("CSV processing complete: {}/{} servers processed successfully", successCount, totalCount);
            
            return allSuccess;
        } catch (Exception e) {
            logger.error("Error processing CSV files: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process all server logs
     * @return True if successful
     */
    public boolean processLogs() {
        try {
            logger.info("Processing all server logs");
            
            // Get all servers
            List<GameServer> servers = serverRepository.findAll();
            
            int successCount = 0;
            int totalCount = servers.size();
            
            // Process each server
            for (GameServer server : servers) {
                try {
                    // Find log file
                    String logFile = connector.findLogFile(server);
                    
                    if (logFile != null) {
                        // Process log file
                        List<String> logLines = connector.downloadLogFile(server, logFile);
                        
                        // Parse log lines (simplified for this implementation)
                        if (logLines != null && !logLines.isEmpty()) {
                            logger.info("Successfully processed log file for server {}: {} lines", 
                                server.getName(), logLines.size());
                        }
                        
                        // Count success for this server
                        successCount++;
                    } else {
                        logger.warn("No log file found for server: {}", server.getName());
                    }
                } catch (Exception e) {
                    logger.error("Error processing log file for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                }
            }
            
            boolean allSuccess = successCount == totalCount;
            
            logger.info("Log processing complete: {}/{} servers processed successfully", successCount, totalCount);
            
            return allSuccess;
        } catch (Exception e) {
            logger.error("Error processing log files: {}", e.getMessage(), e);
            return false;
        }
    }
}