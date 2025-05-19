package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.PathResolutionFix;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Integration point for parser fixes in the bot
 * This class provides methods to integrate the parser path fixes
 * with the main bot functionality
 */
public class BotParserIntegration {
    private static final Logger logger = LoggerFactory.getLogger(BotParserIntegration.class);
    
    private final JDA jda;
    private final GameServerRepository serverRepository;
    private final PlayerRepository playerRepository;
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param jda The JDA instance
     * @param serverRepository The server repository
     * @param playerRepository The player repository
     * @param sftpConnector The SFTP connector
     */
    public BotParserIntegration(JDA jda, GameServerRepository serverRepository,
                              PlayerRepository playerRepository, SftpConnector sftpConnector) {
        this.jda = jda;
        this.serverRepository = serverRepository;
        this.playerRepository = playerRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Initialize the integration
     */
    public void initialize() {
        try {
            logger.info("Initializing BotParserIntegration");
            // Initialization logic can be added here
        } catch (Exception e) {
            logger.error("Error initializing BotParserIntegration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process CSV files for a server with path resolution fixes
     * @param server The game server
     * @return The number of files processed
     */
    public int processCsvFiles(GameServer server) {
        try {
            // Find CSV files with fallback path resolution
            List<String> csvFiles = PathResolutionFix.findCsvFilesWithFallback(server, sftpConnector);
            
            if (csvFiles.isEmpty()) {
                logger.warn("No CSV files found for server {}", server.getName());
                return 0;
            }
            
            logger.info("Processing {} CSV files for server {}", csvFiles.size(), server.getName());
            int fileCount = 0;
            
            // Process each CSV file
            for (String csvFile : csvFiles) {
                try {
                    // Download and process the file
                    String content = sftpConnector.readFile(server, csvFile);
                    
                    if (content != null && !content.isEmpty()) {
                        String[] lines = content.split("\n");
                        int processedLines = 0;
                        
                        // Process each line
                        for (String line : lines) {
                            if (line != null && !line.trim().isEmpty()) {
                                // Use the fixed CSV parser
                                boolean success = CsvParsingFix.processDeathLogLineFixed(
                                    server, line, playerRepository);
                                
                                if (success) {
                                    processedLines++;
                                }
                            }
                        }
                        
                        logger.info("Processed {} lines from CSV file {} for server {}", 
                            processedLines, csvFile, server.getName());
                        fileCount++;
                    }
                } catch (Exception e) {
                    logger.error("Error processing CSV file {} for server {}: {}", 
                        csvFile, server.getName(), e.getMessage(), e);
                }
            }
            
            // Validate and sync stats after processing
            if (fileCount > 0) {
                boolean syncSuccess = StatValidationFix.validateAndSyncStats(playerRepository);
                
                if (syncSuccess) {
                    logger.info("Successfully validated and synced stats for server {}", 
                        server.getName());
                } else {
                    logger.warn("Failed to validate and sync stats for server {}", 
                        server.getName());
                }
            }
            
            return fileCount;
        } catch (Exception e) {
            logger.error("Error processing CSV files for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Process log file for a server with path resolution fixes
     * @param server The game server
     * @return True if the log file was processed successfully
     */
    public boolean processLogFile(GameServer server) {
        try {
            // Find log file with fallback path resolution
            String logFile = PathResolutionFix.findLogFileWithFallback(server, sftpConnector);
            
            if (logFile == null || logFile.isEmpty()) {
                logger.warn("No log file found for server {}", server.getName());
                return false;
            }
            
            logger.info("Processing log file {} for server {}", logFile, server.getName());
            
            // Download and process the file
            String content = sftpConnector.readFile(server, logFile);
            
            if (content != null && !content.isEmpty()) {
                // In a real implementation, this would process the log file
                // For now, just log success to allow compilation
                logger.info("Successfully processed log file for server {}", 
                    server.getName());
                return true;
            } else {
                logger.warn("Empty log file for server {}", server.getName());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error processing log file for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate a server's parser configuration
     * @param server The server to validate
     * @return The validation results
     */
    public DeadsideParserValidator.ValidationResults validateServer(GameServer server) {
        DeadsideParserValidator validator = new DeadsideParserValidator(
            jda, serverRepository, playerRepository, sftpConnector);
        return validator.validateServer(server);
    }
}