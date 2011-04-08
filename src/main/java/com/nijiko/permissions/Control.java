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

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

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
    private Configuration config;

    
    public Control(Configuration config) {
        this.config = config;
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
    	if (this.Worlds.contains(world)) {
    		this.clearCache(world);
    		
    		synchronized (Worlds) {
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
        if(!this.Worlds.contains(world)) {
            this.load(world, new Configuration(new File(Permissions.instance.getDataFolder().getPath() + File.separator + world + ".yml")));
            log.info("Loaded world: " + world);
           return true;
        }

        return false;
    }
    
    public void forceLoadWorld(String world) {
        this.load(world, new Configuration(new File(Permissions.instance.getDataFolder().getPath() + File.separator + world + ".yml")));
    }

    public boolean checkWorld(String world) {
        if(this.Worlds.contains(world)) {
            return true;
        } else {
            return false;
        }
    }

    public void load() {
        if(this.defaultWorld == null ? "" != null : !this.defaultWorld.equals("")) {
            return;
        }

        this.load(this.defaultWorld, this.config);
    }

    @SuppressWarnings("unused")
	public void load(String world, Configuration config) {
        if (!(new File(Permissions.instance.getDataFolder().getPath() + File.separator + world + ".yml").exists())) {
            FileManager file = new FileManager(Permissions.instance.getDataFolder().getPath() + File.separator, world + ".yml", true);
        }


        config.load();

        this.Worlds.add(world);
        this.WorldConfiguration.put(world, config);

        if(!world.equals(this.defaultWorld)) {
            if(config.getString("plugin.permissions.copies", "") == null ? "" != null : !config.getString("plugin.permissions.copies", "").equals("")) {
                this.WorldInheritance.put(world, config.getString("plugin.permissions.copies", ""));
                return;
            }
            
            if (!(new File(Permissions.instance.getDataFolder().getPath() + File.separator + world + ".yml").exists())) {
            	this.WorldInheritance.put(world, defaultWorld);
            }
            
        }

        this.WorldBase.put(world, "");
        this.WorldCache.put(world, new HashMap<String, Boolean>());
        this.WorldUserPermissions.put(world, new HashMap<String, Set<String>>());
        this.WorldUserGroups.put(world, new HashMap<String, String>());
        this.WorldGroups.put(world, new HashMap<String, Set<String>>());
        this.WorldGroupsData.put(world, new HashMap<String, Object[]>());
        this.WorldGroupsInheritance.put(world, new HashMap<String, Set<String>>());

        // Grab the keys we are going to need
        List<String> userKeys = config.getKeys("users");
        List<String> groupKeys = config.getKeys("groups");

        // Permission set.
        Set<String> Permissions = new HashSet<String>();
        Set<String> Inheritance = new HashSet<String>();

        // Permission list
        List<String> permissions;
        List<String> inheritance;

        // Group
        String group;

        if (groupKeys != null) {
            for (String key : groupKeys) {
                Inheritance = new HashSet<String>();
                Permissions = new HashSet<String>();

                // Configuration
                inheritance = config.getStringList("groups." + key + ".inheritance", null);
                permissions = config.getStringList("groups." + key + ".permissions", null);
                boolean Default = config.getBoolean("groups." + key + ".default", false);
                String prefix = config.getString("groups." + key + ".info.prefix", null);
                String suffix = config.getString("groups." + key + ".info.suffix", null);
                boolean build = config.getBoolean("groups." + key + ".info.build", false);

                if (Default && (this.WorldBase.get(world) == null ? "" == null : this.WorldBase.get(world).equals(""))) {
                    this.WorldBase.put(world, key.toLowerCase());
                }

                if (inheritance.size() > 0) {
                    Inheritance.addAll(inheritance);
                }

                if (permissions.size() > 0) {
                    Permissions.addAll(permissions);
                }

                this.WorldGroups.get(world).put(key.toLowerCase(), Permissions);
                this.WorldGroupsData.get(world).put(key.toLowerCase(), new Object[]{key, prefix, suffix, build});

                if (Inheritance.size() > 0) {
                   this.WorldGroupsInheritance.get(world).put(key.toLowerCase(), Inheritance);
                }
            }
        }

        if (userKeys != null) {
            for (String key : userKeys) {
                Permissions = new HashSet<String>();

                // Configuration
                permissions = config.getStringList("users." + key + ".permissions", null);
                group = config.getString("users." + key + ".group");

                if (group != null) {
                    if (!group.isEmpty()) {
                        this.WorldUserGroups.get(world).put(key.toLowerCase(), group);
                    }
                } else {
                    this.WorldUserGroups.get(world).put(key.toLowerCase(), this.WorldBase.get(world));
                }

                if (permissions.size() > 0) {
                    Permissions.addAll(permissions);
                }

                this.WorldUserPermissions.get(world).put(key.toLowerCase(), Permissions);
            }
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
     *	System.out.println("The user can reload!");
     * } else {
     *	System.out.println("The user has no such permission!");
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

        // Fix to disable console users getting errors
        if (name == null && world == null)
        {
        	return true;
        }
        
        // Load if it isn't already
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if (this.WorldCache.get(world).containsKey(name + "," + permission)) {
            return this.WorldCache.get(world).get(name + "," + permission);
        }

        Map<String, Set<String>> UserPermissions = this.WorldUserPermissions.get(world);
        Map<String, String> UserGroups = this.WorldUserGroups.get(world);
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);
        Map<String, Set<String>> GroupsInheritance = this.WorldGroupsInheritance.get(world);
        Map<String, Boolean> Cached = this.WorldCache.get(world);
        String base = this.WorldBase.get(world);

        if (this.WorldUserPermissions.get(world).containsKey(name)) {
            Permissions = UserPermissions.get(name);
            group = UserGroups.get(name).toLowerCase();

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
                } else {
                    continue;
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
                } else {
                    return new HashSet<String>();
                }
            }

            return new HashSet<String>();
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
                Set<String> GottenInheritance = getInheritance(world, inherited);

                if (GroupPermissions == null) {
                    continue;
                }

                if (GroupPermissions.size() > 0) {
                    Permissions.addAll(GroupPermissions);
                }

                if (!Checked.contains(inherited)) {
                    Checked.add(inherited);
                    Object[] InheritedPermissions = getInheritancePermissions(world, Permissions, GottenInheritance, Checked, inherited);

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
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

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
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        name = name.toLowerCase();

        if (this.WorldUserPermissions.get(world).containsKey(name) && this.WorldUserGroups.get(world).containsKey(name)) {
            String group = (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldUserGroups.get(world).get(name).toLowerCase()))[0];
            return (group == null) ? null : group;
        } else {
            if (this.WorldBase.get(world).equals("")) {
                return null;
            } else {
                String group = (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[0];
                return (group == null) ? null : group;
            }
        }
    }

    public String getGroupPrefix(String world, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            String prefix = (String) ((Object[]) this.WorldGroupsData.get(world).get(group))[1];
            return (prefix == null) ? null : Messaging.parse(prefix);
        } else {
            return null;
        }
    }

    public String getGroupSuffix(String world, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            String suffix = (String) ((Object[]) this.WorldGroupsData.get(world).get(group))[2];
            return (suffix == null) ? null : Messaging.parse(suffix);
        } else {
            return null;
        }
    }

    public boolean canGroupBuild(String world, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            return (Boolean) ((Object[]) this.WorldGroupsData.get(world).get(group))[3];
        } else {
            if (this.WorldBase.get(world).equals("")) {
                return false;
            } else {
                return (Boolean) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[3];
            }
        }
    }

    public String[] getGroups(String world, String name) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String Group = (String) this.WorldUserGroups.get(world).get(name.toLowerCase());
        if (Group == null)
        {
        	Group = (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[0];
        }
        Set<String> Inherited = getInheritance(world, Group.toLowerCase());
        Inherited.add(Group.toLowerCase());

        return Inherited.toArray(new String[0]);
    }

    public void setCache(String world, Map<String, Boolean> Cache) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            this.WorldCache.put(world, Cache);
        }
    }
   
    public void setCacheItem(String world, String player, String permission, boolean data) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            this.WorldCache.get(world).put(player + "," + permission, data);
        }
    }

    public Map<String, Boolean> getCache(String world) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            return this.WorldCache.get(world);
        }

        return new HashMap<String, Boolean>();
    }

    public boolean getCacheItem(String world, String player, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.WorldInheritance.containsKey(world)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            if (this.WorldCache.get(world).containsKey(player + "," + permission)) {
                return this.WorldCache.get(world).get(player + "," + permission);
            }
        }

        return false;
    }

    public void removeCachedItem(String world, String player, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.WorldInheritance.containsKey(world)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            if (this.WorldCache.get(world).containsKey(player + "," + permission)) {
                this.WorldCache.get(world).remove(player + "," + permission);
            }
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
        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);
        }

        if(this.Worlds.contains(world)) {
            this.WorldCache.put(world, new HashMap<String, Boolean>());
        }
    }
    //Fixed functions by rcjrrjcr
    public void addGroupPermission(String world, String group, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("groups." + group + ".permissions", new LinkedList<String>());
        list.add(node);
        this.WorldConfiguration.get(world).setProperty("groups." + group + ".permissions", list);
        //MODIFICATION START
        Set<String> groupPerms = this.WorldGroups.get(world).get(group);
        if(groupPerms==null) groupPerms = new HashSet<String>();
        groupPerms.add(node);
        this.WorldGroups.get(world).put(group, groupPerms);
        //MODIFICATION END
    }
    
    public void removeGroupPermission(String world, String group, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("groups." + group + ".permissions", new LinkedList<String>());
        
        if(list.contains(node)) {
            list.remove(node);
        }
        
        this.WorldConfiguration.get(world).setProperty("groups." + group + ".permissions", list);
        //MODIFICATION START
        Set<String> groupPerms = this.WorldGroups.get(world).get(group);
        if(groupPerms==null) groupPerms = new HashSet<String>();
        groupPerms.remove(node);
        this.WorldGroups.get(world).put(group, groupPerms);
        //MODIFICATION END
    }
    
    public void addGroupInfo(String world, String group, String node, Object data) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).setProperty("groups." + group + ".info." + node, data);
        //MODIFICATION START
        Object[] groupData = this.WorldGroupsData.get(world).get(group);
        if(groupData == null) groupData = new Object[]{group,"","",true};
        if(data instanceof Boolean && node.equals("build")) groupData[3] = data;
        else if (data instanceof String)
        {
        	if(node.equals("prefix")) groupData[1]= data;
        	else if(node.equals("suffix")) groupData[2]= data;
        }
        this.WorldGroupsData.get(world).put(group, groupData);
        //MODIFICATION END
    }
    
    public void removeGroupInfo(String world, String group, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).removeProperty("groups." + group + ".info." + node);
        
        //MODIFICATION START
        Object[] groupData = this.WorldGroupsData.get(world).get(group);
        if(groupData == null) groupData = new Object[]{group,"","",true};
        if(node.equals("build")) groupData[3] = true;
        else if(node.equals("prefix")) groupData[1]= "";
        else if(node.equals("suffix")) groupData[2]= "";
        this.WorldGroupsData.get(world).put(group, groupData);
        //MODIFICATION END
    }
    
    public void addUserPermission(String world, String user, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("users." + user + ".permissions", new LinkedList<String>());
        list.add(node);		
        this.WorldConfiguration.get(world).setProperty("users." + user + ".permissions", list);
        
        //MODIFICATION START
        Set<String> userPerms = this.WorldUserPermissions.get(world).get(user);
        if(userPerms==null) userPerms = new HashSet<String>();
        userPerms.add(node);
        this.WorldGroups.get(world).put(user, userPerms);
        //MODIFICATION END
    }
    
    public void removeUserPermission(String world, String user, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("users." + user + ".permissions", new LinkedList<String>());
        
        if(list.contains(node)) {
            list.remove(node);
        }
        
        this.WorldConfiguration.get(world).setProperty("users." + user + ".permissions", list);
        
        //MODIFICATION START
        Set<String> userPerms = this.WorldUserPermissions.get(world).get(user);
        if(userPerms==null) userPerms = new HashSet<String>();
        userPerms.remove(node);
        this.WorldGroups.get(world).put(user, userPerms);
        //MODIFICATION END
    }
    //End of fixes by rcjrrjcr
    public void addUserInfo(String world, String user, String node, Object data) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).setProperty("users." + user + ".info." + node, data);
    }
    
    public void removeUserInfo(String world, String user, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).removeProperty("users." + user + ".info." + node);
    }
    
    public String getGroupPermissionString(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return (this.WorldConfiguration.get(world).getString("groups." + group + ".info." + permission) == null)
                ? "" : this.WorldConfiguration.get(world).getString("groups." + group + ".info." + permission);
    }
    
    public int getGroupPermissionInteger(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getInt("groups." + group + ".info." + permission, -1);
    }
    
    public boolean getGroupPermissionBoolean(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getBoolean("groups." + group + ".info." + permission, false);
    }
    
    public double getGroupPermissionDouble(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getDouble("groups." + group + ".info." + permission, -1.0);
    }
    
    public String getUserPermissionString(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return (this.WorldConfiguration.get(world).getString("users." + name + ".info." + permission) == null)
                ? "" : this.WorldConfiguration.get(world).getString("users." + name + ".info." + permission);
    }
    
    public int getUserPermissionInteger(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getInt("users." + name + ".info." + permission, -1);
    }
    
    public boolean getUserPermissionBoolean(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getBoolean("users." + name + ".info." + permission, false);
    }
    
    public double getUserPermissionDouble(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getDouble("users." + name + ".info." + permission, -1.0);
    }
    
    public String getPermissionString(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

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
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

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
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

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
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String group = this.getGroup(world, name);
        double userPermission = this.getUserPermissionDouble(world, name, permission);
        double userGroupPermission = -1.0;

        if (group != null) {
            userGroupPermission = this.getGroupPermissionDouble(world, group, permission);
        }

        if (userPermission != -1.0) {
            return userPermission;
        }

        return userGroupPermission;
    }
	//Addition by rcjrrjcr
    @Override
    public void save(String world)
    {
    	Configuration worldConfig = this.WorldConfiguration.get(world);
    	if(worldConfig!=null) worldConfig.save();
    }

	@Override
	public void saveAll() {
		for(Configuration worldConfig : this.WorldConfiguration.values())
		{
			worldConfig.save();
		}
	}
	//End of addition by rcjrrjcr
}
