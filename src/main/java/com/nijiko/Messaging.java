package com.nijiko;

import org.bukkit.entity.Player;

/**
 * Permissions 2.x
 * Copyright (C) 2011  Matt 'The Yeti' Burnett <admin@theyeticave.net>
 * Original Credit & Copyright (C) 2010 Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Permissions Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Permissions Public License for more details.
 *
 * You should have received a copy of the GNU Permissions Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Messaging.java
 * <br /><br />
 * Lets us do fancy pantsy things with colors, messages, and broadcasting :D!
 *
 * @author Nijikokun <nijikokun@gmail.com>
 */
public class Messaging {

    public static Player player = null;

    /**
     * Converts a list of arguments into points.
     *
     * @param original The original string necessary to convert inside of.
     * @param arguments The list of arguments, multiple arguments are seperated by commas for a single point.
     * @param points The point used to alter the argument.
     *
     * @return <code>String</code> - The parsed string after converting arguments to variables (points)
     */
    public static String argument(String original, String[] arguments, String[] points) {
       for (int i = 0; i < arguments.length; i++) {
	    if(arguments[i].contains(",")) {
		for(String arg : arguments[i].split(",")) {
		    original = original.replace(arg, points[i]);
		}
	    } else {
		original = original.replace(arguments[i], points[i]);
	   }
        }

       return original;
    }

    /**
     * Parses the original string against color specific codes. This one converts &[code] to ยง[code]
     * <br /><br />
     * Example:
     * <blockquote><pre>
     * Messaging.parse("Hello &2world!"); // returns: Hello ยง2world!
     * </pre></blockquote>
     *
     * @param original The original string used for conversions.
     *
     * @return <code>String</code> - The parsed string after conversion.
     */
    public static String parse(String original) {
	original = colorize(original);
	//Optionally change the \u00A7 back to ยง and tack .replace(String.valueOf((char)194), "") onto end of line
	return original.replaceAll("(&([a-z0-9]))", "\u00A7$2").replace("&&", "&");
    }

    /**
     * Converts color codes into the simoleon code. Sort of a HTML format color code tag.
     * <p>
     * Color codes allowed: black, navy, green, teal, red, purple, gold, silver, gray, blue, lime, aqua, rose, pink, yellow, white.</p>
     * Example:
     * <blockquote<pre>
     * Messaging.colorize("Hello &lt;green>world!"); // returns: Hello ยง2world!
     * </pre></blockquote>
     *
     * @param original Original string to be parsed against group of color names.
     *
     * @return <code>String</code> - The parsed string after conversion.
     */
    public static String colorize(String original) {
    //Removed the weird ย character
	return original.replace("<black>", "ง0").replace("<navy>", "ง1").replace("<green>", "ง2").replace("<teal>", "ง3").replace("<red>", "ง4").replace("<purple>", "ง5").replace("<gold>", "ง6").replace("<silver>", "ง7").replace("<gray>", "ง8").replace("<blue>", "ง9").replace("<lime>", "งa").replace("<aqua>", "งb").replace("<rose>", "งc").replace("<pink>", "งd").replace("<yellow>", "งe").replace("<white>", "งf");
    }

    /**
     * Helper function to assist with making brackets. Why? Dunno, lazy.
     *
     * @param message The message inside of brackets.
     *
     * @return <code>String</code> - The message inside [brackets]
     */
    public static String bracketize(String message) {
	return "[" + message + "]";
    }

    /**
     * Save the player to be sent messages later. Ease of use sending messages.
     * <br /><br />
     * Example:
     * <blockquote><pre>
     * Messaging.save(player);
     * Messaging.send("This will go to the player saved.");
     * </pre></blockquote>
     *
     * @param player The player we wish to save for later.
     */
    public static void save(Player player) {
	Messaging.player = player;
    }

    /**
     * Sends a message to a specific player.
     * <br /><br />
     * Example:
     * <blockquote><pre>
     * Messaging.send(player, "This will go to the player saved.");
     * </pre></blockquote>
     *
     * @param player Player we are sending the message to.
     * @param message The message to be sent.
     */
    public static void send(Player player, String message) {
	player.sendMessage(parse(message));
    }

    /**
     * Sends a message to the stored player.
     *
     * @param message The message to be sent.
     * @see Messaging#save(Player)
     */
    public static void send(String message) {
	if(Messaging.player != null)
	    player.sendMessage(parse(message));
    }
}
