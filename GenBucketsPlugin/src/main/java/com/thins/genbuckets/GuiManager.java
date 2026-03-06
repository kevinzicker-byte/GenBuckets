package com.thins.genbuckets;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class GuiManager {
    private final GenBucketsPlugin plugin;

    public GuiManager(GenBucketsPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory createMainMenu() {
        String title = BucketRegistry.color(plugin.getConfig().getString("gui.title", "&d&lGen Buckets"));
        int size = plugin.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection filler = plugin.getConfig().getConfigurationSection("gui.filler");
        if (filler != null && filler.getBoolean("enabled", true)) {
            Material fillerMat = Material.matchMaterial(filler.getString("material", "BLACK_STAINED_GLASS_PANE"));
            if (fillerMat != null) {
                ItemStack item = new ItemStack(fillerMat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(BucketRegistry.color(filler.getString("name", " ")));
                    List<String> lore = new ArrayList<>();
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                for (int i = 0; i < size; i++) inv.setItem(i, item);
            }
        }

        for (GenBucketDefinition def : plugin.getBucketRegistry().all()) {
            if (!def.enabled()) continue;
            if (def.slot() >= 0 && def.slot() < size) {
                inv.setItem(def.slot(), plugin.getBucketRegistry().createItem(def, 1));
            }
        }
        return inv;
    }
}
