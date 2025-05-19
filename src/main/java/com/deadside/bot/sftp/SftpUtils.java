package com.deadside.bot.sftp;

import com.deadside.bot.db.models.GameServer;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for SFTP connections
 */
public class SftpUtils {
    private static final Logger logger = LoggerFactory.getLogger(SftpUtils.class);
    
    /**
     * Close an SFTP channel safely
     * @param channel The channel to close
     */
    public static void closeChannel(ChannelSftp channel) {
        if (channel != null && channel.isConnected()) {
            try {
                channel.disconnect();
            } catch (Exception e) {
                logger.debug("Error closing SFTP channel: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Disconnect an SSH session safely
     * @param session The session to disconnect
     */
    public static void disconnect(Session session) {
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception e) {
                logger.debug("Error disconnecting SSH session: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Helper method to construct a standard path for a server
     * @param server The game server
     * @param suffix The path suffix to append
     * @return The full path
     */
    public static String constructPath(GameServer server, String suffix) {
        if (server == null) {
            return suffix;
        }
        
        // Get server properties
        String host = server.getSftpHost();
        if (host == null || host.isEmpty()) {
            host = server.getHost();
        }
        
        String serverName = server.getServerId();
        if (serverName == null || serverName.isEmpty()) {
            serverName = server.getName().replaceAll("\\s+", "_");
        }
        
        // Construct the path
        String basePath = host + "_" + serverName;
        
        if (suffix != null && !suffix.isEmpty()) {
            return basePath + "/" + suffix;
        } else {
            return basePath;
        }
    }
}