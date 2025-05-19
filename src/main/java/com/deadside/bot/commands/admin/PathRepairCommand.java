package com.deadside.bot.commands.admin;

import com.deadside.bot.Bot;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.fixes.PathResolutionManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command for repairing server paths
 * This command allows admins to repair server paths
 */
public class PathRepairCommand extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PathRepairCommand.class);
    
    // The Bot instance
    private final Bot bot;
    
    /**
     * Constructor
     * @param bot The Bot instance
     */
    public PathRepairCommand(Bot bot) {
        this.bot = bot;
    }
    
    /**
     * Create the command
     * @return The command data
     */
    public static SlashCommandData createCommand() {
        return Commands.slash("pathrepair", "Repair server paths")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addOption(OptionType.STRING, "server_name", "The name of the server to repair (leave blank for all servers)", false);
    }
    
    /**
     * Handle the command
     * @param event The event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pathrepair")) {
            return;
        }
        
        try {
            // Check if user has admin permission
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("You need Administrator permission to use this command.").setEphemeral(true).queue();
                return;
            }
            
            // Defer reply to avoid timeout
            event.deferReply().queue();
            
            // Get the guild ID
            long guildId = event.getGuild().getIdLong();
            
            // Set context for guild
            com.deadside.bot.utils.GuildIsolationManager.getInstance().setContext(guildId, null);
            
            try {
                // Get the server name if provided
                String serverName = event.getOption("server_name") != null ? 
                    event.getOption("server_name").getAsString() : null;
                
                if (serverName != null && !serverName.isEmpty()) {
                    // Repair a specific server
                    repairSpecificServer(event, guildId, serverName);
                } else {
                    // Repair all servers
                    repairAllServers(event, guildId);
                }
            } finally {
                // Always clear context
                com.deadside.bot.utils.GuildIsolationManager.getInstance().clearContext();
            }
        } catch (Exception e) {
            logger.error("Error executing pathrepair command: {}", e.getMessage(), e);
            event.getHook().sendMessage("An error occurred while repairing paths: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Repair a specific server
     * @param event The event
     * @param guildId The guild ID
     * @param serverName The server name
     */
    private void repairSpecificServer(SlashCommandInteractionEvent event, long guildId, String serverName) {
        try {
            // Get the repository
            GameServerRepository repo = bot.getGameServerRepository();
            
            // Try to find the server
            GameServer server = repo.findByGuildIdAndNameIgnoreCase(guildId, serverName);
            
            if (server == null) {
                event.getHook().sendMessage("Server '" + serverName + "' not found.").queue();
                return;
            }
            
            // Skip restricted servers
            if (server.hasRestrictedIsolation()) {
                event.getHook().sendMessage("Server '" + serverName + "' has restricted isolation and cannot be repaired.").queue();
                return;
            }
            
            // Try to repair the server
            boolean fixed = PathResolutionManager.getInstance().fixPathsForServer(server);
            
            if (fixed) {
                event.getHook().sendMessage("Successfully repaired paths for server '" + serverName + "':\n" +
                    "CSV Path: " + server.getDeathlogsDirectory() + "\n" +
                    "Log Path: " + server.getLogDirectory()).queue();
            } else {
                event.getHook().sendMessage("No path issues found for server '" + serverName + "' or could not fix them.\n" +
                    "Current paths:\n" +
                    "CSV Path: " + server.getDeathlogsDirectory() + "\n" +
                    "Log Path: " + server.getLogDirectory()).queue();
            }
        } catch (Exception e) {
            logger.error("Error repairing server {}: {}", serverName, e.getMessage(), e);
            event.getHook().sendMessage("An error occurred while repairing server '" + serverName + "': " + e.getMessage()).queue();
        }
    }
    
    /**
     * Repair all servers
     * @param event The event
     * @param guildId The guild ID
     */
    private void repairAllServers(SlashCommandInteractionEvent event, long guildId) {
        try {
            // Try to repair all servers
            int fixed = PathResolutionManager.getInstance().fixPathsForGuild(guildId);
            
            // Get all servers for the guild
            GameServerRepository repo = bot.getGameServerRepository();
            List<GameServer> servers = repo.findAllByGuildId(guildId);
            
            StringBuilder sb = new StringBuilder();
            
            if (fixed > 0) {
                sb.append("Successfully repaired paths for ").append(fixed).append(" servers:\n\n");
            } else {
                sb.append("No path issues found or could not fix them.\n\n");
            }
            
            sb.append("Current paths:\n");
            
            for (GameServer server : servers) {
                // Skip restricted servers
                if (server.hasRestrictedIsolation()) {
                    continue;
                }
                
                sb.append("Server: ").append(server.getName()).append("\n");
                sb.append("  CSV Path: ").append(server.getDeathlogsDirectory()).append("\n");
                sb.append("  Log Path: ").append(server.getLogDirectory()).append("\n\n");
            }
            
            event.getHook().sendMessage(sb.toString()).queue();
        } catch (Exception e) {
            logger.error("Error repairing all servers: {}", e.getMessage(), e);
            event.getHook().sendMessage("An error occurred while repairing servers: " + e.getMessage()).queue();
        }
    }
}