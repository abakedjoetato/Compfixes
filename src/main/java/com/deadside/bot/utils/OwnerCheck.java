package com.deadside.bot.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for checking owner and admin permissions
 */
public class OwnerCheck {
    private static final Logger logger = LoggerFactory.getLogger(OwnerCheck.class);
    
    // Configurable owner and admin IDs
    private static final List<Long> OWNER_IDS = Arrays.asList(
        // Add your owner IDs here
        123456789012345678L
    );
    
    /**
     * Check if the user is an owner
     * @param userId The user ID
     * @return True if the user is an owner
     */
    public static boolean isOwner(long userId) {
        return OWNER_IDS.contains(userId);
    }
    
    /**
     * Check if the user is an owner
     * @param user The user
     * @return True if the user is an owner
     */
    public static boolean isOwner(User user) {
        if (user == null) {
            return false;
        }
        
        return isOwner(user.getIdLong());
    }
    
    /**
     * Check if the user is an owner
     * @param event The slash command event
     * @return True if the user is an owner
     */
    public static boolean isOwner(SlashCommandInteractionEvent event) {
        if (event == null || event.getUser() == null) {
            return false;
        }
        
        return isOwner(event.getUser());
    }
    
    /**
     * Check if the member is an admin
     * @param member The member
     * @return True if the member is an admin
     */
    public static boolean isAdmin(Member member) {
        if (member == null) {
            return false;
        }
        
        // Check if the member has administrator permission
        return member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR);
    }
}