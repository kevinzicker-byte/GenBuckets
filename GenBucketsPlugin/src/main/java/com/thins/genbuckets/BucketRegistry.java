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

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        Material baseMaterial = Material.LAVA_BUCKET;
        if (itemsSection != null) {
            ConfigurationSection baseSection = itemsSection.getConfigurationSection("base");
            if (baseSection != null) {
                String baseMatName = baseSection.getString("material", "LAVA_BUCKET");
                Material parsed = Material.matchMaterial(baseMatName);
                if (parsed != null) {
                    baseMaterial = parsed;
                }
            }
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("buckets");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) {
                continue;
            }

            boolean enabled = s.getBoolean("enabled", true);

            String placeName = s.getString("material", "COBBLESTONE");
            Material placeMaterial = Material.matchMaterial(placeName);
            if (placeMaterial == null) {
                placeMaterial = Material.COBBLESTONE;
            }

            String modeStr = s.getString("mode", "HORIZONTAL");
            GenMode mode;
            try {
                mode = GenMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                mode = GenMode.HORIZONTAL;
            }

            int slot = s.getInt("slot", 0);
            int maxLen = s.getInt("max_length", 0);
            String name = s.getString("name", "&fGen Bucket");
            List<String> lore = s.getStringList("lore");

            GenBucketDefinition def = new GenBucketDefinition(
                    id,
                    enabled,
                    baseMaterial,
                    placeMaterial,
                    mode,
                    slot,
                    maxLen,
                    name,
                    lore
            );

            buckets.put(id.toLowerCase(Locale.ROOT), def);
        }

        plugin.getLogger().info("Loaded " + buckets.size() + " gen buckets.");
    }

    public Collection<GenBucketDefinition> all() {
        return buckets.values();
    }

    public Optional<GenBucketDefinition> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(buckets.get(id.toLowerCase(Locale.ROOT)));
    }

    public ItemStack createItem(GenBucketDefinition def, int amount) {
        int finalAmount = Math.max(1, amount);
        ItemStack item = new ItemStack(def.baseMaterial(), finalAmount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

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

        String heldName = meta.getDisplayName();

        for (GenBucketDefinition def : buckets.values()) {
            ItemStack test = createItem(def, 1);
            if (!test.hasItemMeta()) {
                continue;
            }

            ItemMeta testMeta = test.getItemMeta();
            if (testMeta != null && testMeta.hasDisplayName() && heldName.equals(testMeta.getDisplayName())) {
                return Optional.of(def);
            }
        }

        return Optional.empty();
    }

    public Optional<GenBucketDefinition> fromItem(ItemStack item) {
        return match(item);
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
