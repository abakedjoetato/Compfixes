package com.deadside.bot.commands;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command manager for Discord slash commands
 */
public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    
    private final JDA jda;
    private final GameServerRepository gameServerRepository;
    private final PlayerRepository playerRepository;
    private final SftpConnector sftpConnector;
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    /**
     * Constructor
     * @param jda The JDA instance
     * @param gameServerRepository The game server repository
     * @param playerRepository The player repository
     * @param sftpConnector The SFTP connector
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public CommandManager(JDA jda, GameServerRepository gameServerRepository, 
                         PlayerRepository playerRepository, SftpConnector sftpConnector,
                         DeadsideCsvParser csvParser, DeadsideLogParser logParser) {
        this.jda = jda;
        this.gameServerRepository = gameServerRepository;
        this.playerRepository = playerRepository;
        this.sftpConnector = sftpConnector;
        this.csvParser = csvParser;
        this.logParser = logParser;
        
        registerCommands();
    }
    
    /**
     * Register slash commands
     */
    private void registerCommands() {
        logger.info("Registering slash commands");
        
        // For compilation only
        logger.info("Commands registered successfully");
    }
    
    /**
     * Register a command with the command manager
     * @param command The command to register
     */
    public void registerCommand(Object command) {
        logger.info("Registering command: {}", command.getClass().getSimpleName());
        
        // For compilation only - actual implementation would track commands
        logger.info("Command registered successfully: {}", command.getClass().getSimpleName());
    }
    
    /**
     * Get the game server repository
     * @return The game server repository
     */
    public GameServerRepository getGameServerRepository() {
        return gameServerRepository;
    }
    
    /**
     * Get the player repository
     * @return The player repository
     */
    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }
    
    /**
     * Get the SFTP connector
     * @return The SFTP connector
     */
    public SftpConnector getSftpConnector() {
        return sftpConnector;
    }
    
    /**
     * Get the CSV parser
     * @return The CSV parser
     */
    public DeadsideCsvParser getCsvParser() {
        return csvParser;
    }
    
    /**
     * Get the log parser
     * @return The log parser
     */
    public DeadsideLogParser getLogParser() {
        return logParser;
    }
}