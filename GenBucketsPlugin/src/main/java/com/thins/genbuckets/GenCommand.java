package com.thins.genbuckets;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class GenCommand implements CommandExecutor, TabCompleter {
    private final GenBucketsPlugin plugin;

    public GenCommand(GenBucketsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color("&cOnly players can open the GUI."));
                return true;
            }
            player.openInventory(plugin.getGuiManager().createMainMenu());
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "reload" -> {
                plugin.reloadLocalState();
                sender.sendMessage(color("&aGenBuckets config reloaded."));
            }
            case "list" -> sender.sendMessage(color("&dBuckets: &f" + plugin.getBucketRegistry().all().stream().map(GenBucketDefinition::id).collect(Collectors.joining("&7, &f"))));
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /gen info <bucket>"));
                    return true;
                }
                plugin.getBucketRegistry().get(args[1]).ifPresentOrElse(def -> {
                    sender.sendMessage(color("&dBucket: &f" + def.id()));
                    sender.sendMessage(color("&dPlace: &f" + def.placeMaterial().name()));
                    sender.sendMessage(color("&dMode: &f" + def.mode().name()));
                    sender.sendMessage(color("&dSlot: &f" + def.slot()));
                }, () -> sender.sendMessage(color("&cUnknown bucket.")));
            }
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(color("&cUsage: /gen give <player> <bucket> [amount]"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color("&cPlayer not found."));
                    return true;
                }
                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
                }
                plugin.getBucketRegistry().get(args[2]).ifPresentOrElse(def -> {
                    target.getInventory().addItem(plugin.getBucketRegistry().createItem(def, amount));
                    sender.sendMessage(color("&aGave &f" + target.getName() + " &ax" + amount + " &f" + def.id()));
                }, () -> sender.sendMessage(color("&cUnknown bucket.")));
            }
            case "debug" -> {
                plugin.setDebug(!plugin.isDebug());
                sender.sendMessage(color("&dDebug is now " + (plugin.isDebug() ? "&aON" : "&cOFF")));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&d&lGen Help"));
        sender.sendMessage(color("&7/gen &8- &fOpen the GUI"));
        sender.sendMessage(color("&7/gen help &8- &fShow this help"));
        sender.sendMessage(color("&7/gen reload &8- &fReload the plugin"));
        sender.sendMessage(color("&7/gen give <player> <bucket> [amount] &8- &fGive buckets"));
        sender.sendMessage(color("&7/gen list &8- &fList bucket IDs"));
        sender.sendMessage(color("&7/gen info <bucket> &8- &fShow bucket info"));
        sender.sendMessage(color("&7/gen debug &8- &fToggle debug"));
    }

    private String color(String s) {
        return BucketRegistry.color(s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("help", "reload", "give", "list", "info", "debug");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return null;
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) return plugin.getBucketRegistry().all().stream().map(GenBucketDefinition::id).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return plugin.getBucketRegistry().all().stream().map(GenBucketDefinition::id).toList();
        return new ArrayList<>();
    }
}
