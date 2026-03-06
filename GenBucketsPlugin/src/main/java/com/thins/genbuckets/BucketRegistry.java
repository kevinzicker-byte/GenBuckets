package com.thins.genbuckets;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public final class BucketRegistry {
    private final GenBucketsPlugin plugin;
    private final Map<String, GenBucketDefinition> buckets = new LinkedHashMap<>();

    public BucketRegistry(GenBucketsPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        buckets.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("buckets");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection bucket = section.getConfigurationSection(id);
            if (bucket == null) continue;

            boolean enabled = bucket.getBoolean("enabled", true);
            Material itemMaterial = Material.matchMaterial(bucket.getString("material", "LAVA_BUCKET"));
            Material placeMaterial = Material.matchMaterial(bucket.getString("place_material", bucket.getString("material", "COBBLESTONE")));
            GenMode mode = GenMode.valueOf(bucket.getString("mode", "HORIZONTAL").toUpperCase(Locale.ROOT));
            int slot = bucket.getInt("slot", 10);
            int maxLength = bucket.getInt("max_length", plugin.getConfig().getInt("settings.horizontal_length", 16));
            String name = color(bucket.getString("name", id));
            List<String> lore = bucket.getStringList("lore").stream().map(BucketRegistry::color).collect(Collectors.toList());

            if (itemMaterial == null || placeMaterial == null) continue;
            buckets.put(id.toLowerCase(Locale.ROOT), new GenBucketDefinition(id, enabled, itemMaterial, placeMaterial, mode, slot, maxLength, name, lore));
        }
    }

    public Collection<GenBucketDefinition> all() {
        return buckets.values();
    }

    public Optional<GenBucketDefinition> get(String id) {
        return Optional.ofNullable(buckets.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<GenBucketDefinition> fromItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return Optional.empty();
        String display = meta.getDisplayName();
        return buckets.values().stream().filter(def -> def.name().equals(display)).findFirst();
    }

    public ItemStack createItem(GenBucketDefinition def, int amount) {
        ItemStack item = new ItemStack(def.itemMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(def.name());
            meta.setLore(def.lore());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
