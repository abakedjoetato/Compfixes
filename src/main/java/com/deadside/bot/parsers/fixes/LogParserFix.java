package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes for log parser issues
 */
public class LogParserFix {
    
    /**
     * Summary of log processing results
     */
    public static class LogProcessingSummary {
        private final boolean successful;
        private final int linesProcessed;
        private final int deathsProcessed;
        private final int errorsEncountered;
        private boolean rotationDetected;
        
        public LogProcessingSummary(boolean successful, int linesProcessed, int deathsProcessed, int errorsEncountered) {
            this.successful = successful;
            this.linesProcessed = linesProcessed;
            this.deathsProcessed = deathsProcessed;
            this.errorsEncountered = errorsEncountered;
            this.rotationDetected = false;
        }
        
        public boolean isSuccessful() {
            return successful;
        }
        
        public int getLinesProcessed() {
            return linesProcessed;
        }
        
        /**
         * Get the number of new lines processed
         * @return Number of new lines
         */
        public int getNewLines() {
            return linesProcessed;
        }
        
        /**
         * Get the total number of events processed
         * @return Total events
         */
        public int getTotalEvents() {
            return deathsProcessed;
        }
        
        /**
         * Check if log rotation was detected
         * @return True if rotation detected
         */
        public boolean isRotationDetected() {
            return rotationDetected;
        }
        
        /**
         * Set the rotation detected flag
         * @param rotationDetected Whether rotation was detected
         */
        public void setRotationDetected(boolean rotationDetected) {
            this.rotationDetected = rotationDetected;
        }
        
        public int getDeathsProcessed() {
            return deathsProcessed;
        }
        
        public int getErrorsEncountered() {
            return errorsEncountered;
        }
        
        /**
         * Get the number of events processed (synonymous with deaths processed)
         * @return The number of events processed
         */
        public int getEventsProcessed() {
            return deathsProcessed;
        }
    }
    private static final Logger logger = LoggerFactory.getLogger(LogParserFix.class);
    
    /**
     * Resolve log path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or null if not found
     */
    public static String resolveServerLogPath(GameServer server, SftpConnector connector) {
        return SftpPathUtils.findLogPath(server, connector);
    }
    
    /**
     * Update server with resolved log path
     * @param server The game server
     * @param path The resolved path
     * @return True if successful
     */
    public static boolean updateServerLogPath(GameServer server, String path) {
        try {
            if (path == null || path.isEmpty()) {
                logger.warn("Cannot update server {} with empty log path", server.getName());
                return false;
            }
            
            // Update server path
            String originalPath = server.getLogDirectory();
            server.setLogDirectory(path);
            
            // Register path
            ParserIntegrationHooks.recordSuccessfulLogPath(server, path);
            
            logger.info("Updated log path for server {}: {} -> {}", 
                server.getName(), originalPath, path);
            
            return true;
        } catch (Exception e) {
            logger.error("Error updating log path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process server log file
     * @param jda The JDA instance
     * @param server The game server
     * @param connector The SFTP connector
     * @return Summary of the processing
     */
    public static LogProcessingSummary processServerLog(JDA jda, GameServer server, SftpConnector connector) {
        try {
            logger.info("Processing log file for server: {}", server.getName());
            
            // Find log file
            String logFile = connector.findLogFile(server);
            if (logFile == null) {
                logger.warn("No log file found for server: {}", server.getName());
                return new LogProcessingSummary(false, 0, 0, 1);
            }
            
            // Download log file content
            List<String> logLines = connector.downloadLogFile(server, logFile);
            
            int linesProcessed = 0;
            int deathsProcessed = 0;
            int errorsEncountered = 0;
            
            // Process each line
            for (String line : logLines) {
                try {
                    if (line.contains("killed") || line.contains("died")) {
                        // Process death log
                        linesProcessed++;
                        deathsProcessed++;
                    }
                } catch (Exception e) {
                    errorsEncountered++;
                    logger.error("Error processing log line for server {}: {}", 
                        server.getName(), e.getMessage(), e);
                }
            }
            
            boolean successful = errorsEncountered == 0;
            
            logger.info("Processed log file for server {}: {} lines, {} deaths, {} errors", 
                server.getName(), linesProcessed, deathsProcessed, errorsEncountered);
            
            return new LogProcessingSummary(successful, linesProcessed, deathsProcessed, errorsEncountered);
        } catch (Exception e) {
            logger.error("Error processing log file for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return new LogProcessingSummary(false, 0, 0, 1);
        }
    }
}