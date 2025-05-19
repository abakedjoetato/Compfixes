package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Enhanced SFTP connector with improved directory traversal and file discovery
 * This class extends the standard SftpConnector with additional capabilities
 * for directory traversal and file discovery
 */
public class EnhancedSftpConnector extends SftpConnector {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedSftpConnector.class);
    
    // Maximum depth for recursive directory traversal
    private static final int MAX_RECURSION_DEPTH = 3;
    
    // Maximum number of files to check
    private static final int MAX_FILES_TO_CHECK = 500;
    
    /**
     * Connect to an SFTP server using the game server configuration
     * @param server The game server
     * @return The session
     */
    protected Session connect(GameServer server) throws Exception {
        SftpConnection connection = super.connect(server);
        return connection.getSession();
    }
    
    /**
     * Open an SFTP channel from a session
     * @param session The session
     * @return The channel
     */
    protected ChannelSftp openChannel(Session session) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new Exception("Cannot open SFTP channel: No active session");
        }
        
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        return channel;
    }
    
    /**
     * Close an SFTP channel safely
     * @param channel The channel to close
     */
    protected void closeChannel(ChannelSftp channel) {
        SftpUtils.closeChannel(channel);
    }
    
    /**
     * Disconnect an SSH session safely
     * @param session The session to disconnect
     */
    protected void disconnect(Session session) {
        SftpUtils.disconnect(session);
    }
    
    /**
     * Find CSV files in the deathlogs directory and its parent directories
     * This method adds improved path resolution logic to the standard method
     * @param server The game server
     * @return List of CSV files
     */
    @Override
    public List<String> findDeathlogFiles(GameServer server) {
        try {
            List<String> result = new ArrayList<>();
            String path = server.getDeathlogsDirectory();
            
            if (path == null || path.isEmpty()) {
                logger.warn("Empty deathlogs directory for server {}", server.getName());
                return result;
            }
            
            // Try the standard path first
            List<String> standardResult = super.findDeathlogFiles(server);
            if (standardResult != null && !standardResult.isEmpty()) {
                // Record successful path
                com.deadside.bot.parsers.fixes.ParserIntegrationHooks.recordSuccessfulCsvPath(server, path);
                return standardResult;
            }
            
            // If standard path failed, try alternative paths
            List<String> alternativePaths = getAlternativeCsvPaths(server);
            
            for (String alternativePath : alternativePaths) {
                try {
                    logger.debug("Trying alternative CSV path for server {}: {}", 
                        server.getName(), alternativePath);
                    
                    List<String> files = findCsvFilesInPath(server, alternativePath);
                    
                    if (files != null && !files.isEmpty()) {
                        logger.info("Found CSV files in alternative path {} for server {}", 
                            alternativePath, server.getName());
                        
                        // Record successful path
                        com.deadside.bot.parsers.fixes.ParserIntegrationHooks.recordSuccessfulCsvPath(
                            server, alternativePath);
                        
                        return files;
                    }
                } catch (Exception e) {
                    logger.debug("Error trying alternative CSV path {} for server {}: {}", 
                        alternativePath, server.getName(), e.getMessage());
                }
            }
            
            // If all alternatives failed, try recursive search from root path
            String rootPath = getRootPath(server);
            
            if (rootPath != null && !rootPath.isEmpty()) {
                try {
                    logger.debug("Trying recursive CSV file search from root path {} for server {}", 
                        rootPath, server.getName());
                    
                    List<String> files = findCsvFilesRecursively(server, rootPath, 0);
                    
                    if (files != null && !files.isEmpty()) {
                        logger.info("Found CSV files in recursive search from root path {} for server {}", 
                            rootPath, server.getName());
                        
                        return files;
                    }
                } catch (Exception e) {
                    logger.debug("Error in recursive CSV file search from root path {} for server {}: {}", 
                        rootPath, server.getName(), e.getMessage(), e);
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error finding deathlog files for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find the Deadside.log file in the logs directory and its parent directories
     * This method adds improved path resolution logic to the standard method
     * @param server The game server
     * @return The log file if found, null otherwise
     */
    @Override
    public String findLogFile(GameServer server) {
        try {
            String path = server.getLogDirectory();
            
            if (path == null || path.isEmpty()) {
                logger.warn("Empty log directory for server {}", server.getName());
                return null;
            }
            
            // Try the standard path first
            String standardResult = super.findLogFile(server);
            if (standardResult != null && !standardResult.isEmpty()) {
                // Record successful path
                com.deadside.bot.parsers.fixes.ParserIntegrationHooks.recordSuccessfulLogPath(server, path);
                return standardResult;
            }
            
            // If standard path failed, try alternative paths
            List<String> alternativePaths = getAlternativeLogPaths(server);
            
            for (String alternativePath : alternativePaths) {
                try {
                    logger.debug("Trying alternative Log path for server {}: {}", 
                        server.getName(), alternativePath);
                    
                    String logFile = findLogFileInPath(server, alternativePath);
                    
                    if (logFile != null && !logFile.isEmpty()) {
                        logger.info("Found log file in alternative path {} for server {}", 
                            alternativePath, server.getName());
                        
                        // Record successful path
                        com.deadside.bot.parsers.fixes.ParserIntegrationHooks.recordSuccessfulLogPath(
                            server, alternativePath);
                        
                        return logFile;
                    }
                } catch (Exception e) {
                    logger.debug("Error trying alternative Log path {} for server {}: {}", 
                        alternativePath, server.getName(), e.getMessage());
                }
            }
            
            // If all alternatives failed, try recursive search from root path
            String rootPath = getRootPath(server);
            
            if (rootPath != null && !rootPath.isEmpty()) {
                try {
                    logger.debug("Trying recursive Log file search from root path {} for server {}", 
                        rootPath, server.getName());
                    
                    String logFile = findLogFileRecursively(server, rootPath, 0);
                    
                    if (logFile != null && !logFile.isEmpty()) {
                        logger.info("Found Log file in recursive search from root path {} for server {}", 
                            rootPath, server.getName());
                        
                        return logFile;
                    }
                } catch (Exception e) {
                    logger.debug("Error in recursive Log file search from root path {} for server {}: {}", 
                        rootPath, server.getName(), e.getMessage(), e);
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error finding log file for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get alternative CSV paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private List<String> getAlternativeCsvPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
        // Get server properties
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Add alternative paths
        paths.add(host + "_" + serverName + "/actual1/deathlogs");
        paths.add(host + "_" + serverName + "/actual/deathlogs");
        paths.add(host + "/" + serverName + "/actual1/deathlogs");
        paths.add(host + "/" + serverName + "/actual/deathlogs");
        paths.add(serverName + "/actual1/deathlogs");
        paths.add(serverName + "/actual/deathlogs");
        
        // Check for recommended paths
        try {
            List<String> recommendedPaths = 
                com.deadside.bot.parsers.fixes.ParserPathTracker.getInstance()
                    .getRecommendedPaths(server, com.deadside.bot.parsers.fixes.ParserPathTracker.CATEGORY_CSV);
            
            if (recommendedPaths != null) {
                for (String path : recommendedPaths) {
                    if (!paths.contains(path)) {
                        paths.add(path);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting recommended CSV paths: {}", e.getMessage());
        }
        
        return paths;
    }
    
    /**
     * Get alternative Log paths for a server
     * @param server The game server
     * @return List of alternative paths
     */
    private List<String> getAlternativeLogPaths(GameServer server) {
        List<String> paths = new ArrayList<>();
        
        // Get server properties
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Add alternative paths
        paths.add(host + "_" + serverName + "/Logs");
        paths.add(host + "_" + serverName + "/Deadside/Logs");
        paths.add(host + "/" + serverName + "/Logs");
        paths.add(host + "/" + serverName + "/Deadside/Logs");
        paths.add(serverName + "/Logs");
        paths.add(serverName + "/Deadside/Logs");
        
        // Check for recommended paths
        try {
            List<String> recommendedPaths = 
                com.deadside.bot.parsers.fixes.ParserPathTracker.getInstance()
                    .getRecommendedPaths(server, com.deadside.bot.parsers.fixes.ParserPathTracker.CATEGORY_LOG);
            
            if (recommendedPaths != null) {
                for (String path : recommendedPaths) {
                    if (!paths.contains(path)) {
                        paths.add(path);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting recommended Log paths: {}", e.getMessage());
        }
        
        return paths;
    }
    
    /**
     * Get the root path for a server
     * @param server The game server
     * @return The root path
     */
    private String getRootPath(GameServer server) {
        // Get server properties
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Try to construct a root path
        return host + "_" + serverName;
    }
    
    /**
     * Find CSV files in a specified path
     * @param server The game server
     * @param path The path to search
     * @return List of CSV files
     */
    private List<String> findCsvFilesInPath(GameServer server, String path) {
        List<String> files = new ArrayList<>();
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            session = connect(server);
            channel = openChannel(session);
            
            // Try to navigate to the path
            channel.cd(path);
            
            // Get the file list
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = channel.ls(".");
            
            if (fileList == null || fileList.isEmpty()) {
                return files;
            }
            
            // Check for CSV files
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if it's a CSV file
                if (filename.endsWith(".csv")) {
                    files.add(path + "/" + filename);
                }
            }
            
            return files;
        } catch (Exception e) {
            logger.debug("Error finding CSV files in path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return files;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
    
    /**
     * Find the log file in a specified path
     * @param server The game server
     * @param path The path to search
     * @return The log file if found, null otherwise
     */
    private String findLogFileInPath(GameServer server, String path) {
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            session = connect(server);
            channel = openChannel(session);
            
            // Try to navigate to the path
            channel.cd(path);
            
            // Get the file list
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = channel.ls(".");
            
            if (fileList == null || fileList.isEmpty()) {
                return null;
            }
            
            // Check for the log file
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if it's the log file
                if (filename.equals("Deadside.log")) {
                    return path + "/Deadside.log";
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error finding log file in path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return null;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
    
    /**
     * Find CSV files recursively from a root path
     * @param server The game server
     * @param path The path to search
     * @param depth The current recursion depth
     * @return List of CSV files
     */
    private List<String> findCsvFilesRecursively(GameServer server, String path, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return Collections.emptyList();
        }
        
        List<String> files = new ArrayList<>();
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            session = connect(server);
            channel = openChannel(session);
            
            // Try to navigate to the path
            channel.cd(path);
            
            // Get the file list
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = channel.ls(".");
            
            if (fileList == null) {
                return files;
            }
            
            // First check for CSV files in the current directory
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if it's a CSV file
                if (filename.endsWith(".csv")) {
                    files.add(path + "/" + filename);
                }
                
                // If we found files, stop searching
                if (files.size() >= MAX_FILES_TO_CHECK) {
                    break;
                }
            }
            
            // If we found files, return them
            if (!files.isEmpty()) {
                return files;
            }
            
            // If we didn't find any files, recursively check subdirectories
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // If it's a directory, recursively search it
                if (entry.getAttrs().isDir()) {
                    String subPath = path + "/" + filename;
                    
                    try {
                        List<String> subFiles = findCsvFilesRecursively(server, subPath, depth + 1);
                        
                        if (subFiles != null && !subFiles.isEmpty()) {
                            files.addAll(subFiles);
                            
                            // If we found enough files, stop searching
                            if (files.size() >= MAX_FILES_TO_CHECK) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Error searching directory {} for CSV files: {}", 
                            subPath, e.getMessage());
                    }
                }
            }
            
            return files;
        } catch (Exception e) {
            logger.debug("Error finding CSV files recursively in path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return files;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
    
    /**
     * Find the log file recursively from a root path
     * @param server The game server
     * @param path The path to search
     * @param depth The current recursion depth
     * @return The log file if found, null otherwise
     */
    private String findLogFileRecursively(GameServer server, String path, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            return null;
        }
        
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            session = connect(server);
            channel = openChannel(session);
            
            // Try to navigate to the path
            channel.cd(path);
            
            // Get the file list
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> fileList = channel.ls(".");
            
            if (fileList == null) {
                return null;
            }
            
            // First check for the log file in the current directory
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if it's the log file
                if (filename.equals("Deadside.log")) {
                    return path + "/Deadside.log";
                }
            }
            
            // If we didn't find the log file, recursively check subdirectories
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // If it's a directory, recursively search it
                if (entry.getAttrs().isDir()) {
                    String subPath = path + "/" + filename;
                    
                    try {
                        String logFile = findLogFileRecursively(server, subPath, depth + 1);
                        
                        if (logFile != null) {
                            return logFile;
                        }
                    } catch (Exception e) {
                        logger.debug("Error searching directory {} for Log file: {}", 
                            subPath, e.getMessage());
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error finding Log file recursively in path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return null;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
    
    /**
     * Test connection to a specific path on a server
     * @param server The game server
     * @return True if the connection is successful
     */
    @Override
    public boolean testConnection(GameServer server) {
        // First try the standard test
        boolean standardResult = super.testConnection(server);
        
        if (standardResult) {
            return true;
        }
        
        // If standard test fails, try with our enhanced method
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            session = connect(server);
            channel = openChannel(session);
            
            // Try to list the directory
            channel.cd(".");
            return true;
        } catch (Exception e) {
            logger.debug("Enhanced connection test failed for server {}: {}", 
                server.getName(), e.getMessage());
            return false;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
}