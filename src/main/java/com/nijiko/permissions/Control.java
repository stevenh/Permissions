package com.nijiko.permissions;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.World;

import com.nijiko.Messaging;
import com.nijikokun.bukkit.Permissions.FileManager;
import com.nijikokun.bukkit.Permissions.Permissions;

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
 * iControl.java
 * Permission handler
 *
 * @author Nijiko
 */
public class Control extends PermissionHandler {

    public static final Logger log = Logger.getLogger("Minecraft");

    private List<String> Worlds = new LinkedList<String>();
    private Map<String, Configuration> WorldConfiguration = new HashMap<String, Configuration>();
    private Map<String, String> WorldBase = new HashMap<String, String>();
    private Map<String, String> WorldInheritance = new HashMap<String, String>();
    private Map<String, Map<String, Set<String>>> WorldUserPermissions = new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, String>> WorldUserGroups = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, Set<String>>> WorldGroups = new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, Object[]>> WorldGroupsData = new HashMap<String, Map<String, Object[]>>();
    private Map<String, Map<String, Set<String>>> WorldGroupsInheritance = new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, Boolean>> WorldCache = new HashMap<String, Map<String, Boolean>>();

    private String defaultWorld = "";
    private Permissions permissionsPlugin;

    public Control(String world,Permissions permissionsPlugin) {
        this.permissionsPlugin = permissionsPlugin;
        this.setDefaultWorld( world );
    }

    public void reload() {
        this.clearAllCache();

        final List<String> w = new LinkedList<String>(Worlds);
        Worlds = new LinkedList<String>();

        synchronized (w) {
            for (Iterator<String> it = w.iterator(); it.hasNext();) {
                String world = it.next();
                this.forceLoadWorld(world);
            }
        }
    }

    public boolean reload(String world) {
        if (this.checkWorld(world)) {
            this.clearCache(world);

            synchronized (this.Worlds) {
                this.Worlds.remove(world);
            }

            this.forceLoadWorld(world);
            return true;
        }
        return false;
    }

    public void setDefaultWorld(String world) {
        this.defaultWorld = world;
    }

    public boolean loadWorld(String world) {
        boolean loaded = this.loadWorld(world, false);

        String parentWorld = this.WorldInheritance.get(world);
        if( world.equals( parentWorld ) ) {
            return loaded;
        }

        return this.loadWorld(parentWorld, false) || loaded;
    }

    public void forceLoadWorld(String world) {
        this.loadWorld( world, true );
    }

    private boolean loadWorld(String world, Boolean reload) {
        if(reload || !this.checkWorld(world)) {
            this.load(world);
            return true;
        }

        return false;
    }

    public boolean checkWorld(String world) {
        return this.Worlds.contains(world);
    }

    public void load() {
        this.load(this.defaultWorld);
        for ( World world : this.permissionsPlugin.getServer().getWorlds() )
        {
            this.loadWorld( world.getName() );
        }
    }

    private Configuration loadWordConfig( String world ) {
        File worldCfgFile = this.permissionsPlugin.getWorldConfigFile( world );
        if (!worldCfgFile.exists()) {
            com.nijiko.Misc.touch( worldCfgFile );
        }
        Configuration config = new Configuration( worldCfgFile );
        config.load();

        return config;
    }

