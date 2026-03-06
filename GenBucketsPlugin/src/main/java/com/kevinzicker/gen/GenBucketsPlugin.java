package com.kevinzicker.gen;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public final class GenBucketsPlugin extends JavaPlugin implements Listener, TabCompleter {
    private final Map<String, BucketDefinition> buckets = new LinkedHashMap<>();
    private final Set<UUID> debugPlayers = new HashSet<>();

    private NamespacedKey bucketKey;
    private String guiTitle;
    private int guiRows;
    private Material fillerMaterial;
    private String fillerName;
    private Material baseItemMaterial;
    private int horizontalLength;
    private int minY;
    private int maxY;
    private long delayTicks;
    private boolean chainGens;
    private boolean preventDoubleAnchors;
    private boolean replaceAnchorOnEarlyStop;
    private Material anchorBlock;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        bucketKey = new NamespacedKey(this, "bucket_id");
        reloadPlugin();
        Objects.requireNonNull(getCommand("gen")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void reloadPlugin() {
        reloadConfig();
        buckets.clear();

        guiTitle = color(getConfig().getString("settings.gui.title", "&8Gen Buckets"));
        guiRows = Math.max(1, Math.min(6, getConfig().getInt("settings.gui.rows", 3)));
        fillerMaterial = match(getConfig().getString("settings.gui.filler", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        fillerName = color(getConfig().getString("settings.gui.filler_name", " "));
        baseItemMaterial = match(getConfig().getString("items.base.material", "LAVA_BUCKET"), Material.LAVA_BUCKET);
        horizontalLength = Math.max(1, getConfig().getInt("settings.horizontal_length", 16));
        minY = Math.max(0, getConfig().getInt("settings.min_y", 1));
        maxY = Math.min(319, getConfig().getInt("settings.max_y", 255));
        delayTicks = Math.max(1, getConfig().getLong("settings.delay_ticks", 2));
        anchorBlock = match(getConfig().getString("settings.anchor_block", "WHITE_WOOL"), Material.WHITE_WOOL);
        chainGens = getConfig().getBoolean("settings.chain_gens", true);
        preventDoubleAnchors = getConfig().getBoolean("settings.prevent_double_anchors", true);
        replaceAnchorOnEarlyStop = getConfig().getBoolean("settings.replace_anchor_on_early_stop", true);

        ConfigurationSection section = getConfig().getConfigurationSection("buckets");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) continue;
                BucketDefinition def = BucketDefinition.fromSection(key, child);
                buckets.put(key.toLowerCase(Locale.ROOT), def);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) && args.length == 0) {
            sender.sendMessage(color("&cUse /gen help from console."));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color("&cConsole cannot open the GUI."));
                return true;
            }
            if (!sender.hasPermission("gen.use")) {
                sender.sendMessage(color("&cNo permission."));
                return true;
            }
            openGui(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> {
                sendHelp(sender);
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("gen.reload")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                reloadPlugin();
                sender.sendMessage(color("&aGenBuckets reloaded."));
                return true;
            }
            case "list" -> {
                if (!sender.hasPermission("gen.list")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                sender.sendMessage(color("&dAvailable buckets: &f" + String.join("&7, &f", buckets.keySet())));
                return true;
            }
            case "info" -> {
                if (!sender.hasPermission("gen.info")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /gen info <bucket>"));
                    return true;
                }
                BucketDefinition def = buckets.get(args[1].toLowerCase(Locale.ROOT));
                if (def == null) {
                    sender.sendMessage(color("&cUnknown bucket."));
                    return true;
                }
                sender.sendMessage(color("&dBucket: &f" + def.id()));
                sender.sendMessage(color("&dPlace: &f" + def.place()));
                sender.sendMessage(color("&dMode: &f" + def.mode()));
                sender.sendMessage(color("&dEnabled: &f" + def.enabled()));
                return true;
            }
            case "give" -> {
                if (!sender.hasPermission("gen.give")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(color("&cUsage: /gen give <player> <bucket> [amount]"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color("&cPlayer not found."));
                    return true;
                }
                BucketDefinition def = buckets.get(args[2].toLowerCase(Locale.ROOT));
                if (def == null) {
                    sender.sendMessage(color("&cUnknown bucket."));
                    return true;
                }
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Math.max(1, Integer.parseInt(args[3]));
                    } catch (NumberFormatException ignored) {}
                }
                target.getInventory().addItem(createBucketItem(def, amount));
                sender.sendMessage(color("&aGave &f" + amount + " &aof &f" + def.id() + " &ato &f" + target.getName() + "&a."));
                return true;
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(color("&cConsole cannot toggle player debug mode."));
                    return true;
                }
                if (!sender.hasPermission("gen.debug")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                if (debugPlayers.contains(player.getUniqueId())) {
                    debugPlayers.remove(player.getUniqueId());
                    player.sendMessage(color("&cGen debug disabled."));
                } else {
                    debugPlayers.add(player.getUniqueId());
                    player.sendMessage(color("&aGen debug enabled."));
                }
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&d&lGen Commands"));
        sender.sendMessage(color("&7/gen &8- &fOpen the gen bucket GUI"));
        sender.sendMessage(color("&7/gen help &8- &fShow this help"));
        if (sender.hasPermission("gen.reload")) sender.sendMessage(color("&7/gen reload &8- &fReload the config"));
        if (sender.hasPermission("gen.give")) sender.sendMessage(color("&7/gen give <player> <bucket> [amount] &8- &fGive buckets"));
        if (sender.hasPermission("gen.list")) sender.sendMessage(color("&7/gen list &8- &fList bucket IDs"));
        if (sender.hasPermission("gen.info")) sender.sendMessage(color("&7/gen info <bucket> &8- &fShow bucket info"));
        if (sender.hasPermission("gen.debug")) sender.sendMessage(color("&7/gen debug &8- &fToggle debug"));
    }

    private void openGui(Player player) {
        Inventory inventory = Bukkit.createInventory(null, guiRows * 9, guiTitle);
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(fillerName);
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        for (BucketDefinition def : buckets.values()) {
            if (!def.enabled()) continue;
            if (def.slot() >= 0 && def.slot() < inventory.getSize()) {
                inventory.setItem(def.slot(), createBucketItem(def, 1));
            }
        }
        player.openInventory(inventory);
    }

    private ItemStack createBucketItem(BucketDefinition def, int amount) {
        ItemStack item = new ItemStack(baseItemMaterial, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(def.name()));
            meta.setLore(def.lore().stream().map(this::color).collect(Collectors.toList()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(bucketKey, PersistentDataType.STRING, def.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle()).equals(ChatColor.stripColor(guiTitle))) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String id = getBucketId(clicked);
        if (id == null) return;
        BucketDefinition def = buckets.get(id.toLowerCase(Locale.ROOT));
        if (def == null) return;
        player.getInventory().addItem(createBucketItem(def, 1));
        player.sendMessage(color("&aGiven yourself &f" + def.id() + "&a."));
        player.closeInventory();
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;
        String id = getBucketId(item);
        if (id == null) return;

        BucketDefinition def = buckets.get(id.toLowerCase(Locale.ROOT));
        if (def == null || !def.enabled()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        BlockFace face = event.getBlockFace();
        if (face == null) return;

        Block start = findStartBlock(clicked, face, def);
        if (start == null) {
            debug(event.getPlayer(), "No valid start block found.");
            event.setCancelled(true);
            return;
        }

        if (preventDoubleAnchors && hasNearbyAnchor(start)) {
            debug(event.getPlayer(), "Stopped by nearby anchor.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        start.setType(anchorBlock, false);
        if (event.getHand() != null) {
            removeOne(event.getPlayer(), event.getHand(), item);
        }
        startGeneration(event.getPlayer(), start, face, def);
    }

    private void removeOne(Player player, org.bukkit.inventory.EquipmentSlot hand, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            if (hand == org.bukkit.inventory.EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
        }
    }

    private Block findStartBlock(Block clicked, BlockFace face, BucketDefinition def) {
        Block current = clicked.getRelative(face);
        BlockFace advanceFace = face;
        if (!def.mode().isHorizontal()) {
            advanceFace = def.mode() == BucketMode.VERTICAL_DOWN ? BlockFace.DOWN : BlockFace.UP;
            if (face != BlockFace.UP && face != BlockFace.DOWN) {
                current = clicked.getRelative(BlockFace.UP);
            }
        }

        for (int i = 0; i < 64; i++) {
            Material type = current.getType();
            if (type.isAir()) return current;
            if (chainGens && (type == anchorBlock || type == def.place())) {
                current = current.getRelative(advanceFace);
                continue;
            }
            return null;
        }
        return null;
    }

    private boolean hasNearbyAnchor(Block block) {
        if (block.getType() == anchorBlock) return true;
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (block.getRelative(face).getType() == anchorBlock) return true;
        }
        return false;
    }

    private void startGeneration(Player player, Block anchor, BlockFace clickedFace, BucketDefinition def) {
        final BlockFace direction;
        if (def.mode() == BucketMode.VERTICAL_DOWN) direction = BlockFace.DOWN;
        else if (def.mode() == BucketMode.VERTICAL_UP) direction = BlockFace.UP;
        else direction = clickedFace;

        new BukkitRunnable() {
            Block current = anchor;
            int placed = 0;

            @Override
            public void run() {
                Block next = current.getRelative(direction);

                if (def.mode().isHorizontal() && placed >= horizontalLength) {
                    finish(anchor, def.place());
                    cancel();
                    return;
                }
                if (!def.mode().isHorizontal()) {
                    int nextY = next.getY();
                    if (nextY < minY || nextY > maxY) {
                        finish(anchor, def.place());
                        cancel();
                        return;
                    }
                }

                Material nextType = next.getType();
                if (nextType.isAir()) {
                    next.setType(def.place(), false);
                    current = next;
                    placed++;
                    return;
                }

                if (chainGens && (nextType == anchorBlock || nextType == def.place())) {
                    current = next;
                    return;
                }

                if (replaceAnchorOnEarlyStop) finish(anchor, def.place());
                else if (anchor.getType() == anchorBlock) anchor.setType(Material.AIR, false);
                debug(player, "Stopped early at " + next.getX() + "," + next.getY() + "," + next.getZ() + " because of " + nextType);
                cancel();
            }
        }.runTaskTimer(this, delayTicks, delayTicks);
    }

    private void finish(Block anchor, Material place) {
        if (anchor.getType() == anchorBlock) {
            anchor.setType(place, false);
        }
    }

    private String getBucketId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(bucketKey, PersistentDataType.STRING);
    }

    private void debug(Player player, String message) {
        if (debugPlayers.contains(player.getUniqueId())) {
            player.sendMessage(color("&8[&dGenDebug&8] &7" + message));
        }
    }

    private Material match(String value, Material fallback) {
        Material material = Material.matchMaterial(value);
        return material == null ? fallback : material;
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help", "reload", "give", "list", "info", "debug").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return buckets.keySet().stream().filter(s -> s.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return buckets.keySet().stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return Collections.emptyList();
    }
}
