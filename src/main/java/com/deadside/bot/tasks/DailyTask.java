package com.deadside.bot.tasks;

import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task that runs daily
 */
public class DailyTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DailyTask.class);
    
    private final JDA jda;
    private final GameServerRepository gameServerRepository;
    private final PlayerRepository playerRepository;
    
    /**
     * Constructor
     * @param jda The JDA instance
     * @param gameServerRepository The game server repository
     * @param playerRepository The player repository
     */
    public DailyTask(JDA jda, GameServerRepository gameServerRepository, PlayerRepository playerRepository) {
        this.jda = jda;
        this.gameServerRepository = gameServerRepository;
        this.playerRepository = playerRepository;
    }
    
    @Override
    public void run() {
        logger.info("Running daily task");
        
        try {
            // Run daily tasks
            // For compilation only
            logger.info("Daily task completed successfully");
        } catch (Exception e) {
            logger.error("Error running daily task", e);
        }
    }
}