package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.sftp.SftpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Validator for Deadside parser configurations
 * This class validates server configurations for proper operation
 */
public class DeadsideParserValidator {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideParserValidator.class);
    
    private final SftpConnector connector;
    
    /**
     * Constructor
     * @param connector The SFTP connector
     */
    public DeadsideParserValidator(SftpConnector connector) {
        this.connector = connector;
    }
    
    /**
     * Validate a server configuration
     * @param server The game server to validate
     * @return True if the server is valid
     */
    public boolean validateServer(GameServer server) {
        try {
            logger.info("Validating server: {}", server.getName());
            
            // Check for required fields
            if (server.getId() == null) {
                logger.error("Server {} has no ID", server.getName());
                return false;
            }
            
            if (server.getGuildId() == null) {
                logger.error("Server {} has no guild ID", server.getName());
                return false;
            }
            
            if (server.getName() == null || server.getName().isEmpty()) {
                logger.error("Server has no name");
                return false;
            }
            
            // Get validation results
            Map<String, Object> results = validateServerPaths(server);
            
            // Check CSV path
            boolean csvPathValid = results.containsKey("csvPathValid") && (boolean)results.get("csvPathValid");
            
            // Check log path
            boolean logPathValid = results.containsKey("logPathValid") && (boolean)results.get("logPathValid");
            
            // Overall validity
            boolean valid = csvPathValid && logPathValid;
            
            if (valid) {
                logger.info("Server {} is valid", server.getName());
            } else {
                logger.warn("Server {} is invalid", server.getName());
            }
            
            return valid;
        } catch (Exception e) {
            logger.error("Error validating server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate server paths
     * @param server The game server
     * @return Validation results
     */
    public Map<String, Object> validateServerPaths(GameServer server) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Validating paths for server: {}", server.getName());
            
            // Check SFTP credentials
            boolean credentialsValid = false;
            
            if (server.getHost() == null || server.getHost().isEmpty()) {
                logger.error("Server {} has no host", server.getName());
            } else if (server.getPort() == null) {
                logger.error("Server {} has no port", server.getName());
            } else if (server.getUsername() == null || server.getUsername().isEmpty()) {
                logger.error("Server {} has no username", server.getName());
            } else if (server.getPassword() == null || server.getPassword().isEmpty()) {
                logger.error("Server {} has no password", server.getName());
            } else {
                credentialsValid = true;
            }
            
            results.put("credentialsValid", credentialsValid);
            
            if (!credentialsValid) {
                logger.error("Server {} has invalid SFTP credentials", server.getName());
                results.put("csvPathValid", false);
                results.put("logPathValid", false);
                return results;
            }
            
            // Test connection
            if (!connector.testConnection(server)) {
                logger.error("Could not connect to server {}", server.getName());
                results.put("csvPathValid", false);
                results.put("logPathValid", false);
                return results;
            }
            
            // Check CSV path
            String csvPath = server.getDeathlogsDirectory();
            boolean csvPathValid = csvPath != null && !csvPath.isEmpty();
            
            if (csvPathValid) {
                // Check if path exists
                boolean csvPathExists = connector.isValidCsvPath(server);
                results.put("csvPathExists", csvPathExists);
                
                // Validate path only if it exists
                csvPathValid = csvPathExists;
            }
            
            results.put("csvPath", csvPath);
            results.put("csvPathValid", csvPathValid);
            
            // Check log path
            String logPath = server.getLogDirectory();
            boolean logPathValid = logPath != null && !logPath.isEmpty();
            
            if (logPathValid) {
                // Check if path exists
                boolean logPathExists = connector.isValidLogPath(server);
                results.put("logPathExists", logPathExists);
                
                // Validate path only if it exists
                logPathValid = logPathExists;
            }
            
            results.put("logPath", logPath);
            results.put("logPathValid", logPathValid);
            
            // Overall validity
            results.put("pathsValid", csvPathValid && logPathValid);
            
            logger.info("Path validation for server {}: CSV path valid: {}, Log path valid: {}", 
                server.getName(), csvPathValid, logPathValid);
            
            return results;
        } catch (Exception e) {
            logger.error("Error validating paths for server {}: {}", 
                server.getName(), e.getMessage(), e);
            
            results.put("error", e.getMessage());
            results.put("csvPathValid", false);
            results.put("logPathValid", false);
            results.put("pathsValid", false);
            
            return results;
        }
    }
}