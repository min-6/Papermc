package dev.thehale.papermc_plugin_template.music;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionMusicCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "papermcplugintemplate.regionmusic";
    private final Logger logger;
    private final RegionMusicManager manager;

    public RegionMusicCommand(JavaPlugin plugin, RegionMusicManager manager) {
        this.logger = plugin.getLogger();
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            warn(sender, "permission denied for /" + label + " " + String.join(" ", args));
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            return true;
        }
        if (!(sender instanceof Player)) {
            warn(sender, "console cannot manage music regions");
            sender.sendMessage(ChatColor.RED + "게임 안에서 플레이어가 실행해야 합니다.");
            return true;
        }
        if (args.length == 0) {
            warn(sender, "missing subcommand for /" + label);
            sendUsage(sender, label);
            return true;
        }

        Player player = (Player) sender;
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add":
                addRegion(player, label, args);
                break;
            case "song":
                setSong(player, args);
                break;
            case "move":
                moveRegion(player, args);
                break;
            case "delete":
                deleteRegion(player, args);
                break;
            default:
                warn(sender, "unknown subcommand for /" + label + ": " + args[0]);
                sendUsage(sender, label);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            candidates.addAll(Arrays.asList("add", "song", "move", "delete"));
        } else if (args.length == 2 && isRegionNameCommand(args[0])) {
            candidates.addAll(manager.getRegionNames());
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
    }

    private void addRegion(Player player, String label, String[] args) {
        if (args.length != 6) {
            warn(player, "invalid add syntax");
            sendUsage(player, label);
            return;
        }
        Integer[] coordinates = parseCoordinates(args, 1);
        if (coordinates == null) {
            warn(player, "invalid coordinates for add");
            player.sendMessage(ChatColor.RED + "좌표는 정수로 입력해야 합니다.");
            return;
        }
        String name = args[5];
        if (!RegionMusicManager.isValidRegionName(name)) {
            warn(player, "invalid region name for add: " + name);
            player.sendMessage(ChatColor.RED + "영역 이름은 영문자, 숫자, '_' 또는 '-'로 1~32자여야 합니다.");
            return;
        }
        if (manager.addRegion(player, name, coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
            player.sendMessage(ChatColor.GREEN + "현재 월드에 영역 '" + name + "'을 저장했습니다.");
        } else {
            player.sendMessage(ChatColor.RED + "영역이 이미 있거나 저장에 실패했습니다.");
        }
    }

    private void setSong(Player player, String[] args) {
        if (args.length != 3) {
            warn(player, "invalid song syntax");
            player.sendMessage(ChatColor.YELLOW + "사용법: /regionmusic song <name> <namespace:sound>");
            return;
        }
        if (NamespacedKey.fromString(args[2]) == null) {
            warn(player, "invalid sound key for region '" + args[1] + "': " + args[2]);
            player.sendMessage(ChatColor.RED + "사운드 키 형식이 올바르지 않습니다. 예: mypack:music.forest");
            return;
        }
        if (manager.setSong(args[1], args[2])) {
            player.sendMessage(ChatColor.GREEN + "영역 '" + args[1] + "'의 노래를 설정했습니다.");
        } else {
            player.sendMessage(ChatColor.RED + "해당 영역을 찾지 못했거나 파일 저장에 실패했습니다.");
        }
    }

    private void moveRegion(Player player, String[] args) {
        if (args.length != 6) {
            warn(player, "invalid move syntax");
            player.sendMessage(ChatColor.YELLOW + "사용법: /regionmusic move <name> <x1> <z1> <x2> <z2>");
            return;
        }
        Integer[] coordinates = parseCoordinates(args, 2);
        if (coordinates == null) {
            warn(player, "invalid coordinates for move of region '" + args[1] + "'");
            player.sendMessage(ChatColor.RED + "좌표는 정수로 입력해야 합니다.");
            return;
        }
        if (manager.moveRegion(args[1], coordinates[0], coordinates[1], coordinates[2], coordinates[3])) {
            player.sendMessage(ChatColor.GREEN + "영역 '" + args[1] + "'의 위치를 변경했습니다.");
        } else {
            player.sendMessage(ChatColor.RED + "해당 영역을 찾지 못했거나 파일 저장에 실패했습니다.");
        }
    }

    private void deleteRegion(Player player, String[] args) {
        if (args.length != 2) {
            warn(player, "invalid delete syntax");
            player.sendMessage(ChatColor.YELLOW + "사용법: /regionmusic delete <name>");
            return;
        }
        if (manager.deleteRegion(args[1])) {
            player.sendMessage(ChatColor.GREEN + "영역 '" + args[1] + "'을 삭제했습니다.");
        } else {
            player.sendMessage(ChatColor.RED + "해당 영역을 찾지 못했거나 파일 저장에 실패했습니다.");
        }
    }

    private Integer[] parseCoordinates(String[] args, int startIndex) {
        try {
            return new Integer[] {
                    Integer.valueOf(args[startIndex]),
                    Integer.valueOf(args[startIndex + 1]),
                    Integer.valueOf(args[startIndex + 2]),
                    Integer.valueOf(args[startIndex + 3])
            };
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isRegionNameCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("song") || normalized.equals("move") || normalized.equals("delete");
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "영역 추가: /" + label + " add <x1> <z1> <x2> <z2> <name>");
        sender.sendMessage(ChatColor.YELLOW + "노래 지정: /" + label + " song <name> <namespace:sound>");
        sender.sendMessage(ChatColor.YELLOW + "영역 변경: /" + label + " move <name> <x1> <z1> <x2> <z2>");
        sender.sendMessage(ChatColor.YELLOW + "영역 삭제: /" + label + " delete <name>");
    }

    private void warn(CommandSender sender, String message) {
        MinBgmLog.warning(logger, "Command by '" + sender.getName() + "': " + message + ".");
    }
}