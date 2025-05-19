package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes for log file parsing
 */
public class LogParserFix {
    private static final Logger logger = LoggerFactory.getLogger(LogParserFix.class);
    
    /**
     * Process a server log file
     * @param logContent The log file content
     * @param server The game server
     * @return Summary of processing
     */
    public static LogProcessingSummary processServerLog(String logContent, GameServer server) {
        LogProcessingSummary summary = new LogProcessingSummary();
        
        try {
            if (logContent == null || logContent.isEmpty()) {
                logger.warn("Empty log content for server {}", server.getName());
                return summary;
            }
            
            // Split into lines
            String[] lines = logContent.split("\n");
            List<String> newLines = new ArrayList<>();
            
            // Detect log rotation
            boolean rotationDetected = detectLogRotation(lines);
            summary.setRotationDetected(rotationDetected);
            
            // Process each line
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                
                // Process the line (in a real implementation)
                // Here we just count the lines and events
                newLines.add(line);
                
                // Count events based on log patterns
                if (line.contains("Server started") || 
                    line.contains("Player connected") || 
                    line.contains("Player disconnected")) {
                    summary.incrementTotalEvents();
                }
            }
            
            summary.setNewLines(newLines);
            logger.info("Processed {} lines and found {} events in log for server {}", 
                newLines.size(), summary.getTotalEvents(), server.getName());
            
            return summary;
        } catch (Exception e) {
            logger.error("Error processing server log: {}", e.getMessage(), e);
            return summary;
        }
    }
    
    /**
     * Detect log rotation in server logs
     * @param lines The log lines
     * @return True if rotation detected
     */
    private static boolean detectLogRotation(String[] lines) {
        if (lines == null || lines.length < 2) {
            return false;
        }
        
        // Check for timestamp reversal, which indicates rotation
        String firstLine = lines[0];
        String lastLine = lines[lines.length - 1];
        
        // Extract timestamps (this is a simplified example)
        String firstTimestamp = extractTimestamp(firstLine);
        String lastTimestamp = extractTimestamp(lastLine);
        
        if (firstTimestamp != null && lastTimestamp != null) {
            // Compare timestamps - if first is after last, rotation detected
            try {
                return firstTimestamp.compareTo(lastTimestamp) > 0;
            } catch (Exception e) {
                logger.debug("Error comparing log timestamps: {}", e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * Extract timestamp from a log line
     * @param line The log line
     * @return The timestamp
     */
    private static String extractTimestamp(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        
        // This is a simplified example - actual implementation would depend on log format
        // Assuming format like "[2025-05-19 08:30:45]"
        try {
            int startBracket = line.indexOf('[');
            int endBracket = line.indexOf(']');
            
            if (startBracket >= 0 && endBracket > startBracket) {
                return line.substring(startBracket + 1, endBracket);
            }
        } catch (Exception e) {
            logger.debug("Error extracting timestamp: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Summary of log processing
     */
    public static class LogProcessingSummary {
        private List<String> newLines = new ArrayList<>();
        private int totalEvents = 0;
        private boolean rotationDetected = false;
        
        public List<String> getNewLines() {
            return newLines;
        }
        
        public void setNewLines(List<String> newLines) {
            this.newLines = newLines;
        }
        
        public int getTotalEvents() {
            return totalEvents;
        }
        
        public void setTotalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
        }
        
        public void incrementTotalEvents() {
            totalEvents++;
        }
        
        public boolean isRotationDetected() {
            return rotationDetected;
        }
        
        public void setRotationDetected(boolean rotationDetected) {
            this.rotationDetected = rotationDetected;
        }
    }
}