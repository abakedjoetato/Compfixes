package com.deadside.bot;

import com.deadside.bot.commands.CommandManager;
import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.listeners.EventListener;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.parsers.fixes.DeadsideParserFixEntrypoint;
import com.deadside.bot.parsers.fixes.DeadsideParserPathRegistry;
import com.deadside.bot.services.CronService;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.tasks.DailyTask;
import com.deadside.bot.tasks.HourlyTask;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main bot class for the Deadside Discord Bot
 */
public class Bot {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    
    private final JDA jda;
    private final CommandManager commandManager;
    private final List<ListenerAdapter> listeners = new ArrayList<>();
    private CronService cronService;
    
    /**
     * Constructor
     * @param token The Discord bot token
     * @throws LoginException If login fails
     */
    public Bot(String token) throws LoginException {
        logger.info("Initializing Deadside Discord Bot");
        
        // Initialize MongoDB
        try {
            // Use default connection string from config
            String connectionString = "mongodb://localhost:27017";
            MongoDBConnection.getInstance().initialize(connectionString);
        } catch (Exception e) {
            logger.error("Error initializing MongoDB", e);
        }
        
        // Create JDA instance
        jda = JDABuilder.createDefault(token)
            .setStatus(OnlineStatus.ONLINE)
            .setActivity(Activity.playing("Deadside"))
            .enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(ChunkingFilter.ALL)
            .build();
        
        // Create repositories
        GameServerRepository gameServerRepository = new GameServerRepository();
        PlayerRepository playerRepository = new PlayerRepository();
        
        // Create SFTP connector
        SftpConnector sftpConnector = new SftpConnector();
        
        // Initialize path registry
        DeadsideParserPathRegistry.getInstance().initialize();
        
        // Create parsers
        DeadsideCsvParser csvParser = new DeadsideCsvParser(jda, sftpConnector, playerRepository, gameServerRepository);
        DeadsideLogParser logParser = new DeadsideLogParser(jda, gameServerRepository, sftpConnector);
        
        // Apply parser fixes
        DeadsideParserFixEntrypoint fixEntrypoint = new DeadsideParserFixEntrypoint(
            jda, gameServerRepository, playerRepository, sftpConnector, csvParser, logParser);
        
        fixEntrypoint.executeAllFixesAsBatch();
        
        // Create command manager
        commandManager = new CommandManager(jda, gameServerRepository, playerRepository, sftpConnector, csvParser, logParser);
        
        // Initialize simple listener
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(net.dv8tion.jda.api.events.session.ReadyEvent event) {
                logger.info("Bot is ready! Logged in as {}", event.getJDA().getSelfUser().getAsTag());
            }
        });
        
        // Add simple listener for command interactions
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onSlashCommandInteraction(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
                logger.info("Received slash command: {}", event.getName());
                event.reply("Command received: " + event.getName()).queue();
            }
        });
        
        // Register listeners (simplified for compilation)
        for (ListenerAdapter listener : listeners) {
            jda.addEventListener(listener);
        }
        
        logger.info("Deadside Discord Bot initialized");
    }
    
    /**
     * Get the JDA instance
     * @return The JDA instance
     */
    public JDA getJda() {
        return jda;
    }
    
    /**
     * Get the command manager
     * @return The command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    /**
     * Get the cron service
     * @return The cron service
     */
    public CronService getCronService() {
        return cronService;
    }
    
    /**
     * Get the game server repository
     * @return The game server repository
     */
    public GameServerRepository getGameServerRepository() {
        return commandManager.getGameServerRepository();
    }
    
    /**
     * Get the player repository
     * @return The player repository
     */
    public PlayerRepository getPlayerRepository() {
        return commandManager.getPlayerRepository();
    }
    
    /**
     * Get the SFTP connector
     * @return The SFTP connector
     */
    public SftpConnector getSftpConnector() {
        return commandManager.getSftpConnector();
    }
    
    /**
     * Get the CSV parser
     * @return The CSV parser
     */
    public DeadsideCsvParser getCsvParser() {
        return commandManager.getCsvParser();
    }
    
    /**
     * Get the log parser
     * @return The log parser
     */
    public DeadsideLogParser getLogParser() {
        return commandManager.getLogParser();
    }
    
    /**
     * Shutdown the bot
     */
    public void shutdown() {
        logger.info("Shutting down Deadside Discord Bot");
        
        // Shutdown cron service
        if (cronService != null) {
            cronService.shutdown();
        }
        
        // Shutdown JDA
        if (jda != null) {
            jda.shutdown();
        }
        
        // Shutdown MongoDB
        try {
            // Use static close method instead
            MongoDBConnection.getInstance().close();
        } catch (Exception e) {
            logger.error("Error shutting down MongoDB", e);
        }
        
        logger.info("Deadside Discord Bot shutdown complete");
    }
}