package creeperdev.superTpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class SuperTpa extends JavaPlugin {
    private FileConfiguration langConfig;
    private HashMap<UUID, UUID> tpaRequests = new HashMap<>();
    private HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final int COOLDOWN_SECONDS = 30;

    @Override
    public void onEnable() {
        loadLangConfig();
        getCommand("tpa").setExecutor(this);
        getCommand("tpaccept").setExecutor(this);
        getCommand("tpdeny").setExecutor(this);
        getCommand("tphere").setExecutor(this);
    }

    private void loadLangConfig() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private String getLangString(String path) {
        return langConfig.getString(path, "Missing language string: " + path);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getLangString("only_player"));
            return true;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "tpa":
                handleTpaCommand(player, args);
                break;
            case "tpaccept":
                handleTpAcceptCommand(player);
                break;
            case "tpdeny":
                handleTpDenyCommand(player);
                break;
            case "tphere":
                handleTpHereCommand(player, args);
                break;
        }
        return true;
    }

    private void handleTpaCommand(Player sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(getLangString("usage_tpa"));
            return;
        }

        if (hasCooldown(sender)) {
            sender.sendMessage(getLangString("cooldown_message")
                    .replace("%seconds%", String.valueOf(getRemainingCooldown(sender))));
            return;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getLangString("player_not_found"));
            return;
        }

        if (target.equals(sender)) {
            sender.sendMessage(getLangString("cannot_teleport_self"));
            return;
        }

        tpaRequests.put(target.getUniqueId(), sender.getUniqueId());
        setCooldown(sender);

        sender.sendMessage(getLangString("request_sent")
                .replace("%player%", target.getName()));

        // 創建可點擊的接受/拒絕按鈕
        Component message = Component.text(getLangString("request_received")
                .replace("%player%", sender.getName()))
                .append(Component.newline())
                .append(Component.text(getLangString("click_accept"))
                        .color(TextColor.color(0x55FF55))
                        .clickEvent(ClickEvent.runCommand("/tpaccept")))
                .append(Component.text(" "))
                .append(Component.text(getLangString("click_deny"))
                        .color(TextColor.color(0xFF5555))
                        .clickEvent(ClickEvent.runCommand("/tpdeny")));

        target.sendMessage(message);
    }

    private void handleTpHereCommand(Player sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(getLangString("usage_tphere"));
            return;
        }

        if (hasCooldown(sender)) {
            sender.sendMessage(getLangString("cooldown_message")
                    .replace("%seconds%", String.valueOf(getRemainingCooldown(sender))));
            return;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(getLangString("player_not_found"));
            return;
        }

        if (target.equals(sender)) {
            sender.sendMessage(getLangString("cannot_teleport_self"));
            return;
        }

        tpaRequests.put(target.getUniqueId(), sender.getUniqueId());
        setCooldown(sender);

        sender.sendMessage(getLangString("request_here_sent")
                .replace("%player%", target.getName()));

        Component message = Component.text(getLangString("request_here_received")
                .replace("%player%", sender.getName()))
                .append(Component.newline())
                .append(Component.text(getLangString("click_accept"))
                        .color(TextColor.color(0x55FF55))
                        .clickEvent(ClickEvent.runCommand("/tpaccept")))
                .append(Component.text(" "))
                .append(Component.text(getLangString("click_deny"))
                        .color(TextColor.color(0xFF5555))
                        .clickEvent(ClickEvent.runCommand("/tpdeny")));

        target.sendMessage(message);
    }

    private void handleTpAcceptCommand(Player player) {
        UUID requesterId = tpaRequests.get(player.getUniqueId());
        if (requesterId == null) {
            player.sendMessage(getLangString("no_pending_request"));
            return;
        }

        Player requester = getServer().getPlayer(requesterId);
        if (requester == null) {
            player.sendMessage(getLangString("player_offline"));
            tpaRequests.remove(player.getUniqueId());
            return;
        }

        requester.teleport(player.getLocation());
        requester.sendMessage(getLangString("teleport_success"));
        player.sendMessage(getLangString("request_accepted"));
        tpaRequests.remove(player.getUniqueId());
    }

    private void handleTpDenyCommand(Player player) {
        UUID requesterId = tpaRequests.get(player.getUniqueId());
        if (requesterId == null) {
            player.sendMessage(getLangString("no_pending_request"));
            return;
        }

        Player requester = getServer().getPlayer(requesterId);
        if (requester != null) {
            requester.sendMessage(getLangString("request_denied"));
        }

        player.sendMessage(getLangString("request_denied_sender"));
        tpaRequests.remove(player.getUniqueId());
    }

    private boolean hasCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        return System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < COOLDOWN_SECONDS * 1000;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private long getRemainingCooldown(Player player) {
        long timeElapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
        return (COOLDOWN_SECONDS * 1000 - timeElapsed) / 1000;
    }
}