    @SuppressWarnings("unused")
    public void load(String world) {
        if( null == world || world.equals("")) {
            this.permissionsPlugin.warn("Load called with empty or null world name" );
            return;
        }

        Configuration config = this.loadWordConfig( world );

        this.Worlds.add(world);
        this.WorldConfiguration.put(world, config);

        if(!world.equals(this.defaultWorld)) {
            String copies = config.getString("plugin.permissions.copies", this.defaultWorld);
            if( !copies.equals("") ) {
                if ( copies.equals( this.defaultWorld ) ) {
                    this.permissionsPlugin.info("Using default permissions for world: " + world);
                }
                else {
                    this.permissionsPlugin.info("Using permissions from '" + copies + " for world: " + world);

                }

                this.WorldInheritance.put(world, copies);
                return;
            }
        }

        this.WorldBase.put(world, "");
        this.WorldCache.put(world, new HashMap<String, Boolean>());
        this.WorldUserPermissions.put(world, new HashMap<String, Set<String>>());
        this.WorldUserGroups.put(world, new HashMap<String, String>());
        this.WorldGroups.put(world, new HashMap<String, Set<String>>());
        this.WorldGroupsData.put(world, new HashMap<String, Object[]>());
        this.WorldGroupsInheritance.put(world, new HashMap<String, Set<String>>());
        this.WorldInheritance.put(world, world);

        // Grab the keys we are going to need
        List<String> userKeys = config.getKeys("users");
        List<String> groupKeys = config.getKeys("groups");

        // log.info("Grabbing group keys for world: " + directory.getPath() + File.separator + world + ".yml");
        // log.info("User keys for world: " + world + " - " + new ArrayList<String>(groupKeys).toString());
        String worldBase = this.WorldBase.get(world);
        if (groupKeys != null) {
            Map worldGroups = this.WorldGroups.get(world);
            Map worldGroupsData = this.WorldGroupsData.get(world);
            Map worldGroupsInheritance = this.WorldGroupsInheritance.get(world);
            for (String key : groupKeys) {
                String group = key.toLowerCase();
                List<String> inheritanceList = config.getStringList("groups." + key + ".inheritance", null);
                List<String> permissionsList = config.getStringList("groups." + key + ".permissions", null);
                String prefix = config.getString("groups." + key + ".info.prefix", null);
                String suffix = config.getString("groups." + key + ".info.suffix", null);
                boolean build = config.getBoolean("groups." + key + ".info.build", false);


                if (config.getBoolean("groups." + key + ".default", false) && ( null == worldBase || worldBase.equals(""))) {
                    worldBase = key.toLowerCase();
                    this.WorldBase.put(world, worldBase);
                }

                if (inheritanceList.size() > 0) {
                    worldGroupsInheritance.put(group, new HashSet<String>(inheritanceList));
                }

                worldGroups.put(group, new HashSet<String>(permissionsList));
                worldGroupsData.put(group, new Object[]{key, prefix, suffix, build});
            }
        }

        // log.info("Grabbing userkeys for world: " + directory.getPath() + File.separator + world + ".yml");
        // log.info("User keys for world: " + world + " - " + new ArrayList<String>(userKeys).toString());
        if (userKeys != null) {
            Map worldUserGroups = this.WorldUserGroups.get(world);
            Map worldPermissions = this.WorldUserPermissions.get(world);
            for (String key : userKeys) {
                // Configuration
                List<String> permissionsList = config.getStringList("users." + key + ".permissions", null);
                String group = config.getString("users." + key + ".group");
                String user = key.toLowerCase();
                worldUserGroups.put(user, ( null != group && !group.isEmpty() ) ? group : worldBase );
                worldPermissions.put(user, new HashSet<String>(permissionsList) );
            }
        }

        if ( world.equals( this.defaultWorld ) ) {
            this.permissionsPlugin.info("Loaded default permissions" );
        } else {
            this.permissionsPlugin.info("Loaded permissions for world: " + world);
        }
    }

    @SuppressWarnings("unused")
    private String toArrayListString(Collection<String> variable) {
        return new ArrayList<String>(variable).toString();
    }

    /**
     * Simple alias for permission method.
     * Easier to understand / recognize what it does and is checking for.
     *
     * @param player
     * @param permission
     * @return boolean
     */
    public boolean has(Player player, String permission) {
        return this.permission(player, permission);
    }

