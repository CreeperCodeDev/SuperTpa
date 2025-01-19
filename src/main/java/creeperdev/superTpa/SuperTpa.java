package creeperdev.superTpa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.command.ConsoleCommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;

import java.util.HashMap;
import java.util.Map;

public class SuperTpa extends JavaPlugin implements CommandExecutor, Listener {

    private Map<Player, Player> tpaRequests = new HashMap<>();
    private Map<String, String> messages = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        this.getCommand("tpa").setExecutor(this);
        this.getCommand("tpaccept").setExecutor(this);
        this.getCommand("tpdeny").setExecutor(this);
        this.getCommand("tphere").setExecutor(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);
    }

    private void loadMessages() {
        // 加載語言包中的訊息
        if (getConfig().contains("messages")) {
            for (String key : getConfig().getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, getConfig().getString("messages." + key));
            }
        }
    }

    private String getMessage(String key, String... placeholders) {
        String message = messages.getOrDefault(key, key);
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("%" + (i + 1), placeholders[i]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-players"));
            return false;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "tpa":
                if (args.length != 1) {
                    return false;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(getMessage("tpa-request-invalid-player"));
                    return false;
                }
                tpaRequests.put(target, player);

                // 使用TextComponent來顯示可以點擊的命令
                TextComponent acceptMessage = new TextComponent(getMessage("tpa-request-received", player.getName()));
                TextComponent acceptCommand = new TextComponent("/tpaccept ");
                acceptCommand.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                acceptCommand.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
                acceptMessage.addExtra(acceptCommand);

                TextComponent denyCommand = new TextComponent(" /tpdeny");
                denyCommand.setColor(net.md_5.bungee.api.ChatColor.RED);
                denyCommand.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
                acceptMessage.addExtra(denyCommand);

                target.spigot().sendMessage(acceptMessage); // 使用spigot發送消息，這樣可以顯示點擊的文字

                player.sendMessage(getMessage("tpa-request-sent", target.getName()));
                break;

            case "tpaccept":
                if (!tpaRequests.containsKey(player)) {
                    player.sendMessage(getMessage("tpa-no-pending-request"));
                    return false;
                }
                Player requester = tpaRequests.remove(player);
                requester.teleport(player.getLocation());
                player.sendMessage(getMessage("tpa-request-accepted", requester.getName()));
                requester.sendMessage(getMessage("tpa-request-accepted", player.getName()));
                break;

            case "tpdeny":
                if (!tpaRequests.containsKey(player)) {
                    player.sendMessage(getMessage("tpa-no-pending-request"));
                    return false;
                }
                tpaRequests.remove(player);
                player.sendMessage(getMessage("tpa-request-denied", player.getName()));
                break;

            case "tphere":
                if (args.length != 1) {
                    return false;
                }
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    player.sendMessage(getMessage("tpa-request-invalid-player"));
                    return false;
                }
                targetPlayer.teleport(player.getLocation());
                player.sendMessage(getMessage("tphere-success", targetPlayer.getName()));
                break;

            default:
                return false;
        }

        return true;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
