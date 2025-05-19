package com.deadside.bot.parsers;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log parser for Deadside logs
 */
public class DeadsideLogParser {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideLogParser.class);
    
    private JDA jda;
    private final GameServerRepository gameServerRepository;
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param jda The JDA instance
     * @param gameServerRepository The game server repository
     * @param sftpConnector The SFTP connector
     */
    public DeadsideLogParser(JDA jda, GameServerRepository gameServerRepository, 
                           SftpConnector sftpConnector) {
        this.jda = jda;
        this.gameServerRepository = gameServerRepository;
        this.sftpConnector = sftpConnector;
        
        logger.info("Log parser initialized");
    }
    
    /**
     * Set the JDA instance (used for updating after JDA is built)
     * @param jda The JDA instance
     */
    public void setJda(JDA jda) {
        this.jda = jda;
    }
}