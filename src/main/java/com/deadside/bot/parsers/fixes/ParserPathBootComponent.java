package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Boot component for the parser path resolution system
 * This class is responsible for bootstrapping the parser path resolution system
 * during application startup
 */
public class ParserPathBootComponent {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathBootComponent.class);
    
    // Static initialization flag to ensure we only bootstrap once
    private static boolean initialized = false;
    
    /**
     * Bootstrap the parser path resolution system
     * This method should be called during application startup
     * @param gameServerRepository Repository for server data
     * @param sftpConnector SFTP connector for file access
     * @param csvParser CSV parser component
     * @param logParser Log parser component
     * @return True if bootstrap was successful
     */
    public static synchronized boolean bootstrap(GameServerRepository gameServerRepository,
                                             SftpConnector sftpConnector,
                                             DeadsideCsvParser csvParser,
                                             DeadsideLogParser logParser) {
        if (initialized) {
            logger.info("Parser path resolution system already bootstrapped");
            return true;
        }
        
        logger.info("Bootstrapping parser path resolution system");
        
        try {
            // Create configuration loader
            ParserPathConfigurationLoader configLoader = new ParserPathConfigurationLoader(
                gameServerRepository, sftpConnector, csvParser, logParser);
            
            // Load configuration
            boolean success = configLoader.loadConfiguration();
            
            if (success) {
                initialized = true;
                logger.info("Parser path resolution system bootstrapped successfully");
                
                // Run an immediate path resolution pass
                Thread initialResolutionThread = new Thread(() -> {
                    try {
                        // Wait a bit for system startup
                        Thread.sleep(5000);
                        
                        logger.info("Running initial path resolution pass");
                        int fixedCount = configLoader.runImmediatePathResolution();
                        
                        if (fixedCount >= 0) {
                            logger.info("Initial path resolution fixed {} paths", fixedCount);
                        } else {
                            logger.warn("Initial path resolution failed");
                        }
                    } catch (Exception e) {
                        logger.error("Error during initial path resolution: {}", e.getMessage(), e);
                    }
                });
                
                initialResolutionThread.setName("InitialPathResolution");
                initialResolutionThread.setDaemon(true);
                initialResolutionThread.start();
                
                return true;
            } else {
                logger.error("Failed to bootstrap parser path resolution system");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error bootstrapping parser path resolution system: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if the parser path resolution system is bootstrapped
     * @return True if bootstrapped
     */
    public static boolean isBootstrapped() {
        return initialized;
    }
    
    /**
     * Get statistics about the parser path resolution system
     * @return A statistics summary
     */
    public static String getStatistics() {
        if (!initialized) {
            return "Parser path resolution system not bootstrapped";
        }
        
        return DeadsideParserPathRegistry.getInstance().getStatistics();
    }
}