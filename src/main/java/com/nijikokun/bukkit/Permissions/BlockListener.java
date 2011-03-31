package com.nijikokun.bukkit.Permissions;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockListener;
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
