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

        if (start.getType() == anchorBlock()) {
            return false;
        }

        start.setType(anchorBlock(), false);

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
        Material anchor = anchorBlock();

        // Always start from the block adjacent to the clicked face
        Block current = clickedBlock.getRelative(clickedFace);

        for (int i = 0; i < 512; i++) {
            Material type = current.getType();

            if (isReplaceableStart(type)) {
                return current;
            }

            // Chain gens: clicking old anchor or finished line continues outward
            if (type == anchor || type == finalMaterial) {
                current = advance(current, clickedFace, def.mode());
                continue;
            }

            return null;
        }

        return null;
    }

    private Block advance(Block current, BlockFace clickedFace, GenMode mode) {
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
        Material anchor = anchorBlock();

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
        final int max = def.maxLength() > 0 ? def.maxLength() : horizontalLength();
        final long delay = delayTicks();

        new BukkitRunnable() {
            private Block current = anchor;
            private int placed = 0;

            @Override
            public void run() {
                // Stop instantly if anchor was broken
                if (anchor.getType() != anchorBlock()) {
                    cancel();
                    return;
                }

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
        final int minY = minY();
        final int maxY = maxY();
        final long delay = delayTicks();

        new BukkitRunnable() {
            private Block current = anchor;

            @Override
            public void run() {
                // Stop instantly if anchor was broken
                if (anchor.getType() != anchorBlock()) {
                    cancel();
                    return;
                }

                Block next = current.getRelative(direction);
                int y = next.getY();

                // With max_y: 255, allow placing at 255 and stop before 256
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
        if (anchor.getType() == anchorBlock()) {
            anchor.setType(finalMaterial, false);
        }
    }

    private Material anchorBlock() {
        String raw = plugin.getConfig().getString("settings.anchor_block", "WHITE_WOOL");
        Material mat = Material.matchMaterial(raw);
        return mat != null ? mat : Material.WHITE_WOOL;
    }

    private int horizontalLength() {
        return plugin.getConfig().getInt("settings.horizontal_length", 16);
    }

    private int minY() {
        return plugin.getConfig().getInt("settings.min_y", 1);
    }

    private int maxY() {
        return plugin.getConfig().getInt("settings.max_y", 255);
    }

    private long delayTicks() {
        return plugin.getConfig().getLong("settings.delay_ticks", 2L);
    }
}
