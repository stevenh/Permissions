package com.nijikokun.bukkit.Permissions;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockListener;
import com.nijikokun.bukkit.Permissions.Permissions;


public class Listener extends BlockListener {
	@SuppressWarnings("unused")
	private final Permissions plugin;
	
	public Listener(final Permissions plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
        final World world;
        
        world = player.getWorld();
		String[] groups = Permissions.Security.getGroups(world.getName(), player.getName());
		int build = 0;
		
		for (String group : groups) {
			if (!Permissions.Security.canGroupBuild(world.getName(), group)) {
				build++;
			}
		}
		
		if (build == groups.length) {
			event.setBuild(false);
			return;
		}
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		final Player player = event.getPlayer();
        final World world;
        
        world = player.getWorld();
		String[] groups = Permissions.Security.getGroups(world.getName(), player.getName());
		int build = 0;
		
		for (String group : groups) {
			if (!Permissions.Security.canGroupBuild(world.getName(), group)) {
				build++;
			}
		}
		
		if (build == groups.length) {
			event.setCancelled(true);
			return;
		}
	}
}
