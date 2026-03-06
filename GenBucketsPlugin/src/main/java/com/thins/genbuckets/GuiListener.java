package com.thins.genbuckets;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class GuiListener implements Listener {
    private final GenBucketsPlugin plugin;

    public GuiListener(GenBucketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String expected = BucketRegistry.color(plugin.getConfig().getString("gui.title", "&d&lGen Buckets"));
        if (!event.getView().getTitle().equals(expected)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        plugin.getBucketRegistry().fromItem(clicked).ifPresent(def -> {
            player.getInventory().addItem(plugin.getBucketRegistry().createItem(def, 1));
            player.sendMessage(BucketRegistry.color("&aGiven yourself 1x &f" + def.id()));
        });
    }
}
