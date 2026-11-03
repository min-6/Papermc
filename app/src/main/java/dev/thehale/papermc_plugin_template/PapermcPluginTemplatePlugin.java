// Copyright (c) 2023 Joseph Hale
// 
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package dev.thehale.papermc_plugin_template;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import dev.thehale.papermc_plugin_template.music.MinBgmLog;
import dev.thehale.papermc_plugin_template.music.RegionMusicCommand;
import dev.thehale.papermc_plugin_template.music.RegionMusicManager;

public class PapermcPluginTemplatePlugin extends JavaPlugin {

    public static PapermcPluginTemplatePlugin instance;
    public static Logger log;
    public final static String NAME = "PapermcPluginTemplate";
    private RegionMusicManager regionMusicManager;

    /**
     * Default constructor.
     * 
     * <p>Used solely by MockBukkit during unit tests.
     */
    public PapermcPluginTemplatePlugin() {
        super();
    }

    /**
     * Parameterized constructor.
     * 
     * <p>Used solely by MockBukkit during unit tests.
     */
    protected PapermcPluginTemplatePlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        instance = this;
        log = getLogger();
        logTemplateAttribution();

        regionMusicManager = new RegionMusicManager(this);
        getServer().getPluginManager().registerEvents(regionMusicManager, this);
        MinBgmLog.info(log, "Registered region tracking and music playback listener.");

        RegionMusicCommand regionMusicCommand = new RegionMusicCommand(this, regionMusicManager);
        PluginCommand command = getCommand("regionmusic");
        if (command == null) {
            MinBgmLog.severe(log, "The regionmusic command is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(regionMusicCommand);
        command.setTabCompleter(regionMusicCommand);
        MinBgmLog.info(log, "Registered /regionmusic command.");
        regionMusicManager.refreshOnlinePlayers();

        MinBgmLog.info(log, "Plugin enabled.");
        getServer().getConsoleSender().sendMessage(
                ChatColor.BLUE + "[min_bgm INFO] [Plugin]플러그인이 적용되었습니다" + ChatColor.RESET);
    }

    private void logTemplateAttribution() {
        // Please do *not* remove this attribution. It helps other hopeful plugin developers find this template.
        MinBgmLog.info(log, "[ATTRIBUTION] Powered by the PaperMC Plugin Template created by Joseph Hale (https://jhale.dev)");
        MinBgmLog.info(log, "[ATTRIBUTION] Copyright (c) 2021-2024. All rights reserved.");
        MinBgmLog.info(log, "[ATTRIBUTION] Licensed under the terms of the MPL-2.0 license.");
        MinBgmLog.info(log, "[ATTRIBUTION] Issues with the template can be reported at https://github.com/thehale/papermc-plugin-template/issues");
        MinBgmLog.info(log, "[ATTRIBUTION]");
        MinBgmLog.info(log, "[ATTRIBUTION] CREATE YOUR OWN PLUGIN: https://github.com/thehale/papermc-plugin-template");
    }

    @Override
    public void onDisable() {
        if (regionMusicManager != null) {
            regionMusicManager.stopAll();
        }
        if (log != null) {
            MinBgmLog.info(log, "Plugin disabled. Thanks for using " + NAME + "!");
        }
    }
}