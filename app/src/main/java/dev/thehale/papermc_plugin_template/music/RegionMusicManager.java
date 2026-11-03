package dev.thehale.papermc_plugin_template.music;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RegionMusicManager implements Listener {

    private final JavaPlugin plugin;
    private final File storageFile;
    private final Map<String, MusicRegion> regions = new LinkedHashMap<>();
    private final Map<UUID, Playback> activePlayback = new HashMap<>();

    public RegionMusicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "regions.yml");
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            MinBgmLog.severe(plugin.getLogger(), "Could not create plugin data folder: " + plugin.getDataFolder());
        }
        loadRegions();
    }

    public boolean addRegion(Player player, String name, int x1, int z1, int x2, int z2) {
        if (!isValidRegionName(name)) {
            MinBgmLog.warning(plugin.getLogger(), "Rejected region with invalid name: " + name);
            return false;
        }

        String key = normalizeName(name);
        if (regions.containsKey(key)) {
            MinBgmLog.warning(plugin.getLogger(), "Rejected duplicate region name: " + name);
            return false;
        }

        MusicRegion region = new MusicRegion(name, player.getWorld().getName(), x1, z1, x2, z2, null);
        regions.put(key, region);
        if (!saveRegions()) {
            regions.remove(key);
            return false;
        }
        MinBgmLog.info(plugin.getLogger(), "Added region '" + name + "' in world '"
            + region.worldName + "' with X=" + region.minX + ".." + region.maxX
            + ", Z=" + region.minZ + ".." + region.maxZ + ".");
        refreshOnlinePlayers();
        return true;
    }

    public boolean setSong(String name, String soundKey) {
        MusicRegion region = regions.get(normalizeName(name));
        NamespacedKey parsedKey = NamespacedKey.fromString(soundKey);
        if (region == null || parsedKey == null) {
            MinBgmLog.warning(plugin.getLogger(), "Could not assign sound '" + soundKey
                    + "' to region '" + name + "': region or sound key is invalid.");
            return false;
        }

        String oldSoundKey = region.soundKey;
        region.soundKey = parsedKey.toString();
        if (!saveRegions()) {
            region.soundKey = oldSoundKey;
            return false;
        }
        MinBgmLog.info(plugin.getLogger(), "Set sound '" + region.soundKey + "' for region '"
            + region.name + "'.");
        refreshOnlinePlayers();
        return true;
    }

    public boolean moveRegion(String name, int x1, int z1, int x2, int z2) {
        String key = normalizeName(name);
        MusicRegion oldRegion = regions.get(key);
        if (oldRegion == null) {
            MinBgmLog.warning(plugin.getLogger(), "Could not move missing region '" + name + "'.");
            return false;
        }

        MusicRegion movedRegion = new MusicRegion(oldRegion.name, oldRegion.worldName,
                x1, z1, x2, z2, oldRegion.soundKey);
        regions.put(key, movedRegion);
        if (!saveRegions()) {
            regions.put(key, oldRegion);
            return false;
        }
        MinBgmLog.info(plugin.getLogger(), "Moved region '" + movedRegion.name + "' to X="
            + movedRegion.minX + ".." + movedRegion.maxX + ", Z="
            + movedRegion.minZ + ".." + movedRegion.maxZ + ".");
        refreshOnlinePlayers();
        return true;
    }

    public boolean deleteRegion(String name) {
        String key = normalizeName(name);
        MusicRegion removedRegion = regions.remove(key);
        if (removedRegion == null) {
            MinBgmLog.warning(plugin.getLogger(), "Could not delete missing region '" + name + "'.");
            return false;
        }

        if (!saveRegions()) {
            regions.put(key, removedRegion);
            return false;
        }
        MinBgmLog.info(plugin.getLogger(), "Deleted region '" + removedRegion.name + "'.");
        refreshOnlinePlayers();
        return true;
    }

    public List<String> getRegionNames() {
        List<String> names = new ArrayList<>();
        for (MusicRegion region : regions.values()) {
            names.add(region.name);
        }
        names.sort(Comparator.comparing(String::toLowerCase));
        return names;
    }

    public static boolean isValidRegionName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,32}");
    }

    public void refreshOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    public void stopAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            stopPlayback(player);
        }
        activePlayback.clear();
        MinBgmLog.info(plugin.getLogger(), "Stopped active music for online players during plugin shutdown.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        refreshPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        refreshPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        refreshPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Playback playback = activePlayback.remove(event.getPlayer().getUniqueId());
        if (playback != null) {
            MinBgmLog.info(plugin.getLogger(), "Cleared music state for disconnecting player '"
                    + event.getPlayer().getName() + "'.");
        }
    }

    private void refreshPlayer(Player player) {
        MusicRegion region = findRegion(player);
        String regionName = region == null ? null : normalizeName(region.name);
        String soundKey = region == null ? null : region.soundKey;
        UUID playerId = player.getUniqueId();
        Playback previous = activePlayback.get(playerId);

        if (previous != null && Objects.equals(previous.regionName, regionName)
                && Objects.equals(previous.soundKey, soundKey)) {
            return;
        }

        if (previous != null) {
            stopPlayback(player);
        }
        if (soundKey != null) {
            player.playSound(player.getLocation(), soundKey, SoundCategory.MUSIC, 1.0F, 1.0F);
            MinBgmLog.info(plugin.getLogger(), "Requested sound '" + soundKey + "' for player '"
                    + player.getName() + "' in region '" + region.name + "'.");
        } else if (region != null) {
            MinBgmLog.info(plugin.getLogger(), "Player '" + player.getName() + "' entered region '"
                    + region.name + "', but no sound is assigned.");
        } else if (previous != null) {
            MinBgmLog.info(plugin.getLogger(), "Player '" + player.getName() + "' left music region '"
                    + previous.regionName + "'.");
        }

        if (regionName == null && soundKey == null) {
            activePlayback.remove(playerId);
        } else {
            activePlayback.put(playerId, new Playback(regionName, soundKey));
        }
    }

    private void stopPlayback(Player player) {
        Playback playback = activePlayback.remove(player.getUniqueId());
        if (playback != null && playback.soundKey != null) {
            player.stopSound(playback.soundKey, SoundCategory.MUSIC);
            MinBgmLog.info(plugin.getLogger(), "Requested stop for sound '" + playback.soundKey
                    + "' for player '" + player.getName() + "'.");
        }
    }

    private MusicRegion findRegion(Player player) {
        Location location = player.getLocation();
        MusicRegion bestMatch = null;
        long bestArea = Long.MAX_VALUE;

        for (MusicRegion region : regions.values()) {
            if (region.contains(location) && region.area() < bestArea) {
                bestMatch = region;
                bestArea = region.area();
            }
        }
        return bestMatch;
    }

    private void loadRegions() {
        if (!storageFile.exists()) {
            MinBgmLog.info(plugin.getLogger(), "No saved regions file found; starting with no regions.");
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection section = configuration.getConfigurationSection("regions");
        if (section == null) {
            MinBgmLog.warning(plugin.getLogger(), "Saved regions file has no 'regions' section: " + storageFile);
            return;
        }

        int loadedCount = 0;
        for (String key : section.getKeys(false)) {
            String path = "regions." + key;
            String name = section.getString(key + ".name", key);
            String worldName = section.getString(key + ".world", "");
            if (!isValidRegionName(name) || worldName.isEmpty()) {
                MinBgmLog.warning(plugin.getLogger(), "Skipping invalid music region: " + key);
                continue;
            }

            regions.put(normalizeName(name), new MusicRegion(name, worldName,
                    section.getInt(key + ".x1"), section.getInt(key + ".z1"),
                    section.getInt(key + ".x2"), section.getInt(key + ".z2"),
                    section.getString(key + ".sound")));
            loadedCount++;
        }
        MinBgmLog.info(plugin.getLogger(), "Loaded " + loadedCount + " region(s) from " + storageFile + ".");
    }

    private boolean saveRegions() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<String, MusicRegion> entry : regions.entrySet()) {
            MusicRegion region = entry.getValue();
            String path = "regions." + entry.getKey();
            configuration.set(path + ".name", region.name);
            configuration.set(path + ".world", region.worldName);
            configuration.set(path + ".x1", region.minX);
            configuration.set(path + ".z1", region.minZ);
            configuration.set(path + ".x2", region.maxX);
            configuration.set(path + ".z2", region.maxZ);
            configuration.set(path + ".sound", region.soundKey);
        }

        try {
            configuration.save(storageFile);
            return true;
        } catch (IOException exception) {
            MinBgmLog.severe(plugin.getLogger(), "Could not save music regions to " + storageFile + ".", exception);
            return false;
        }
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static final class MusicRegion {
        private final String name;
        private final String worldName;
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private String soundKey;

        private MusicRegion(String name, String worldName, int x1, int z1, int x2, int z2, String soundKey) {
            this.name = name;
            this.worldName = worldName;
            this.minX = Math.min(x1, x2);
            this.maxX = Math.max(x1, x2);
            this.minZ = Math.min(z1, z2);
            this.maxZ = Math.max(z1, z2);
            this.soundKey = soundKey;
        }

        private boolean contains(Location location) {
            return location.getWorld() != null && worldName.equals(location.getWorld().getName())
                    && location.getBlockX() >= minX && location.getBlockX() <= maxX
                    && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
        }

        private long area() {
            long width = (long) maxX - minX + 1;
            long depth = (long) maxZ - minZ + 1;
            try {
                return Math.multiplyExact(width, depth);
            } catch (ArithmeticException exception) {
                return Long.MAX_VALUE;
            }
        }
    }

    private static final class Playback {
        private final String regionName;
        private final String soundKey;

        private Playback(String regionName, String soundKey) {
            this.regionName = regionName;
            this.soundKey = soundKey;
        }
    }
}