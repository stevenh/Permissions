package com.nijiko.configuration;

import org.bukkit.util.config.Configuration;

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
 * Handles default configuration and loads data.
 * 
 * @author Nijiko
 */
public class ConfigurationHandler extends DefaultConfiguration {
    private Configuration config;

    public ConfigurationHandler(Configuration config) {
        this.config = config;
    }

    public void load() {
        this.permissionSystem = this.config.getString("plugin.permissions.system", this.permissionSystem);
    }
}
