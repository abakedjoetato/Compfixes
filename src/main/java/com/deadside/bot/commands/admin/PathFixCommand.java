package com.deadside.bot.commands.admin;

import com.deadside.bot.Bot;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.fixes.DirectPathResolutionFix;
import com.deadside.bot.parsers.fixes.PathIsolationFix;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedThemes;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Admin command for fixing server paths
 */
public class PathFixCommand extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PathFixCommand.class);
    
    // Bot instance
    private final Bot bot;
    
    /**
     * Constructor
     * @param bot The Bot instance
     */
    public PathFixCommand(Bot bot) {
        this.bot = bot;
    }
    
    /**
     * Create the command data
     * @return The command data
     */
    public static SlashCommandData createCommand() {
        return Commands.slash("pathfix", "Fix server file paths")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addOption(OptionType.STRING, "server", "Server name to fix (leave empty to fix all servers)", false);
    }
    
    /**
     * Handle slash command interaction
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pathfix")) {
            return;
        }
        
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        // Check if user has admin permission
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You need Administrator permission to use this command.").setEphemeral(true).queue();
            return;
        }
        
        // Defer reply as this might take a while
        event.deferReply().queue();
        
        // Get option
        OptionMapping serverOption = event.getOption("server");
        String serverName = serverOption != null ? serverOption.getAsString() : null;
        
        if (serverName != null && !serverName.isEmpty()) {
            // Fix specific server
            fixServerPaths(event, guild.getIdLong(), serverName);
        } else {
            // Fix all servers
            fixAllServerPaths(event, guild.getIdLong());
        }
    }
    
    /**
     * Fix paths for a specific server
     * @param event The event
     * @param guildId The guild ID
     * @param serverName The server name
     */
    private void fixServerPaths(SlashCommandInteractionEvent event, long guildId, String serverName) {
        try {
            // Set context for guild isolation
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                // Get the server
                GameServerRepository repository = bot.getGameServerRepository();
                GameServer server = repository.findByGuildIdAndNameIgnoreCase(guildId, serverName);
                
                if (server == null) {
                    event.getHook().sendMessageEmbeds(
                        EmbedThemes.errorEmbed("Server Not Found", 
                            "Could not find server with name: " + serverName)
                    ).queue();
                    return;
                }
                
                // Skip restricted servers
                if (server.hasRestrictedIsolation()) {
                    event.getHook().sendMessageEmbeds(
                        EmbedThemes.warningEmbed("Server Restricted", 
                            "Server '" + serverName + "' has restricted isolation and cannot be modified.")
                    ).queue();
                    return;
                }
                
                // Check current paths
                String originalCsvPath = server.getDeathlogsDirectory();
                String originalLogPath = server.getLogDirectory();
                
                // Fix paths
                SftpConnector connector = bot.getSftpConnector();
                boolean fixed = DirectPathResolutionFix.fixServerPaths(server, connector);
                
                if (fixed) {
                    // Save the server
                    repository.save(server);
                    
                    // Send success message
                    event.getHook().sendMessageEmbeds(
                        EmbedThemes.successEmbed("Paths Fixed", 
                            "Successfully fixed paths for server '" + serverName + "':\n\n" +
                            "**CSV Path**:\n" +
                            "Original: " + originalCsvPath + "\n" +
                            "Fixed: " + server.getDeathlogsDirectory() + "\n\n" +
                            "**Log Path**:\n" +
                            "Original: " + originalLogPath + "\n" +
                            "Fixed: " + server.getLogDirectory())
                    ).queue();
                } else {
                    // Send message that no paths needed fixing
                    event.getHook().sendMessageEmbeds(
                        EmbedThemes.infoEmbed("No Paths Fixed", 
                            "Server '" + serverName + "' does not need path fixing or paths could not be resolved.\n\n" +
                            "**Current Paths**:\n" +
                            "CSV Path: " + server.getDeathlogsDirectory() + "\n" +
                            "Log Path: " + server.getLogDirectory())
                    ).queue();
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error fixing paths for server {}: {}", serverName, e.getMessage(), e);
            
            event.getHook().sendMessageEmbeds(
                EmbedThemes.errorEmbed("Error", 
                    "An error occurred while fixing paths for server '" + serverName + "':\n" +
                    e.getMessage())
            ).queue();
        }
    }
    
    /**
     * Fix paths for all servers in a guild
     * @param event The event
     * @param guildId The guild ID
     */
    private void fixAllServerPaths(SlashCommandInteractionEvent event, long guildId) {
        try {
            // Fix paths for all servers in this guild
            SftpConnector connector = bot.getSftpConnector();
            GameServerRepository repository = bot.getGameServerRepository();
            
            int fixed = PathIsolationFix.fixGuildServerPaths(guildId, repository, connector);
            
            // Set context for guild isolation
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                // Get all servers for this guild
                List<GameServer> servers = repository.findAllByGuildId(guildId);
                StringBuilder sb = new StringBuilder();
                
                // List all server paths
                for (GameServer server : servers) {
                    // Skip restricted servers
                    if (server.hasRestrictedIsolation()) {
                        continue;
                    }
                    
                    sb.append("**").append(server.getName()).append("**\n");
                    sb.append("CSV Path: ").append(server.getDeathlogsDirectory()).append("\n");
                    sb.append("Log Path: ").append(server.getLogDirectory()).append("\n\n");
                }
                
                // Send response
                if (fixed > 0) {
                    event.getHook().sendMessageEmbeds(
                        EmbedThemes.successEmbed("Paths Fixed", 
                            "Successfully fixed paths for " + fixed + " servers.\n\n" +
                            "**Current Server Paths**:\n" + sb.toString())
                    ).queue();
                } else {
                    event.getHook().sendMessageEmbeds(
                        EmbedThemes.infoEmbed("No Paths Fixed", 
                            "No servers needed path fixing or paths could not be resolved.\n\n" +
                            "**Current Server Paths**:\n" + sb.toString())
                    ).queue();
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error fixing paths for all servers: {}", e.getMessage(), e);
            
            event.getHook().sendMessageEmbeds(
                EmbedThemes.errorEmbed("Error", 
                    "An error occurred while fixing paths for all servers:\n" +
                    e.getMessage())
            ).queue();
        }
    }
}