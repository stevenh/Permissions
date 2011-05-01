package com.nijikokun.bukkit.Permissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.Messaging;
import com.nijiko.Misc;
import com.nijiko.configuration.ConfigurationHandler;
import com.nijiko.configuration.DefaultConfiguration;
import com.nijiko.configuration.NotNullConfiguration;
import com.nijiko.permissions.Control;
import com.nijiko.permissions.PermissionHandler;

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

public class Permissions extends JavaPlugin {

    public static Logger log = Logger.getLogger("Minecraft");
    public static PluginDescriptionFile description;
    public static Plugin instance;
    public static Server Server = null;
    public File directory;
    private DefaultConfiguration config;
    public static String name = "Permissions";
    public static String version = "2.7.2";
    public static String codename = "Phoenix";
    
    
    public Listener l = new Listener(this);

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
        try {
        	if (!(new File("plugins" + File.separator + "Permissions", DefaultWorld + ".yml").exists())) {
        		if (new File("plugins" + File.separator + "Permissions", "world.yml").exists()) {
        			if (!DefaultWorld.equals("world")) {
        				FileChannel source = null;
        				FileChannel destination = null;
        				try {
        					source = new FileInputStream(new File("plugins" + File.separator + "Permissions", "world.yml")).getChannel();
        					destination = new FileOutputStream(new File("plugins" + File.separator + "Permissions" + File.separator, DefaultWorld + ".yml")).getChannel();
        					destination.transferFrom(source, 0, source.size());
        				}
        				finally {
        					if (source != null) {
        						source.close();
        					}
        					if (destination != null) {
        						destination.close();
        					}
        				}
        				Misc.delete("plugins" + File.separator + "Permissions" + File.separator + "world.yml");
        				log.info("[Permissions] Invalid config file found and converted to proper name/format.");
        			}
        		}
        		else if (new File("plugins" + File.separator + "Permissions", "config.yml").exists()) {
        			FileChannel source = null;
        			FileChannel destination = null;
        		
        			try {
        				source = new FileInputStream(new File("plugins" + File.separator + "Permissions", "config.yml")).getChannel();
        				destination = new FileOutputStream(new File("plugins" + File.separator + "Permissions"  + File.separator, DefaultWorld + ".yml")).getChannel();
        				destination.transferFrom(source, 0, source.size());
        			}
        			finally {
        				if (source != null) {
        					source.close();
        				}
        				if (destination != null) {
        					destination.close();
        				}
        			}
        			Misc.delete("plugins" + File.separator + "Permissions" + File.separator + "config.yml");
        			log.info("[Permissions] Invalid config file found and converted to proper name/format.");
        		}           
        	}
        } catch (IOException e) {
        	e.printStackTrace();
        }

        Configuration configure = new NotNullConfiguration(new File("plugins" + File.separator + "Permissions", DefaultWorld + ".yml"));
        configure.load();

        // Gogo
        this.config = new ConfigurationHandler(configure);

        instance = this;

        // Enabled
        log.info("[Permissions] (" + codename + ") was Initialized.");
    }
    
    public void onLoad() {
        // Setup Permission
        setupPermissions();
    }

    public void onDisable() {
    	//Addition by rcjrrjcr
    	Security.saveAll();
    	log.info("[Permissions] (" + codename + ") saved all data.");
    	//End of addition by rcjrrjcr
    	
    	log.info("[Permissions] (" + codename + ") disabled successfully.");
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
        Security = new Control(new NotNullConfiguration(new File("plugins" + File.separator + "Permissions", DefaultWorld + ".yml")));
        Security.setDefaultWorld(DefaultWorld);
        Security.load();
    }

    public void onEnable() {
    	Server = this.getServer();
    	description = this.getDescription();
    	directory = getDataFolder();
    	
    	
        // Start Registration
        getDataFolder().mkdirs();

        PropertyHandler server = new PropertyHandler("server.properties");
        DefaultWorld = server.getString("level-name");

        // Attempt
//        if (!(new File(getDataFolder(), DefaultWorld + ".yml").exists())) {
//            com.nijiko.Misc.touch(getDataFolder() + File.separator + DefaultWorld + ".yml");
//        }

        // Gogo
        this.config = new ConfigurationHandler(getConfiguration());

        // Load Configuration File
        getConfiguration().load();

        // Load Configuration Settings
        this.config.load();

        // Enabled
        log.info("[" + description.getName() + "] version [" + description.getVersion() + "] (" + codename + ")  loaded");
        
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_PLACE, l, Priority.High, this);
        this.getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, l, Priority.High, this);
    }
    
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;
        String commandName = command.getName().toLowerCase();
        PluginDescriptionFile pdfFile = this.getDescription();
        
        if (sender instanceof Player) {
        	player = (Player)sender;

        	Messaging.save(player);
        }

        if (commandName.compareToIgnoreCase("permissions") == 0) {
        	if (args.length < 1) {
        		if (player != null) {
        			Messaging.send("&7-------[ &fPermissions&7 ]-------");
        			Messaging.send("&7Currently running version: &f[" + pdfFile.getVersion() + "] (" + codename + ")");

        			if (Security.has(player, "permissions.reload")) {
        				Messaging.send("&7Reload with: &f/permissions -reload [World]");
        				Messaging.send("&fLeave [World] blank to reload default world.");
        			}

        			Messaging.send("&7-------[ &fPermissions&7 ]-------");
        			return true;
        			}
        		else {
                	sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + codename + ")  loaded");
        		}
            }
                    
        	if (args.length >= 1) {
        		if (args[0].compareToIgnoreCase("-reload") == 0) {
        			if (args.length == 2) {
        				if (args[1].compareToIgnoreCase("all") == 0) {
        					if (player != null) {
        						if (Security.has(player, "permissions.reload")) {
        							Security.reload();
        							player.sendMessage(ChatColor.GRAY + "[Permissions] World Reloads completed.");
        							return true;
        						}
        						else {
        							player.sendMessage(ChatColor.RED + "[Permissions] You lack the necessary permissions to perform this action.");
        							return true;
        						}
        					}
        					else {
        						Security.reload();
        						sender.sendMessage("All world files reloaded.");
        						return true;
        					}
        				}
        				else {
        					if (player != null) {
        						if (Security.has(player, "permissions.reload")) {
        							String world = args[1];
        							if (Security.reload(world)) {
        								player.sendMessage(ChatColor.GRAY + "[Permissions] " + args[1] + " World Reload completed.");
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
        					else {
        						String world = args[1];
        						if (Security.reload(world)) {
        							sender.sendMessage("[Permissions] Reload of World " + world + " completed.");
        						}
        						else {
        							sender.sendMessage("[Permissions] World " + world + " does not exist.");
        						}
        						return true;
        					}
        				}
        			}
        		}
        	}
        }
        return false;
    }
}
