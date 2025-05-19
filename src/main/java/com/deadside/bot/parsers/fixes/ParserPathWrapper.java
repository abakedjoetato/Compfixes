package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for parser path integration
 * This class provides direct integration with the parser classes
 * to ensure proper path resolution
 */
public class ParserPathWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathWrapper.class);
    
    // Dependencies
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    private final SftpConnector sftpConnector;
    private final GameServerRepository gameServerRepository;
    
    // Whether path resolution is enabled
    private boolean pathResolutionEnabled = true;
    
    /**
     * Constructor
     * @param csvParser CSV parser
     * @param logParser Log parser
     * @param sftpConnector SFTP connector
     * @param gameServerRepository Game server repository
     */
    public ParserPathWrapper(DeadsideCsvParser csvParser, DeadsideLogParser logParser,
                            SftpConnector sftpConnector, GameServerRepository gameServerRepository) {
        this.csvParser = csvParser;
        this.logParser = logParser;
        this.sftpConnector = sftpConnector;
        this.gameServerRepository = gameServerRepository;
        
        // Initialize the direct path resolution fix
        DirectPathResolutionFix.initialize();
        
        logger.info("ParserPathWrapper initialized");
    }
    
    /**
     * Enable or disable path resolution
     * @param enabled Whether path resolution is enabled
     */
    public void setPathResolutionEnabled(boolean enabled) {
        this.pathResolutionEnabled = enabled;
        logger.info("Path resolution {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Process death logs for a server with path resolution
     * @param server The game server
     * @param processHistorical Whether to process historical data
     * @return Number of deaths processed
     */
    public int processDeathLogsWithPathResolution(GameServer server, boolean processHistorical) {
        if (server == null) {
            return 0;
        }
        
        try {
            // Fix path if needed
            if (pathResolutionEnabled) {
                fixPathsIfNeeded(server);
            }
            
            // Call the original parser
            return csvParser.processDeathLogs(server, processHistorical);
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
     * Process logs for a server with path resolution
     * @param server The game server
     * @return Number of log lines processed
     */
    public int processLogsWithPathResolution(GameServer server) {
        if (server == null) {
            return 0;
        }
        
        try {
            // Fix path if needed
            if (pathResolutionEnabled) {
                fixPathsIfNeeded(server);
            }
            
            // Call the original parser
            return logParser.processLogs(server);
        } catch (Exception e) {
            logger.error("Error processing logs with path resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Process a death log content with path resolution
     * @param server The game server
     * @param content The death log content
     * @return Number of deaths processed
     */
    public int processDeathLogContentWithPathResolution(GameServer server, String content) {
        if (server == null || content == null) {
            return 0;
        }
        
        try {
            // Fix path if needed
            if (pathResolutionEnabled) {
                fixPathsIfNeeded(server);
            }
            
            // Call the original parser
            return csvParser.processDeathLogContent(server, content);
        } catch (Exception e) {
            logger.error("Error processing death log content with path resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Fix paths for a server if needed
     * @param server The server
     */
    private void fixPathsIfNeeded(GameServer server) {
        try {
            // Check if paths need fixing
            boolean csvPathValid = isValidCsvPath(server);
            boolean logPathValid = isValidLogPath(server);
            
            if (!csvPathValid || !logPathValid) {
                logger.info("Server {} has invalid paths, attempting to fix (CSV valid: {}, Log valid: {})", 
                    server.getName(), csvPathValid, logPathValid);
                
                boolean pathsFixed = DirectPathResolutionFix.fixServerPaths(server, sftpConnector);
                
                if (pathsFixed) {
                    // Save the server
                    gameServerRepository.save(server);
                    
                    logger.info("Fixed paths for server {}: CSV path={}, Log path={}", 
                        server.getName(), server.getDeathlogsDirectory(), server.getLogDirectory());
                } else {
                    logger.warn("Could not fix paths for server {}", server.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Check if a server has a valid CSV path
     * @param server The server
     * @return True if valid
     */
    private boolean isValidCsvPath(GameServer server) {
        String path = server.getDeathlogsDirectory();
        
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/actual1/deathlogs") || 
               path.contains("\\actual1\\deathlogs") ||
               path.contains("/actual/deathlogs") || 
               path.contains("\\actual\\deathlogs");
    }
    
    /**
     * Check if a server has a valid log path
     * @param server The server
     * @return True if valid
     */
    private boolean isValidLogPath(GameServer server) {
        String path = server.getLogDirectory();
        
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/Logs") || path.contains("\\Logs");
    }
}