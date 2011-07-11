package com.nijikokun.bukkit.Permissions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.World;

import com.nijiko.Messaging;
import com.nijiko.Misc;
import com.nijiko.configuration.ConfigurationHandler;
import com.nijiko.configuration.DefaultConfiguration;
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
    public static String codename = "MultiplayCompat";
    private static final String defaultConfig = "config";
    private String name;


    public BlockListener blockListener = new BlockListener(this);
    public WorldListener worldListener = new WorldListener(this);

    /**
     * Controller for permissions and security.
     */
    public static PermissionHandler Security;

    /**
     * Miscellaneous object for various functions that don't belong anywhere else
     */
    public static Misc Misc = new Misc();

    public void info( String string )
    {
        log.info( "[" + this.name + "] " + string );
    }

    public void warn( String string )
    {
        log.log( Level.WARNING, "[" + this.name + "] " + string );
    }

    public String getDefaultWorld() {
        PropertyHandler server = new PropertyHandler("server.properties");
        return server.getString("level-name");
    }

    public String getWorldConfigFilename( String world ) {
        return getDataFolder() + File.separator + world + ".yml";
    }

    public File getDefaultConfigFile() {
        return new File( getWorldConfigFilename( defaultConfig ) );
    }

    public File getWorldConfigFile( String world ) {
        return new File( getWorldConfigFilename( world ) );
    }

    public void onDisable() {
        this.info("(" + this.codename + ") disabled successfully.");
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

    private void copyFile( File sourceFile, File destinationFile ) throws IOException {
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destinationFile).getChannel();
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
    }

    /**
     * Initialise the default permissions file
     * Copy old naming convention permissions config or create an empty one.
     *
     * We use the following search order to obtain the "default" permissions set:
     * 1. config.yml
     * 2. <level-name>.yml copy to config.yml
     * 3. world.yml copy to config.yml
     * 4. create an empty config.yml
     */
    private void initDefaultPermissions() {
        File defaultFile = getDefaultConfigFile();
        if ( defaultFile.exists() ) {
            return;
        }

        try {
            // no config.yml check for <level-name>.yml
            String defaultWorld = getDefaultWorld();
            File defaultWorldFile = getWorldConfigFile( defaultWorld );
            if (defaultWorldFile.exists()) {
                if ( ! defaultConfig.equals( defaultWorld ) ) {
                    this.info("Default permissions created from default world '" + defaultWorld + "' permissions");
                    copyFile( defaultWorldFile, defaultFile );
                }
            }
            else {
                // no config.yml or <level-name>.yml check for world.yml
                File worldFile = getWorldConfigFile( "world" );
                if (worldFile.exists()) {
                    this.info("Default permissions created from 'world' permissions");
                    copyFile( worldFile, defaultFile );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (! defaultFile.exists() ) {
            // create an empty config
            this.info("No default permissions found initialising");
            com.nijiko.Misc.touch( defaultFile );
        }
    }

    public void setupPermissions() {
        initDefaultPermissions();
        Security = new Control(defaultConfig,this);
        Security.setDefaultWorld( defaultConfig ); // We use defaultConfig here for predicability
        Security.load();
    }

    public void onEnable() {
        instance = this;
        Server = this.getServer();
        this.description = this.getDescription();
        this.name = description.getName();
        this.directory = getDataFolder();

        // Start Registration
        directory.mkdirs();

        // Setup Permission
        setupPermissions();

        // Enabled
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.High, this);
        pluginManager.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.High, this);
        pluginManager.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.High, this);

        this.info("v" + description.getVersion() + " (" + codename + ") enabled");
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        Player player = null;
        String commandName = command.getName().toLowerCase();
        if (sender instanceof Player) {
            player = (Player)sender;

            Messaging.save(player);
        }

        if (commandName.equals("permissions")) {
            if (args.length < 1) {
                PluginDescriptionFile pdfFile = this.getDescription();
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
                sender.sendMessage("[" + pdfFile.getName() + "] version [" + pdfFile.getVersion() + "] (" + codename + ")  loaded");
            }

            if (2 == args.length && args[0].compareToIgnoreCase("-reload") == 0 ) {
                if (args[1].compareToIgnoreCase("all") == 0) {
                    if (player != null) {
                        if (Security.has(player, "permissions.reload")) {
                            Security.reload();
                            player.sendMessage(ChatColor.GRAY + "[Permissions] World Reloads completed.");
                            return true;
                        }
                        player.sendMessage(ChatColor.RED + "[Permissions] You lack the necessary permissions to perform this action.");
                        return true;
                    }

                    Security.reload();
                    sender.sendMessage("All world files reloaded.");
                    return true;
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

                        player.sendMessage(ChatColor.RED + "[Permissions] You lack the necessary permissions to permform this action.");
                        return true;
                    }

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
        return false;
    }
}
