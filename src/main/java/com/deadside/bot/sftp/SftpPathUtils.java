package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.parsers.fixes.CsvParsingFix;
import com.deadside.bot.parsers.fixes.LogParserFix;
import com.deadside.bot.parsers.fixes.ParserPathRepairHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utilities for SFTP path operations
 * This class provides utilities for enhancing SFTP path operations
 */
public class SftpPathUtils {
    private static final Logger logger = LoggerFactory.getLogger(SftpPathUtils.class);
    
    /**
     * Get the deathlog CSV directory for a server with path resolution
     * @param server The game server
     * @param sftpConnector The SFTP connector
     * @return The resolved path or the original if resolution failed
     */
    public static String getResolvedDeathlogDirectory(GameServer server, SftpConnector sftpConnector) {
        if (server == null) {
            return null;
        }
        
        try {
            String originalPath = server.getDeathlogsDirectory();
            
            // Try to resolve the path using the hook
            String resolvedPath = ParserPathRepairHook.getCsvPathHook(server, originalPath);
            
            if (resolvedPath != null && !resolvedPath.equals(originalPath)) {
                logger.debug("Resolved CSV path: {} -> {}", originalPath, resolvedPath);
                return resolvedPath;
            }
            
            return originalPath;
        } catch (Exception e) {
            logger.error("Error resolving deathlog directory for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getDeathlogsDirectory();
        }
    }
    
    /**
     * Get the log directory for a server with path resolution
     * @param server The game server
     * @param sftpConnector The SFTP connector
     * @return The resolved path or the original if resolution failed
     */
    public static String getResolvedLogDirectory(GameServer server, SftpConnector sftpConnector) {
        if (server == null) {
            return null;
        }
        
        try {
            String originalPath = server.getLogDirectory();
            
            // Try to resolve the path using the hook
            String resolvedPath = ParserPathRepairHook.getLogPathHook(server, originalPath);
            
            if (resolvedPath != null && !resolvedPath.equals(originalPath)) {
                logger.debug("Resolved log path: {} -> {}", originalPath, resolvedPath);
                return resolvedPath;
            }
            
            return originalPath;
        } catch (Exception e) {
            logger.error("Error resolving log directory for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return server.getLogDirectory();
        }
    }
    
    /**
     * Find deathlog files for a server with path resolution
     * @param server The game server
     * @param sftpConnector The SFTP connector
     * @return List of deathlog files or empty list if none found
     */
    public static List<String> findDeathlogFilesWithResolution(GameServer server, SftpConnector sftpConnector) {
        if (server == null || sftpConnector == null) {
            return java.util.Collections.emptyList();
        }
        
        try {
            // Resolve the path first
            String resolvedPath = getResolvedDeathlogDirectory(server, sftpConnector);
            
            // Try to find files using the resolved path
            List<String> files = sftpConnector.findDeathlogFiles(server);
            
            if (files != null && !files.isEmpty()) {
                // Record the successful path
                ParserPathRepairHook.recordSuccessfulCsvPath(server, resolvedPath);
                return files;
            }
            
            // If no files found, try to find them directly using the CsvParsingFix
            String fixedPath = CsvParsingFix.resolveServerCsvPath(server, sftpConnector);
            if (fixedPath != null && !fixedPath.equals(resolvedPath)) {
                // Try again with the fixed path
                server.setDeathlogsDirectory(fixedPath);
                return sftpConnector.findDeathlogFiles(server);
            }
            
            return files;
        } catch (Exception e) {
            logger.error("Error finding deathlog files with resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Find log file for a server with path resolution
     * @param server The game server
     * @param sftpConnector The SFTP connector
     * @return The log file path or null if not found
     */
    public static String findLogFileWithResolution(GameServer server, SftpConnector sftpConnector) {
        if (server == null || sftpConnector == null) {
            return null;
        }
        
        try {
            // Resolve the path first
            String resolvedPath = getResolvedLogDirectory(server, sftpConnector);
            
            // Try to find the log file using the resolved path
            String logFile = sftpConnector.findLogFile(server);
            
            if (logFile != null && !logFile.isEmpty()) {
                // Record the successful path
                ParserPathRepairHook.recordSuccessfulLogPath(server, resolvedPath);
                return logFile;
            }
            
            // If no log file found, try to find it directly using the LogParserFix
            String fixedPath = LogParserFix.resolveServerLogPath(server, sftpConnector);
            if (fixedPath != null && !fixedPath.equals(resolvedPath)) {
                // Try again with the fixed path
                server.setLogDirectory(fixedPath);
                return sftpConnector.findLogFile(server);
            }
            
            return logFile;
        } catch (Exception e) {
            logger.error("Error finding log file with resolution for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
}