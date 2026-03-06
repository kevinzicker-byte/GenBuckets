package com.thins.genbuckets;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class GenManager {
    private final GenBucketsPlugin plugin;

    public GenManager(GenBucketsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startGen(Player player, Block clickedBlock, BlockFace clickedFace, GenBucketDefinition def) {
        if (clickedBlock == null || clickedFace == null || def == null) {
            return false;
        }

        Block start = findGenStart(clickedBlock, clickedFace, def);
        if (start == null) {
            return false;
        }

        // Only block if the actual start spot is already an active anchor.
        // This allows placing next to active gens.
        if (start.getType() == plugin.getSettings().anchorBlock()) {
            return false;
        }

        start.setType(plugin.getSettings().anchorBlock(), false);

        switch (def.mode()) {
            case HORIZONTAL -> runHorizontal(start, clickedFace, def);
            case VERTICAL_UP -> runVertical(start, BlockFace.UP, def);
            case VERTICAL_DOWN -> runVertical(start, BlockFace.DOWN, def);
        }

        if (plugin.isDebug()) {
            plugin.getLogger().info("Started gen " + def.id() + " at " + start.getLocation());
        }

        return true;
    }

    private Block findGenStart(Block clickedBlock, BlockFace clickedFace, GenBucketDefinition def) {
        Material finalMaterial = def.placeMaterial();
        Material anchor = plugin.getSettings().anchorBlock();

        Block current;

        // Horizontal starts in the clicked face direction.
        // Vertical up/down starts one block above/below if clicked on top/bottom,
        // otherwise defaults to above/below the clicked block.
        if (def.mode() == GenMode.HORIZONTAL) {
            current = clickedBlock.getRelative(clickedFace);
        } else if (def.mode() == GenMode.VERTICAL_UP) {
            current = (clickedFace == BlockFace.UP) ? clickedBlock.getRelative(BlockFace.UP) : clickedBlock.getRelative(BlockFace.UP);
        } else {
            current = (clickedFace == BlockFace.DOWN) ? clickedBlock.getRelative(BlockFace.DOWN) : clickedBlock.getRelative(BlockFace.DOWN);
        }

        // Chain-gen support:
        // skip through anchors and already-generated blocks in the exact path.
        for (int i = 0; i < 512; i++) {
            Material type = current.getType();

            if (isReplaceableStart(type)) {
                return current;
            }

            if (type == anchor || type == finalMaterial) {
                current = advance(current, clickedFace, def.mode());
                continue;
            }

            // Sand is real sand only now, so no sandstone handling needed.
            return null;
        }

        return null;
    }

    private Block advance(Block current, Block clickedFace, GenMode mode) {
        return switch (mode) {
            case HORIZONTAL -> current.getRelative(clickedFace);
            case VERTICAL_UP -> current.getRelative(BlockFace.UP);
            case VERTICAL_DOWN -> current.getRelative(BlockFace.DOWN);
        };
    }

    private boolean isReplaceableStart(Material type) {
        return type.isAir()
                || type == Material.CAVE_AIR
                || type == Material.VOID_AIR
                || type == Material.WATER
                || type == Material.LAVA;
    }

    private boolean canContinueInto(Block block, Material finalMaterial) {
        Material type = block.getType();
        Material anchor = plugin.getSettings().anchorBlock();

        return type.isAir()
                || type == Material.CAVE_AIR
                || type == Material.VOID_AIR
                || type == Material.WATER
                || type == Material.LAVA
                || type == anchor
                || type == finalMaterial;
    }

    private void runHorizontal(Block anchor, BlockFace direction, GenBucketDefinition def) {
        final Material place = def.placeMaterial();
        final int max = def.maxLength() > 0 ? def.maxLength() : plugin.getSettings().horizontalLength();
        final long delay = plugin.getSettings().delayTicks();

        new BukkitRunnable() {
            private Block current = anchor;
            private int placed = 0;

            @Override
            public void run() {
                if (placed >= max) {
                    finishAnchor(anchor, place);
                    cancel();
                    return;
                }

                Block next = current.getRelative(direction);

                if (!canContinueInto(next, place)) {
                    finishAnchor(anchor, place);
                    cancel();
                    return;
                }

                next.setType(place, false);
                current = next;
                placed++;
            }
        }.runTaskTimer(plugin, delay, delay);
    }

    private void runVertical(Block anchor, BlockFace direction, GenBucketDefinition def) {
        final Material place = def.placeMaterial();
        final int minY = plugin.getSettings().minY();
        final int maxY = plugin.getSettings().maxY();
        final long delay = plugin.getSettings().delayTicks();

        new BukkitRunnable() {
            private Block current = anchor;

            @Override
            public void run() {
                Block next = current.getRelative(direction);
                int y = next.getY();

                if (y < minY || y > maxY) {
                    finishAnchor(anchor, place);
                    cancel();
                    return;
                }

                if (!canContinueInto(next, place)) {
                    finishAnchor(anchor, place);
                    cancel();
                    return;
                }

                next.setType(place, false);
                current = next;
            }
        }.runTaskTimer(plugin, delay, delay);
    }

    private void finishAnchor(Block anchor, Material finalMaterial) {
        Material anchorType = plugin.getSettings().anchorBlock();

        if (anchor.getType() == anchorType) {
            anchor.setType(finalMaterial, false);
        }
    }
}
