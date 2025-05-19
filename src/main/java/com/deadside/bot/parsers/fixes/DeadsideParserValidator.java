package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

/**
 * Validator for Deadside parser paths
 * This class provides methods for validating paths for both CSV and Log files
 */
public class DeadsideParserValidator {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserValidator.class);
    
    // File suffixes to check for
    private static final String CSV_FILE_SUFFIX = ".csv";
    private static final String LOG_FILE_NAME = "Deadside.log";
    
    // Maximum number of files to check
    private static final int MAX_FILES_TO_CHECK = 100;
    
    // SFTP connector
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param sftpConnector The SFTP connector to use
     */
    public DeadsideParserValidator(SftpConnector sftpConnector) {
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Validate a CSV path for a server
     * @param server The game server
     * @param path The path to validate
     * @return True if the path is valid and contains CSV files
     */
    public boolean validateCsvPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return false;
        }
        
        try {
            if (!connectAndValidate(server, path, CSV_FILE_SUFFIX, true)) {
                return false;
            }
            
            // If we got here, the path is valid and contains CSV files
            logger.debug("Valid CSV path found for server {}: {}", server.getName(), path);
            
            // Record the successful path
            ParserPathTracker.getInstance().recordSuccessfulPath(
                server, ParserPathTracker.CATEGORY_CSV, path);
            
            return true;
        } catch (Exception e) {
            logger.debug("Error validating CSV path for server {}: {}", 
                server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate a Log path for a server
     * @param server The game server
     * @param path The path to validate
     * @return True if the path is valid and contains the Deadside.log file
     */
    public boolean validateLogPath(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return false;
        }
        
        try {
            if (!connectAndValidate(server, path, LOG_FILE_NAME, false)) {
                return false;
            }
            
            // If we got here, the path is valid and contains the Deadside.log file
            logger.debug("Valid Log path found for server {}: {}", server.getName(), path);
            
            // Record the successful path
            ParserPathTracker.getInstance().recordSuccessfulPath(
                server, ParserPathTracker.CATEGORY_LOG, path);
            
            return true;
        } catch (Exception e) {
            logger.debug("Error validating Log path for server {}: {}", 
                server.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Connect to a server and validate a path
     * @param server The game server
     * @param path The path to validate
     * @param filePattern The file pattern to look for
     * @param isSuffix Whether the pattern is a suffix
     * @return True if the path is valid and contains matching files
     */
    private boolean connectAndValidate(GameServer server, String path, 
                                    String filePattern, boolean isSuffix) {
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            // Connect to the server
            session = sftpConnector.connect(server);
            channel = sftpConnector.openChannel(session);
            
            // Navigate to the path
            channel.cd(path);
            
            // Get the file list
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = channel.ls(".");
            
            if (fileList == null || fileList.isEmpty()) {
                logger.debug("No files found in path {} for server {}", 
                    path, server.getName());
                return false;
            }
            
            // Check for matching files
            boolean foundMatchingFile = false;
            int filesChecked = 0;
            
            for (ChannelSftp.LsEntry entry : fileList) {
                if (filesChecked >= MAX_FILES_TO_CHECK) {
                    break;
                }
                
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if the file matches the pattern
                if (isSuffix) {
                    // Check if the file ends with the pattern
                    if (filename.endsWith(filePattern)) {
                        foundMatchingFile = true;
                        break;
                    }
                } else {
                    // Check if the file matches the pattern exactly
                    if (filename.equals(filePattern)) {
                        foundMatchingFile = true;
                        break;
                    }
                }
                
                filesChecked++;
            }
            
            if (!foundMatchingFile) {
                logger.debug("No matching files found in path {} for server {}", 
                    path, server.getName());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.debug("Error connecting to or validating path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return false;
        } finally {
            // Close the channel and session
            sftpConnector.closeChannel(channel);
            sftpConnector.disconnect(session);
        }
    }
    
    /**
     * Test if a server path exists
     * @param server The game server
     * @param path The path to test
     * @return True if the path exists
     */
    public boolean testPathExists(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return false;
        }
        
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            // Connect to the server
            session = sftpConnector.connect(server);
            channel = sftpConnector.openChannel(session);
            
            // Try to navigate to the path
            channel.cd(path);
            
            // If we got here, the path exists
            return true;
        } catch (Exception e) {
            // Path doesn't exist or error occurred
            return false;
        } finally {
            // Close the channel and session
            sftpConnector.closeChannel(channel);
            sftpConnector.disconnect(session);
        }
    }
}