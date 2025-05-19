package com.deadside.bot.listeners;

import com.deadside.bot.commands.CommandManager;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General event listener for the bot
 */
public class EventListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(EventListener.class);
    
    private final CommandManager commandManager;
    private final JDA jda;
    private final GameServerRepository gameServerRepository;
    private final PlayerRepository playerRepository;
    
    /**
     * Constructor
     * @param commandManager The command manager
     * @param jda The JDA instance
     * @param gameServerRepository The game server repository
     * @param playerRepository The player repository
     */
    public EventListener(CommandManager commandManager, JDA jda,
                      GameServerRepository gameServerRepository,
                      PlayerRepository playerRepository) {
        this.commandManager = commandManager;
        this.jda = jda;
        this.gameServerRepository = gameServerRepository;
        this.playerRepository = playerRepository;
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        logger.info("Bot is ready! Logged in as {}", event.getJDA().getSelfUser().getAsTag());
    }
    
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        logger.info("Guild {} is ready!", event.getGuild().getName());
        
        // Initialize guild-specific resources if needed
    }
}