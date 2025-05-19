package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central manager for path resolution and repair
 * This class provides methods for resolving and repairing path issues
 * across the system
 */
public class PathResolutionManager {
    private static final Logger logger = LoggerFactory.getLogger(PathResolutionManager.class);
    
    // The singleton instance
    private static PathResolutionManager instance;
    
    // Dependencies
    private GameServerRepository gameServerRepository;
    private SftpConnector sftpConnector;
    
    // Statistics for tracking
    private final AtomicInteger totalPathsFixed = new AtomicInteger(0);
    private final AtomicInteger totalPathsChecked = new AtomicInteger(0);
    
    /**
     * Private constructor for singleton pattern
     */
    private PathResolutionManager() {
        logger.info("PathResolutionManager initialized");
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized PathResolutionManager getInstance() {
        if (instance == null) {
            instance = new PathResolutionManager();
        }
        return instance;
    }
    
    /**
     * Initialize the manager with dependencies
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     */
    public void initialize(GameServerRepository gameServerRepository, SftpConnector sftpConnector) {
        this.gameServerRepository = gameServerRepository;
        this.sftpConnector = sftpConnector;
        
        logger.info("PathResolutionManager initialized with dependencies");
    }
    
    /**
     * Fix paths for a specific server
     * @param server The game server
     * @return True if any paths were fixed
     */
    public boolean fixPathsForServer(GameServer server) {
        if (server == null) {
            logger.warn("Cannot fix paths for null server");
            return false;
        }
        
        try {
            logger.debug("Checking paths for server {}", server.getName());
            totalPathsChecked.incrementAndGet();
            
            boolean anyFixed = false;
            
            // Check and fix CSV path
            boolean csvFixed = fixCsvPath(server);
            
            // Check and fix Log path
            boolean logFixed = fixLogPath(server);
            
            if (csvFixed || logFixed) {
                // Save the server if any paths were fixed
                gameServerRepository.save(server);
                
                totalPathsFixed.incrementAndGet();
                logger.info("Fixed paths for server {}: CSV={}, Log={}", 
                    server.getName(), csvFixed, logFixed);
                
                anyFixed = true;
            } else {
                logger.debug("No path issues found for server {}", server.getName());
            }
            
            return anyFixed;
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fix the CSV path for a server
     * @param server The game server
     * @return True if the path was fixed
     */
    private boolean fixCsvPath(GameServer server) {
        try {
            // Get the current path
            String currentPath = server.getDeathlogsDirectory();
            
            // Check if the current path exists and is valid
            boolean currentPathValid = isValidCsvPath(currentPath);
            boolean currentPathExists = currentPathValid && 
                testPath(server, currentPath);
            
            if (currentPathExists) {
                // Current path is valid and exists, record it
                ParserPathTracker.getInstance().recordSuccessfulPath(
                    server, ParserPathTracker.CATEGORY_CSV, currentPath);
                
                return false;  // No need to fix
            }
            
            // Try to find a valid path
            List<String> recommendedPaths = 
                ParserPathTracker.getInstance().getRecommendedPaths(
                    server, ParserPathTracker.CATEGORY_CSV);
            
            for (String path : recommendedPaths) {
                // Skip the current path
                if (path.equals(currentPath)) {
                    continue;
                }
                
                // Test the path
                if (isValidCsvPath(path) && testPath(server, path)) {
                    // Found a valid path, set it
                    server.setDeathlogsDirectory(path);
                    
                    // Record it
                    ParserPathTracker.getInstance().recordSuccessfulPath(
                        server, ParserPathTracker.CATEGORY_CSV, path);
                    
                    logger.info("Fixed CSV path for server {}: {} -> {}", 
                        server.getName(), currentPath, path);
                    
                    return true;
                }
            }
            
            logger.warn("Could not find a valid CSV path for server {}", server.getName());
            return false;
        } catch (Exception e) {
            logger.error("Error fixing CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Fix the Log path for a server
     * @param server The game server
     * @return True if the path was fixed
     */
    private boolean fixLogPath(GameServer server) {
        try {
            // Get the current path
            String currentPath = server.getLogDirectory();
            
            // Check if the current path exists and is valid
            boolean currentPathValid = isValidLogPath(currentPath);
            boolean currentPathExists = currentPathValid && 
                testPath(server, currentPath);
            
            if (currentPathExists) {
                // Current path is valid and exists, record it
                ParserPathTracker.getInstance().recordSuccessfulPath(
                    server, ParserPathTracker.CATEGORY_LOG, currentPath);
                
                return false;  // No need to fix
            }
            
            // Try to find a valid path
            List<String> recommendedPaths = 
                ParserPathTracker.getInstance().getRecommendedPaths(
                    server, ParserPathTracker.CATEGORY_LOG);
            
            for (String path : recommendedPaths) {
                // Skip the current path
                if (path.equals(currentPath)) {
                    continue;
                }
                
                // Test the path
                if (isValidLogPath(path) && testPath(server, path)) {
                    // Found a valid path, set it
                    server.setLogDirectory(path);
                    
                    // Record it
                    ParserPathTracker.getInstance().recordSuccessfulPath(
                        server, ParserPathTracker.CATEGORY_LOG, path);
                    
                    logger.info("Fixed Log path for server {}: {} -> {}", 
                        server.getName(), currentPath, path);
                    
                    return true;
                }
            }
            
            logger.warn("Could not find a valid Log path for server {}", server.getName());
            return false;
        } catch (Exception e) {
            logger.error("Error fixing Log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if a CSV path is valid
     * @param path The path to check
     * @return True if the path is valid
     */
    private boolean isValidCsvPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/actual1/deathlogs") || 
               path.contains("\\actual1\\deathlogs") ||
               path.contains("/actual/deathlogs") || 
               path.contains("\\actual\\deathlogs");
    }
    
    /**
     * Check if a Log path is valid
     * @param path The path to check
     * @return True if the path is valid
     */
    private boolean isValidLogPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        return path.contains("/Logs") || 
               path.contains("\\Logs");
    }
    
    /**
     * Test if a path exists and is accessible
     * @param server The game server
     * @param path The path to test
     * @return True if the path exists and is accessible
     */
    private boolean testPath(GameServer server, String path) {
        try {
            return sftpConnector.testConnection(server, path);
        } catch (Exception e) {
            logger.debug("Error testing path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Check and fix paths for all servers in a guild
     * @param guildId The guild ID
     * @return Number of servers fixed
     */
    public int fixPathsForGuild(long guildId) {
        int fixed = 0;
        
        try {
            // Set context for guild
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                // Get all servers for this guild
                List<GameServer> servers = gameServerRepository.findAllByGuildId(guildId);
                
                for (GameServer server : servers) {
                    // Skip restricted servers
                    if (server.hasRestrictedIsolation()) {
                        continue;
                    }
                    
                    if (fixPathsForServer(server)) {
                        fixed++;
                    }
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error fixing paths for guild {}: {}", guildId, e.getMessage(), e);
        }
        
        return fixed;
    }
    
    /**
     * Get statistics about the path resolution manager
     * @return Statistics as a formatted string
     */
    public String getStatistics() {
        return String.format("Path Resolution Stats: Checked=%d, Fixed=%d, Ratio=%.2f%%",
            totalPathsChecked.get(), totalPathsFixed.get(),
            totalPathsChecked.get() > 0 ? 
                (totalPathsFixed.get() * 100.0 / totalPathsChecked.get()) : 0);
    }
}