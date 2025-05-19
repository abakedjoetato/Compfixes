package com.deadside.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

/**
 * Utility class for creating themed Discord embeds
 * This class provides methods for creating consistently styled embeds
 */
public class EmbedThemes {
    // Success theme color (green)
    private static final Color SUCCESS_COLOR = new Color(0, 170, 0);
    
    // Error theme color (red)
    private static final Color ERROR_COLOR = new Color(200, 0, 0);
    
    // Warning theme color (orange)
    private static final Color WARNING_COLOR = new Color(255, 150, 0);
    
    // Info theme color (blue)
    private static final Color INFO_COLOR = new Color(0, 120, 200);
    
    /**
     * Create a success embed
     * @param title The embed title
     * @param description The embed description
     * @return The created embed
     */
    public static MessageEmbed successEmbed(String title, String description) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(SUCCESS_COLOR)
            .build();
    }
    
    /**
     * Create an error embed
     * @param title The embed title
     * @param description The embed description
     * @return The created embed
     */
    public static MessageEmbed errorEmbed(String title, String description) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(ERROR_COLOR)
            .build();
    }
    
    /**
     * Create a warning embed
     * @param title The embed title
     * @param description The embed description
     * @return The created embed
     */
    public static MessageEmbed warningEmbed(String title, String description) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(WARNING_COLOR)
            .build();
    }
    
    /**
     * Create an info embed
     * @param title The embed title
     * @param description The embed description
     * @return The created embed
     */
    public static MessageEmbed infoEmbed(String title, String description) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(INFO_COLOR)
            .build();
    }
}