    /**
     * Checks to see if a player has permission to a specific tree node.
     * <br /><br />
     * Example usage:
     * <blockquote><pre>
     * boolean canReload = Plugin.Permissions.Security.permission(player, "permission.reload");
     * if(canReload) {
     *  System.out.println("The user can reload!");
     * } else {
     *  System.out.println("The user has no such permission!");
     * }
     * </pre></blockquote>
     *
     * @param player
     * @param permission
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean permission(Player player, String permission) {
        Set<String> Permissions = new HashSet<String>();
        Set<String> GroupPermissions = new HashSet<String>();
        Set<String> GroupInheritedPermissions = new HashSet<String>();
        String group = "";
        String name = player.getName().toLowerCase();
        String world = player.getWorld().getName();

        // log.info("Checking inside world: " + world);

        // Fix to disable console users getting errors
        if (name == null && world == null) {
            return true;
        }

        world = this.WorldInheritance.get(world);

        if (this.WorldCache.get(world).containsKey(name + "," + permission)) {
            // log.info("World contained cached node " + permission + ": " + world);
            return this.WorldCache.get(world).get(name + "," + permission);
        }

        Map<String, Set<String>> UserPermissions = this.WorldUserPermissions.get(world);
        Map<String, String> UserGroups = this.WorldUserGroups.get(world);
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);
        Map<String, Set<String>> GroupsInheritance = this.WorldGroupsInheritance.get(world);
        Map<String, Boolean> Cached = this.WorldCache.get(world);
        String base = this.WorldBase.get(world);

        // log.info("Checking for the node " + permission + " in the world: " + world);

        if (UserPermissions.containsKey(name)) {
            Permissions = UserPermissions.get(name);
            group = UserGroups.get(name).toLowerCase();

            // log.info("User group:" + group);
            // log.info("User Permissions: " + (new ArrayList<String>(Permissions)).toString());

            if (!Groups.isEmpty() || Groups != null) {
                if (Groups.containsKey(group)) {
                    GroupPermissions = Groups.get(group);
                }

                if (GroupsInheritance.containsKey(group)) {
                    GroupInheritedPermissions = getInheritancePermissions(world, group);
                }
            } else {
                Cached.put(name + "," + permission, false);
                return false;
            }

        } else {
            if ((base == null ? "" == null : base.equals(""))) {
                Cached.put(name + "," + permission, false);
                return false;
            }

            group = ((String) base).toLowerCase();

            if (!Groups.isEmpty() || Groups != null) {
                if (Groups.containsKey(group)) {
                    GroupPermissions = Groups.get(group);
                }

                if (GroupsInheritance.containsKey(group)) {
                    GroupInheritedPermissions = getInheritancePermissions(world, group);
                }
            } else {
                Cached.put(name + "," + permission, false);
                return false;
            }
        }

        StringTokenizer globalized = new StringTokenizer(permission, ".");

        if (GroupInheritedPermissions.size() > 0) {
            GroupPermissions.addAll(GroupInheritedPermissions);
        }

        // log.info("Group Permissions: " + (new ArrayList<String>(GroupPermissions)).toString());

        if (Permissions == null || GroupPermissions == null) {
            Cached.put(name + "," + permission, false);
            return false;
        }

        if(GroupPermissions.contains("-" + permission) || Permissions.contains("-" + permission)) {
            Cached.put(name + "," + permission, false);
            return false;
        }

        if (GroupPermissions.contains("*") || Permissions.contains("*")) {
            Cached.put(name + "," + permission, true);
            return true;
        }

        if (GroupPermissions.contains(permission) || Permissions.contains(permission)) {
            Cached.put(name + "," + permission, true);
            return true;
        }

        if (permission.contains(".")) {
            String setting = "";
            String node = "";

            while (globalized.hasMoreElements()) {
                setting += (String) globalized.nextToken() + ".";
                node = setting + "*";

                if (GroupPermissions.contains(node) || Permissions.contains(node)) {
                    Cached.put(name + "," + permission, true);
                    return true;
                }
            }
        }

        Cached.put(name + "," + permission, false);
        return false;
    }

    private Set<String> getInheritance(String world, String group) {
        if (this.WorldGroupsInheritance.containsKey(world)) {
            Map<String, Set<String>> WorldGroupInheritance = this.WorldGroupsInheritance.get(world);

            if(WorldGroupInheritance.containsKey(group)) {
                if (WorldGroupInheritance.size() > 0) {
                    return WorldGroupInheritance.get(group);
                }
                return new HashSet<String>();
            }
        }

        return new HashSet<String>();
    }

    @SuppressWarnings("unchecked")
    private Object[] getInheritancePermissions(String world, Set<String> Permissions, Set<String> Inheritance, Set<String> Checked, String group) {
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);

        group = group.toLowerCase();

        if (Inheritance.size() > 0) {
            for (String inherited : Inheritance) {
                inherited = inherited.toLowerCase();
                Set<String> GroupPermissions = Groups.get(inherited.toLowerCase());
                if (GroupPermissions == null) {
                    continue;
                }

                if (GroupPermissions.size() > 0) {
                    Permissions.addAll(GroupPermissions);
                }

                if (!Checked.contains(inherited)) {
                    Checked.add(inherited);
                    Object[] InheritedPermissions = getInheritancePermissions(world, Permissions, getInheritance(world, inherited), Checked, inherited);

                    if (((Set<String>) InheritedPermissions[0]).size() > 0) {
                        Permissions.addAll((Set<String>) InheritedPermissions[0]);
                    }
                }
            }
        } else {
            Set<String> GroupPermissions = Groups.get(group);

            if (GroupPermissions.size() > 0) {
                Permissions.addAll(GroupPermissions);
            }
        }

        return new Object[]{Permissions, Checked};
    }

    @SuppressWarnings("unchecked")
    private Set<String> getInheritancePermissions(String world, String group) {
        group = group.toLowerCase();
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);
        Set<String> Permissions = new HashSet<String>();
        Set<String> Inheritance = getInheritance(world, group);
        Set<String> Checked = new HashSet<String>();

        if (Inheritance.size() > 0 && !Inheritance.isEmpty()) {
            for (String inherited : Inheritance) {
                inherited = inherited.toLowerCase();
                Set<String> GroupPermissions = Groups.get(inherited);

                if (GroupPermissions == null) {
                    continue;
                }

                if (GroupPermissions.size() > 0) {
                    Permissions.addAll(GroupPermissions);
                }

                if (getInheritance(world, inherited).size() > 0 && !Checked.contains(inherited)) {
                    Checked.add(inherited);
                    Object[] InheritedPermissions = getInheritancePermissions(world, Permissions, getInheritance(world, inherited), Checked, inherited);

                    if (((Set<String>) InheritedPermissions[0]).size() > 0) {
                        Permissions.addAll((Set<String>) InheritedPermissions[0]);
                    }
                }
            }
        }

        return Permissions;
    }

    public boolean inGroup(String world, String name, String group) {
        world = this.WorldInheritance.get(world);

        name = name.toLowerCase();
        group = group.toLowerCase();

        if (this.WorldUserPermissions.get(world).containsKey(name)) {
            String Group = (String) this.WorldUserGroups.get(world).get(name);
            Set<String> Inherited = getInheritance(world, Group);

            if (Inherited.contains(group) || Group.equalsIgnoreCase(group)) {
                return true;
            }
        }

        return false;
    }

    public boolean inSingleGroup(String world, String name, String group) {
        this.loadWorld(world);

        name = name.toLowerCase();
        group = group.toLowerCase();

        if (this.WorldUserPermissions.get(world).containsKey(name)) {
            String Group = (String) this.WorldUserGroups.get(world).get(name);

            if (Group.equalsIgnoreCase(group)) {
                return true;
            }
        }

        return false;
    }

    public String getGroup(String world, String name) {
        world = this.WorldInheritance.get(world);
        name = name.toLowerCase();

        if (this.WorldUserPermissions.get(world).containsKey(name) && this.WorldUserGroups.get(world).containsKey(name)) {
            return (String)((Object[]) this.WorldGroupsData.get(world).get(this.WorldUserGroups.get(world).get(name).toLowerCase()))[0];
        }
        if (!this.WorldBase.get(world).equals("")) {
            return (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[0];
        }

        return null;
    }

    public String getGroupPrefix(String world, String group) {
        world = this.WorldInheritance.get(world);
        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            String prefix = (String) ((Object[]) this.WorldGroupsData.get(world).get(group))[1];
            return (prefix == null) ? null : Messaging.parse(prefix);
        }
        return null;
    }

    public String getGroupSuffix(String world, String group) {
        world = this.WorldInheritance.get(world);
        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            String suffix = (String) ((Object[]) this.WorldGroupsData.get(world).get(group))[2];
            return (suffix == null) ? null : Messaging.parse(suffix);
        }
        return null;
    }

    public boolean canGroupBuild(String world, String group) {
        world = this.WorldInheritance.get(world);
        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            return (Boolean) ((Object[]) this.WorldGroupsData.get(world).get(group))[3];
        }
        if (this.WorldBase.get(world).equals("")) {
            return false;
        }
        return (Boolean) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[3];
   }

    public String[] getGroups(String world, String name) {
        world = this.WorldInheritance.get(world);
        String Group = (String) this.WorldUserGroups.get(world).get(name.toLowerCase());
        if (Group == null) {
            Group = (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[0];
        }
        Set<String> Inherited = getInheritance(world, Group.toLowerCase());
        Inherited.add(Group.toLowerCase());

        return Inherited.toArray(new String[0]);
    }

    public void setCache(String world, Map<String, Boolean> Cache) {
        world = this.WorldInheritance.get(world);

        if(this.checkWorld(world)) {
            this.WorldCache.put(world, Cache);
        }
    }

    public void setCacheItem(String world, String player, String permission, boolean data) {
        world = this.WorldInheritance.get(world);

        if(this.checkWorld(world)) {
            this.WorldCache.get(world).put(player + "," + permission, data);
        }
    }

    public Map<String, Boolean> getCache(String world) {
        world = this.WorldInheritance.get(world);

        if(this.checkWorld(world)) {
            return this.WorldCache.get(world);
        }

        return new HashMap<String, Boolean>();
    }

    public boolean getCacheItem(String world, String player, String permission) {
        world = this.WorldInheritance.get(world);

        if(this.checkWorld(world)&& this.WorldCache.get(world).containsKey(player + "," + permission)) {
            return this.WorldCache.get(world).get(player + "," + permission);
        }

        return false;
    }

    public void removeCachedItem(String world, String player, String permission) {
        world = this.WorldInheritance.get(world);

        if(this.checkWorld(world) && this.WorldCache.get(world).containsKey(player + "," + permission)) {
            this.WorldCache.get(world).remove(player + "," + permission);
        }
    }

    public void clearCache() {
        this.WorldCache.put(this.defaultWorld, new HashMap<String, Boolean>());
    }

    public void clearAllCache() {
        for(String world : this.WorldCache.keySet()) {
            this.WorldCache.put(world, new HashMap<String, Boolean>());
        }
    }

    public void clearCache(String world) {
        world = this.WorldInheritance.get(world);
        if(this.checkWorld(world)) {
            this.WorldCache.put(world, new HashMap<String, Boolean>());
        }
    }

    public void addGroupPermission(String world, String group, String node) {
        world = this.WorldInheritance.get(world);

        List<String> list = this.WorldConfiguration.get(world).getStringList("groups." + group + ".permissions", new LinkedList<String>());
        list.add(node);
        this.WorldConfiguration.get(world).setProperty("groups." + group + ".permissions", list);
    }

    public void removeGroupPermission(String world, String group, String node) {
        world = this.WorldInheritance.get(world);

        List<String> list = this.WorldConfiguration.get(world).getStringList("groups." + group + ".permissions", new LinkedList<String>());

        if(list.contains(node)) {
            list.remove(node);
        }

        this.WorldConfiguration.get(world).setProperty("groups." + group + ".permissions", list);
    }

    public void addGroupInfo(String world, String group, String node, Object data) {
        world = this.WorldInheritance.get(world);

        this.WorldConfiguration.get(world).setProperty("groups." + group + ".info." + node, data);
    }

    public void removeGroupInfo(String world, String group, String node) {
        world = this.WorldInheritance.get(world);

        this.WorldConfiguration.get(world).removeProperty("groups." + group + ".info." + node);
    }

    public void addUserPermission(String world, String user, String node) {
        world = this.WorldInheritance.get(world);

        List<String> list = this.WorldConfiguration.get(world).getStringList("users." + user + ".permissions", new LinkedList<String>());
        list.add(node);
        this.WorldConfiguration.get(world).setProperty("users." + user + ".permissions", list);
    }

    public void removeUserPermission(String world, String user, String node) {
        world = this.WorldInheritance.get(world);

        List<String> list = this.WorldConfiguration.get(world).getStringList("users." + user + ".permissions", new LinkedList<String>());

        if(list.contains(node)) {
            list.remove(node);
        }

        this.WorldConfiguration.get(world).setProperty("users." + user + ".permissions", list);
    }

    public void addUserInfo(String world, String user, String node, Object data) {
        world = this.WorldInheritance.get(world);

        this.WorldConfiguration.get(world).setProperty("users." + user + ".info." + node, data);
    }

    public void removeUserInfo(String world, String user, String node) {
        world = this.WorldInheritance.get(world);

        this.WorldConfiguration.get(world).removeProperty("users." + user + ".info." + node);
    }

    public String getGroupPermissionString(String world, String group, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getString("groups." + group + ".info." + permission, "");
    }

    public int getGroupPermissionInteger(String world, String group, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getInt("groups." + group + ".info." + permission, -1);
    }

    public boolean getGroupPermissionBoolean(String world, String group, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getBoolean("groups." + group + ".info." + permission, false);
    }

    public double getGroupPermissionDouble(String world, String group, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getDouble("groups." + group + ".info." + permission, -1.0);
    }

    public String getUserPermissionString(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getString("users." + name + ".info." + permission, "");
    }

    public int getUserPermissionInteger(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getInt("users." + name + ".info." + permission, -1);
    }

    public boolean getUserPermissionBoolean(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getBoolean("users." + name + ".info." + permission, false);
    }

    public double getUserPermissionDouble(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        return this.WorldConfiguration.get(world).getDouble("users." + name + ".info." + permission, -1.0);
    }

    public String getPermissionString(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        String group = this.getGroup(world, name);
        String userPermission = this.getUserPermissionString(world, name, permission);
        String userGroupPermission = "";

        if (group != null) {
            userGroupPermission = this.getGroupPermissionString(world, group, permission);
        }

        if (!userPermission.equalsIgnoreCase("")) {
            return userPermission;
        }

        return userGroupPermission;
    }

    public boolean getPermissionBoolean(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);
        String group = this.getGroup(world, name);
        boolean userPermission = this.getUserPermissionBoolean(world, name, permission);
        boolean userGroupPermission = false;

        if (group != null) {
            userGroupPermission = this.getGroupPermissionBoolean(world, group, permission);
        }

        if (userPermission) {
            return userPermission;
        }

        return userGroupPermission;
    }

    @SuppressWarnings("null")
    public int getPermissionInteger(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        String group = this.getGroup(world, name);
        int userPermission = this.getUserPermissionInteger(world, name, permission);
        int userGroupPermission = -1;

        if (group != null || !group.isEmpty()) {
            userGroupPermission = this.getGroupPermissionInteger(world, group, permission);
        }

        if (userPermission != -1) {
            return userPermission;
        }

        return userGroupPermission;
    }

    public double getPermissionDouble(String world, String name, String permission) {
        world = this.WorldInheritance.get(world);

        String group = this.getGroup(world, name);
        double userPermission = this.getUserPermissionDouble(world, name, permission);
        double userGroupPermission = -1.0;

        if (group != null /*|| !group.isEmpty()*/) {
            userGroupPermission = this.getGroupPermissionDouble(world, group, permission);
        }

        if (userPermission != -1.0) {
            return userPermission;
        }

        return userGroupPermission;
    }
}
