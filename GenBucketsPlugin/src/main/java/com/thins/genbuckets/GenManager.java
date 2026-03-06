package com.thins.genbuckets;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
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

        // Optional filler
        Material fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        String fillerName = " ";
        ConfigurationSection fillerSection = plugin.getConfig().getConfigurationSection("gui.filler");
        if (fillerSection != null) {
            Material parsed = Material.matchMaterial(fillerSection.getString("material", "GRAY_STAINED_GLASS_PANE"));
            if (parsed != null) fillerMat = parsed;
            fillerName = BucketRegistry.color(fillerSection.getString("name", " "));
        }

        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(fillerName);
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        plugin.getBucketRegistry().all().stream()
                .filter(GenBucketDefinition::enabled)
                .sorted(Comparator.comparingInt(GenBucketDefinition::slot))
                .forEach(def -> {
                    ItemStack guiItem = createGuiDisplayItem(def);
                    int slot = def.slot();

                    if (slot >= 0 && slot < size) {
                        inv.setItem(slot, guiItem);
                    }
                });

        return inv;
    }

    private ItemStack createGuiDisplayItem(GenBucketDefinition def) {
        Material displayMat = def.placeMaterial();
        if (displayMat == null || displayMat == Material.AIR) {
            displayMat = Material.STONE;
        }

        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(BucketRegistry.color(def.name()));

        List<String> lore = new ArrayList<>();
        for (String line : def.lore()) {
            lore.add(BucketRegistry.color(line));
        }
        lore.add("");
        lore.add(BucketRegistry.color("&8Click to receive this gen bucket"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
}
