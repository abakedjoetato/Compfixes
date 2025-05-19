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
            
            if (fileList == null || fileList.isEmpty()) {
                return files;
            }
            
            // Keep track of subdirectories
            List<String> subdirectories = new ArrayList<>();
            
            // Check for CSV files and subdirectories
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if it's a directory
                if (entry.getAttrs().isDir()) {
                    // Add to subdirectories
                    subdirectories.add(path + "/" + filename);
                } 
                // Check if it's a CSV file
                else if (filename.endsWith(".csv")) {
                    files.add(path + "/" + filename);
                }
            }
            
            // If this path has actual/deathlogs or actual1/deathlogs in its name
            // and contains CSV files, record it as a successful path
            if (!files.isEmpty() && (path.contains("actual/deathlogs") || path.contains("actual1/deathlogs"))) {
                // Record successful path
                com.deadside.bot.parsers.fixes.ParserIntegrationHooks.recordSuccessfulCsvPath(server, path);
            }
            
            // Check subdirectories recursively
            for (String subdirectory : subdirectories) {
                List<String> subdirectoryFiles = findCsvFilesRecursively(server, subdirectory, depth + 1);
                files.addAll(subdirectoryFiles);
                
                // Stop if we have found enough files
                if (files.size() > MAX_FILES_TO_CHECK) {
                    break;
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
            
            if (fileList == null || fileList.isEmpty()) {
                return null;
            }
            
            // Keep track of subdirectories
            List<String> subdirectories = new ArrayList<>();
            
            // Check for the log file and subdirectories
            for (ChannelSftp.LsEntry entry : fileList) {
                String filename = entry.getFilename();
                
                // Skip . and ..
                if (filename.equals(".") || filename.equals("..")) {
                    continue;
                }
                
                // Check if it's a directory
                if (entry.getAttrs().isDir()) {
                    // Add to subdirectories
                    subdirectories.add(path + "/" + filename);
                } 
                // Check if it's the log file
                else if (filename.equals("Deadside.log")) {
                    // If this path has Logs in its name and contains the log file,
                    // record it as a successful path
                    if (path.contains("Logs")) {
                        // Record successful path
                        com.deadside.bot.parsers.fixes.ParserIntegrationHooks.recordSuccessfulLogPath(server, path);
                    }
                    
                    return path + "/Deadside.log";
                }
            }
            
            // Check subdirectories recursively
            for (String subdirectory : subdirectories) {
                String logFile = findLogFileRecursively(server, subdirectory, depth + 1);
                if (logFile != null) {
                    return logFile;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error finding log file recursively in path {} for server {}: {}", 
                path, server.getName(), e.getMessage());
            return null;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
    
    /**
     * Test if a path exists and is accessible
     * @param server The game server
     * @param path The path to test
     * @return True if the path exists and is accessible
     */
    @Override
    public boolean testConnection(GameServer server, String path) {
        if (server == null || path == null || path.isEmpty()) {
            return false;
        }
        
        // Try the standard method first
        boolean standardResult = super.testConnection(server, path);
        
        if (standardResult) {
            return true;
        }
        
        // If standard method failed, try with our enhanced method
        Session session = null;
        ChannelSftp channel = null;
        
        try {
            session = connect(server);
            channel = openChannel(session);
            
            // Try to navigate to the path
            channel.cd(path);
            
            return true;
        } catch (Exception e) {
            // Path doesn't exist or error occurred
            return false;
        } finally {
            closeChannel(channel);
            disconnect(session);
        }
    }
}