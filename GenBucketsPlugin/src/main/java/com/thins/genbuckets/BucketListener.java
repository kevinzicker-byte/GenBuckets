package com.thins.genbuckets;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        BlockFace face = event.getBlockFace();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (clicked == null || face == null || item == null || item.getType() == Material.AIR) {
            return;
        }

        var defOpt = plugin.getBucketRegistry().match(item);
        if (defOpt.isEmpty()) {
            return;
        }

        GenBucketDefinition def = defOpt.get();

        event.setCancelled(true);

        boolean started = plugin.getGenManager().startGen(player, clicked, face, def);

        if (!started) {
            player.sendMessage(BucketRegistry.color("&cThat gen bucket can't start there."));
            return;
        }

        // Infinite use
        player.updateInventory();
    }
}
