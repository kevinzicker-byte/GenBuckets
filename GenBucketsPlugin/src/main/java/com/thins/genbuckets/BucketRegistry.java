package com.thins.genbuckets;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class BucketRegistry {

    private final GenBucketsPlugin plugin;
    private final Map<String, GenBucketDefinition> buckets = new HashMap<>();

    public BucketRegistry(GenBucketsPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        buckets.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("buckets");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) continue;

            String matName = s.getString("material", "COBBLESTONE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.COBBLESTONE;

            String modeStr = s.getString("mode", "HORIZONTAL");
            GenMode mode = GenMode.valueOf(modeStr.toUpperCase());

            int slot = s.getInt("slot", 0);
            int maxLen = s.getInt("max_length", 0);

            String name = s.getString("name", "&fGen Bucket");

            List<String> lore = s.getStringList("lore");

            GenBucketDefinition def = new GenBucketDefinition(
                    id,
                    mat,
                    mode,
                    slot,
                    maxLen,
                    name,
                    lore
            );

            buckets.put(id.toLowerCase(), def);
        }

        plugin.getLogger().info("Loaded " + buckets.size() + " gen buckets.");
    }

    public Collection<GenBucketDefinition> all() {
        return buckets.values();
    }

    public Optional<GenBucketDefinition> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(buckets.get(id.toLowerCase()));
    }

    public ItemStack createItem(GenBucketDefinition def, int amount) {
        ItemStack item = new ItemStack(Material.LAVA_BUCKET, amount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color(def.name()));

        List<String> lore = new ArrayList<>();
        for (String line : def.lore()) {
            lore.add(color(line));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public Optional<GenBucketDefinition> match(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }

        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return Optional.empty();
        }

        String name = meta.getDisplayName();

        for (GenBucketDefinition def : buckets.values()) {
            ItemStack test = createItem(def, 1);
            if (!test.hasItemMeta()) continue;

            ItemMeta testMeta = test.getItemMeta();
            if (testMeta != null && name.equals(testMeta.getDisplayName())) {
                return Optional.of(def);
            }
        }

        return Optional.empty();
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
