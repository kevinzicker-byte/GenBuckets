package com.thins.genbuckets;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BucketListener implements Listener {
    private final GenBucketsPlugin plugin;

    public BucketListener(GenBucketsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        ItemStack item = event.getItem();
        plugin.getBucketRegistry().fromItem(item).ifPresent(def -> {
            event.setCancelled(true);
            Player player = event.getPlayer();
            BlockFace face = event.getBlockFace();
            boolean started = plugin.getGenManager().startGen(player, clicked, face, def);
            if (!started) {
                player.sendMessage(BucketRegistry.color("&cThat gen bucket can't start there."));
                return;
            }
            if (item != null) item.setAmount(item.getAmount() - 1);
        });
    }
}
