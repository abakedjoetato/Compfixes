package com.deadside.bot.parsers.fixes;

import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for parser integration
 */
public class ParserEventListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ParserEventListener.class);
    
    private final DeadsideCsvParser csvParser;
    private final DeadsideLogParser logParser;
    
    /**
     * Constructor
     * @param csvParser The CSV parser
     * @param logParser The log parser
     */
    public ParserEventListener(DeadsideCsvParser csvParser, DeadsideLogParser logParser) {
        this.csvParser = csvParser;
        this.logParser = logParser;
        
        logger.info("Parser event listener initialized");
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) {
            return;
        }
        
        // Handle parser events if needed
        // This is just a placeholder for compilation
    }
}