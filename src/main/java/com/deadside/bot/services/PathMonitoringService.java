package com.deadside.bot.services;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.fixes.ParserPathTracker;
import com.deadside.bot.parsers.fixes.PathResolutionManager;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for monitoring and automatically repairing path issues
 * This service runs in the background and periodically checks server paths
 * for issues and repairs them if needed
 */
public class PathMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(PathMonitoringService.class);
    
    // The singleton instance
    private static PathMonitoringService instance;
    
    // Dependencies
    private final GameServerRepository gameServerRepository;
    private final SftpConnector sftpConnector;
    
    // Scheduler for periodic checks
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Statistics
    private final AtomicInteger totalChecks = new AtomicInteger(0);
    private final AtomicInteger totalFixed = new AtomicInteger(0);
    
    /**
     * Private constructor for singleton pattern
     */
    private PathMonitoringService(GameServerRepository gameServerRepository, SftpConnector sftpConnector) {
        this.gameServerRepository = gameServerRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Get the singleton instance
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     * @return The singleton instance
     */
    public static synchronized PathMonitoringService getInstance(
            GameServerRepository gameServerRepository, SftpConnector sftpConnector) {
        if (instance == null) {
            instance = new PathMonitoringService(gameServerRepository, sftpConnector);
        }
        return instance;
    }
    
    /**
     * Start the monitoring service
     * This will schedule periodic checks for path issues
     */
    public void start() {
        logger.info("Starting path monitoring service");
        
        // Schedule periodic checks
        scheduler.scheduleAtFixedRate(this::checkAllServerPaths, 
            5, 30, TimeUnit.MINUTES); // Check every 30 minutes
        
        logger.info("Path monitoring service started");
    }
    
    /**
     * Stop the monitoring service
     */
    public void stop() {
        logger.info("Stopping path monitoring service");
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Path monitoring service stopped");
    }
    
    /**
     * Check all server paths for issues
     */
    public void checkAllServerPaths() {
        try {
            logger.info("Checking all server paths for issues");
            totalChecks.incrementAndGet();
            
            // Get all guild IDs
            List<Long> guildIds = gameServerRepository.getDistinctGuildIds();
            int fixedInThisRun = 0;
            
            for (Long guildId : guildIds) {
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
                        
                        // Check and fix paths
                        if (checkAndFixServerPaths(server)) {
                            fixedInThisRun++;
                        }
                    }
                } finally {
                    // Always clear context
                    com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
                }
            }
            
            if (fixedInThisRun > 0) {
                totalFixed.addAndGet(fixedInThisRun);
                logger.info("Fixed {} server paths in this check", fixedInThisRun);
            } else {
                logger.info("No path issues found in this check");
            }
        } catch (Exception e) {
            logger.error("Error checking server paths: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check and fix paths for a single server
     * @param server The server to check
     * @return True if any paths were fixed
     */
    private boolean checkAndFixServerPaths(GameServer server) {
        try {
            // Try to use the path resolution manager if available
            if (PathResolutionManager.getInstance() != null) {
                return PathResolutionManager.getInstance().fixPathsForServer(server);
            }
            
            // Otherwise, use our own logic
            boolean anyFixed = false;
            
            // Check CSV path
            String csvPath = server.getDeathlogsDirectory();
            boolean csvPathOk = csvPath != null && !csvPath.isEmpty() && 
                              (csvPath.contains("/actual1/deathlogs") || csvPath.contains("\\actual1\\deathlogs"));
            
            if (!csvPathOk) {
                // Construct the standard path
                String host = server.getSftpHost();
                if (host == null || host.isEmpty()) {
                    host = server.getHost();
                }
                
                String serverName = server.getServerId();
                if (serverName == null || serverName.isEmpty()) {
                    serverName = server.getName().replaceAll("\\s+", "_");
                }
                
                String standardPath = host + "_" + serverName + "/actual1/deathlogs";
                server.setDeathlogsDirectory(standardPath);
                
                // Try to connect to this path
                try {
                    if (sftpConnector.testConnection(server, standardPath)) {
                        logger.info("Fixed CSV path for server {}: {}", server.getName(), standardPath);
                        
                        // Record successful path
                        ParserPathTracker.getInstance().recordSuccessfulPath(
                            server, ParserPathTracker.CATEGORY_CSV, standardPath);
                    }
                } catch (Exception e) {
                    logger.debug("Could not test CSV path: {}", e.getMessage());
                }
                
                anyFixed = true;
            }
            
            // Check Log path
            String logPath = server.getLogDirectory();
            boolean logPathOk = logPath != null && !logPath.isEmpty() && 
                              (logPath.contains("/Logs") || logPath.contains("\\Logs"));
            
            if (!logPathOk) {
                // Construct the standard path
                String host = server.getSftpHost();
                if (host == null || host.isEmpty()) {
                    host = server.getHost();
                }
                
                String serverName = server.getServerId();
                if (serverName == null || serverName.isEmpty()) {
                    serverName = server.getName().replaceAll("\\s+", "_");
                }
                
                String standardPath = host + "_" + serverName + "/Logs";
                server.setLogDirectory(standardPath);
                
                // Try to connect to this path
                try {
                    if (sftpConnector.testConnection(server, standardPath)) {
                        logger.info("Fixed Log path for server {}: {}", server.getName(), standardPath);
                        
                        // Record successful path
                        ParserPathTracker.getInstance().recordSuccessfulPath(
                            server, ParserPathTracker.CATEGORY_LOG, standardPath);
                    }
                } catch (Exception e) {
                    logger.debug("Could not test Log path: {}", e.getMessage());
                }
                
                anyFixed = true;
            }
            
            // Save the server if any paths were fixed
            if (anyFixed) {
                gameServerRepository.save(server);
            }
            
            return anyFixed;
        } catch (Exception e) {
            logger.error("Error checking and fixing paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get statistics about the monitoring service
     * @return Statistics as a formatted string
     */
    public String getStatistics() {
        return String.format("Path Monitoring Stats: Checks=%d, Fixed=%d, Ratio=%.2f%%",
            totalChecks.get(), totalFixed.get(),
            totalChecks.get() > 0 ? (totalFixed.get() * 100.0 / totalChecks.get()) : 0);
    }
}