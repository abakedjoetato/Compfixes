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
 * Validator for Deadside server parsers
 * This class helps diagnose and validate parser path configurations
 */
public class DeadsideParserValidator {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserValidator.class);
    
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
    public DeadsideParserValidator(JDA jda, GameServerRepository serverRepository, 
                                 PlayerRepository playerRepository, SftpConnector sftpConnector) {
        this.jda = jda;
        this.serverRepository = serverRepository;
        this.playerRepository = playerRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Validate a server configuration
     * @param server The server to validate
     * @return Validation results
     */
    public ValidationResults validateServer(GameServer server) {
        ValidationResults results = new ValidationResults();
        
        try {
            // Basic connection test
            boolean connectionOk = sftpConnector.testConnection(server);
            results.setConnectionStatus(connectionOk);
            
            if (!connectionOk) {
                logger.warn("Connection test failed for server {}", server.getName());
                results.addIssue("Connection test failed");
                return results;
            }
            
            // CSV file discovery test
            List<String> csvFiles = PathResolutionFix.findCsvFilesWithFallback(server, sftpConnector);
            
            if (csvFiles.isEmpty()) {
                logger.warn("No CSV files found for server {}", server.getName());
                results.addIssue("No CSV files found");
                results.setCsvFilesFound(false);
            } else {
                logger.info("Found {} CSV files for server {}", csvFiles.size(), server.getName());
                results.setCsvFilesFound(true);
                results.setCsvFileCount(csvFiles.size());
            }
            
            // Log file discovery test
            String logFile = PathResolutionFix.findLogFileWithFallback(server, sftpConnector);
            
            if (logFile == null || logFile.isEmpty()) {
                logger.warn("No log file found for server {}", server.getName());
                results.addIssue("No log file found");
                results.setLogFileFound(false);
            } else {
                logger.info("Found log file for server {}: {}", server.getName(), logFile);
                results.setLogFileFound(true);
            }
            
            // Overall validity
            boolean isValid = connectionOk && results.isCsvFilesFound() && results.isLogFileFound();
            results.setIsValid(isValid);
            
            return results;
        } catch (Exception e) {
            logger.error("Error validating server {}: {}", server.getName(), e.getMessage(), e);
            results.addIssue("Validation error: " + e.getMessage());
            results.setIsValid(false);
            return results;
        }
    }
    
    /**
     * Validation results class
     */
    public static class ValidationResults {
        private boolean isValid;
        private boolean connectionStatus;
        private boolean csvFilesFound;
        private boolean logFileFound;
        private int csvFileCount;
        private final List<String> issues = new java.util.ArrayList<>();
        
        public boolean isValid() {
            return isValid;
        }
        
        public void setIsValid(boolean isValid) {
            this.isValid = isValid;
        }
        
        public boolean isConnectionStatus() {
            return connectionStatus;
        }
        
        public void setConnectionStatus(boolean connectionStatus) {
            this.connectionStatus = connectionStatus;
        }
        
        public boolean isCsvFilesFound() {
            return csvFilesFound;
        }
        
        public void setCsvFilesFound(boolean csvFilesFound) {
            this.csvFilesFound = csvFilesFound;
        }
        
        public boolean isLogFileFound() {
            return logFileFound;
        }
        
        public void setLogFileFound(boolean logFileFound) {
            this.logFileFound = logFileFound;
        }
        
        public int getCsvFileCount() {
            return csvFileCount;
        }
        
        public void setCsvFileCount(int csvFileCount) {
            this.csvFileCount = csvFileCount;
        }
        
        public List<String> getIssues() {
            return new java.util.ArrayList<>(issues);
        }
        
        public void addIssue(String issue) {
            issues.add(issue);
        }
    }
}