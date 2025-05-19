package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator for Deadside parser components
 * This class provides functionality to validate the parser components
 */
public class DeadsideParserValidator {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserValidator.class);
    
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
    public DeadsideParserValidator(
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
     * Validate basic functionality of parser components
     */
    public void validateBasicFunctionality() {
        logger.info("Validating basic functionality of parser components");
        
        Map<String, Boolean> results = new HashMap<>();
        
        // Check if connector is available
        results.put("connector", connector != null);
        
        // Check if repository is available
        results.put("repository", serverRepository != null);
        
        // Check if parsers are available
        results.put("csvParser", csvParser != null);
        results.put("logParser", logParser != null);
        
        // Log results
        for (Map.Entry<String, Boolean> entry : results.entrySet()) {
            logger.info("Component {} validated: {}", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Validate path resolution
     * @param serverId The server ID to validate
     * @return True if validation passed
     */
    public boolean validatePathResolution(Long serverId) {
        try {
            logger.info("Validating path resolution for server ID: {}", serverId);
            
            // Find server
            GameServer server = serverRepository.findById(serverId).orElse(null);
            
            if (server == null) {
                logger.warn("Server not found: {}", serverId);
                return false;
            }
            
            // Create path finder
            ParserPathFinder pathFinder = new ParserPathFinder(connector);
            
            // Find valid paths
            String csvPath = pathFinder.findValidCsvPath(server);
            String logPath = pathFinder.findValidLogPath(server);
            
            // Log results
            logger.info("Valid CSV path for server {}: {}", server.getName(), csvPath);
            logger.info("Valid log path for server {}: {}", server.getName(), logPath);
            
            return csvPath != null && logPath != null;
        } catch (Exception e) {
            logger.error("Error validating path resolution: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate path resolution for all servers
     * @return List of server IDs that passed validation
     */
    public List<Long> validateAllServers() {
        try {
            logger.info("Validating path resolution for all servers");
            
            // Find all servers
            List<GameServer> servers = serverRepository.findAll();
            
            if (servers == null || servers.isEmpty()) {
                logger.warn("No servers found");
                return new ArrayList<>();
            }
            
            List<Long> passedServers = new ArrayList<>();
            
            // Validate each server
            for (GameServer server : servers) {
                try {
                    // Create path finder
                    ParserPathFinder pathFinder = new ParserPathFinder(connector);
                    
                    // Find valid paths
                    String csvPath = pathFinder.findValidCsvPath(server);
                    String logPath = pathFinder.findValidLogPath(server);
                    
                    boolean passed = csvPath != null && logPath != null;
                    
                    if (passed) {
                        passedServers.add(Long.parseLong(server.getId().toString()));
                    }
                    
                    // Log results
                    logger.info("Server {} validated: {}", server.getName(), passed);
                } catch (Exception e) {
                    logger.error("Error validating server {}: {}", server.getName(), e.getMessage(), e);
                }
            }
            
            logger.info("Validated {}/{} servers", passedServers.size(), servers.size());
            
            return passedServers;
        } catch (Exception e) {
            logger.error("Error validating all servers: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}