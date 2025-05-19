package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.PathResolutionFix;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extensions for parser classes to handle path resolution
 * This class provides static methods that can be called from parsers
 */
public class ParserExtensions {
    private static final Logger logger = LoggerFactory.getLogger(ParserExtensions.class);
    
    // Components
    private static GameServerRepository gameServerRepository;
    private static SftpConnector sftpConnector;
    
    // Initialization flag
    private static boolean initialized = false;
    
    /**
     * Initialize the parser extensions
     * @param serverRepository Server repository
     * @param connector SFTP connector
     */
    public static synchronized void initialize(
            GameServerRepository serverRepository, 
            SftpConnector connector) {
        
        if (initialized) {
            return;
        }
        
        gameServerRepository = serverRepository;
        sftpConnector = connector;
        initialized = true;
        
        logger.info("Parser extensions initialized");
    }
    
    /**
     * Process CSV path for a server
     * @param server The server
     * @param originalPath Original path
     * @return Fixed path or original if not applicable
     */
    public static String processCsvPath(GameServer server, String originalPath) {
        if (!initialized || server == null) {
            return originalPath;
        }
        
        try {
            // Use PathResolutionFix to get the correct path
            String resolvedPath = PathResolutionFix.resolveCsvPath(server, sftpConnector);
            
            if (resolvedPath != null && !resolvedPath.equals(originalPath)) {
                // Update the server
                server.setDeathlogsDirectory(resolvedPath);
                gameServerRepository.save(server);
                
                logger.debug("Fixed CSV path for server {}: {} -> {}", 
                    server.getName(), originalPath, resolvedPath);
                
                return resolvedPath;
            }
        } catch (Exception e) {
            logger.error("Error processing CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
        
        return originalPath;
    }
    
    /**
     * Process Log path for a server
     * @param server The server
     * @param originalPath Original path
     * @return Fixed path or original if not applicable
     */
    public static String processLogPath(GameServer server, String originalPath) {
        if (!initialized || server == null) {
            return originalPath;
        }
        
        try {
            // Use PathResolutionFix to get the correct path
            String resolvedPath = PathResolutionFix.resolveLogPath(server, sftpConnector);
            
            if (resolvedPath != null && !resolvedPath.equals(originalPath)) {
                // Update the server
                server.setLogDirectory(resolvedPath);
                gameServerRepository.save(server);
                
                logger.debug("Fixed log path for server {}: {} -> {}", 
                    server.getName(), originalPath, resolvedPath);
                
                return resolvedPath;
            }
        } catch (Exception e) {
            logger.error("Error processing log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
        }
        
        return originalPath;
    }
    
    /**
     * Check if extensions are initialized
     * @return True if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}