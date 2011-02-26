package com.nijikokun.bukkit.Permissions;

import com.nijiko.Messaging;
import com.nijiko.Misc;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import com.nijiko.configuration.ConfigurationHandler;
import com.nijiko.configuration.DefaultConfiguration;
import com.nijiko.permissions.Control;
import com.nijiko.permissions.PermissionHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Permissions 1.x & Code from iConomy 2.x
 * Copyright (C) 2011  Nijikokun <nijikokun@gmail.com>
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
public class Permissions extends JavaPlugin {

    public static Logger log = Logger.getLogger("Minecraft");
    public static PluginDescriptionFile description;
    public static Plugin instance;
    public static Server Server = null;
    private DefaultConfiguration config;
    public static String name = "Permissions";
    public static String codename = "Phoenix";
    public static String version = "2.3";

    /**
     * Controller for permissions and security.
     */
    public static PermissionHandler Security;

    /**
     * Miscellaneous object for various functions that don't belong anywhere else
     */
    public static Misc Misc = new Misc();

    private String DefaultWorld = "";

    public Permissions() {
        new File("plugins" + File.separator + "Permissions" + File.separator).mkdirs();

        PropertyHandler server = new PropertyHandler("server.properties");
        DefaultWorld = server.getString("level-name");

        // Attempt
        if (!(new File(getDataFolder(), DefaultWorld + ".yml").exists())) {
            com.nijiko.Misc.touch(DefaultWorld + ".yml");
        }

        Configuration configure = new Configuration(new File(getDataFolder(), DefaultWorld + ".yml"));
        configure.load();

        // Gogo
        this.config = new ConfigurationHandler(configure);

        // Setup Permission
        setupPermissions();

        // Enabled
        log.info("[" + name + "] version [" + version + "] (" + codename + ") was Initialized.");
    }
    

    public void onDisable() {
    	PluginDescriptionFile pdfFile = this.getDescription();
    	log.info("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + codename + ") disabled successfully.");
    	return;
    }

    /**
     * Alternative method of grabbing Permissions.Security
     * <br /><br />
     * <blockquote><pre>
     * Permissions.getHandler()
     * </pre></blockquote>
     *
     * @return PermissionHandler
     */
    public PermissionHandler getHandler() {
        return Permissions.Security;
    }

    public void setupPermissions() {
        Security = new Control(new Configuration(new File(getDataFolder(), DefaultWorld + ".yml")));
        Security.setDefaultWorld(DefaultWorld);
        Security.setDirectory(new File("plugins" + File.separator + "Permissions"));
        Security.load();
    }

    public void onEnable() {
    	instance = this;
    	Server = this.getServer();
    	description = this.getDescription();
    	
        // Start Registration
        getDataFolder().mkdirs();

        PropertyHandler server = new PropertyHandler("server.properties");
        DefaultWorld = server.getString("level-name");

        // Attempt
        if (!(new File(getDataFolder(), DefaultWorld + ".yml").exists())) {
            com.nijiko.Misc.touch(DefaultWorld + ".yml");
        }

        // Gogo
        this.config = new ConfigurationHandler(getConfiguration());

        // Load Configuration File
        getConfiguration().load();

        // Load Configuration Settings
        this.config.load();

        // Setup Permission
        setupPermissions();

        // Enabled
        PluginDescriptionFile pdfFile = this.getDescription();
        log.info("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + codename + ")  loaded");
    }
    
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;
        String[] tArgs = args;
        String commandName = command.getName().toLowerCase();
        
        if (sender instanceof Player) {
        	player = (Player)sender;
        	if (!Security.permission(player, "permissions.info")) {
        		player.sendMessage("You lack sufficient permissions to do that");
        		return true;
        	}
        }
    	Messaging.save(player);

        if (commandName.compareToIgnoreCase("permissions") == 0) {
        	if (tArgs.length < 1) {
        		Messaging.send("&7-------[ &fPermissions&7 ]-------");
                Messaging.send("&7Currently running version: &f[" + version + "] (" + codename + ")");

                if (Security.permission(player, "permissions.reload")) {
                	Messaging.send("&7Reload with: &f/permissions -reload [World]");
                	Messaging.send("&fLeave [World] blank to reload default world.");
                }

                Messaging.send("&7-------[ &fPermissions&7 ]-------");
                return true;
            }
                    
            if (tArgs[0].compareToIgnoreCase("-reload") == 0) {
            	if (tArgs.length == 2) {
            		if (tArgs[1].compareToIgnoreCase("all") == 0) {
            			if (Security.permission(player, "permissions.reload")) {
            				Security.reload();
            				player.sendMessage(ChatColor.GRAY + "[Permissions] Default World Reload completed.");
                			return true;
            			}
            			else {
            				player.sendMessage(ChatColor.RED + "[Permissions] You lack the necessary permissions to perform this action.");
            				return true;
            			}
            		}
            		else {
            			if (Security.permission(player, "permissions.reload")) {
            				String world = tArgs[1];
            				if (Security.reload(world)) {
            					player.sendMessage(ChatColor.GRAY + "[Permissions] " + tArgs[1] + " World Reload completed.");
            				}
            				else {
            					Messaging.send("&7[Permissions] " + world + " does not exist.");
            				}
                			return true;
            			}
            			else {
            				player.sendMessage(ChatColor.RED + "[Permissions] You lack the necessary permissions to permform this action.");
            				return true;
            			}
            		} 
            	}
            }
        }
        return false;
    }
}
