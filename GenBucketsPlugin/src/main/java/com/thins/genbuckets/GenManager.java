package com.thins.genbuckets;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class GenManager {
    private final GenBucketsPlugin plugin;
    private final int minY;
    private final int maxY;
    private final int horizontalLength;
    private final long delayTicks;
    private final Material anchorMaterial;

    public GenManager(GenBucketsPlugin plugin) {
        this.plugin = plugin;
        this.minY = plugin.getConfig().getInt("settings.min_y", 1);
        this.maxY = plugin.getConfig().getInt("settings.max_y", 255);
        this.horizontalLength = plugin.getConfig().getInt("settings.horizontal_length", 16);
        this.delayTicks = plugin.getConfig().getLong("settings.delay_ticks", 2L);
        Material material = Material.matchMaterial(plugin.getConfig().getString("settings.anchor_block", "WHITE_WOOL"));
        this.anchorMaterial = material == null ? Material.WHITE_WOOL : material;
    }

    public boolean startGen(Player player, Block clicked, BlockFace face, GenBucketDefinition def) {
        Block start = findGenStart(clicked, face, def);
        if (start == null) return false;
        if (hasNearbyAnchor(start)) return false;

        start.setType(anchorMaterial, false);

        switch (def.mode()) {
            case HORIZONTAL -> runHorizontal(start, face, def);
            case VERTICAL_UP -> runVertical(start, BlockFace.UP, def);
            case VERTICAL_DOWN -> runVertical(start, BlockFace.DOWN, def);
        }
        return true;
    }

    private Block findGenStart(Block clicked, BlockFace face, GenBucketDefinition def) {
        Block current = switch (def.mode()) {
            case HORIZONTAL -> clicked.getRelative(face);
            case VERTICAL_UP, VERTICAL_DOWN -> (face == BlockFace.UP || face == BlockFace.DOWN) ? clicked.getRelative(face) : clicked.getRelative(BlockFace.UP);
        };

        for (int i = 0; i < 64; i++) {
            Material type = current.getType();
            if (type.isAir()) return current;
            if (type == anchorMaterial || type == def.placeMaterial()) {
                current = advance(current, face, def.mode());
                continue;
            }
            return null;
        }
        return null;
    }

    private Block advance(Block current, BlockFace face, GenMode mode) {
        return switch (mode) {
            case HORIZONTAL -> current.getRelative(face);
            case VERTICAL_UP -> current.getRelative(BlockFace.UP);
            case VERTICAL_DOWN -> current.getRelative(BlockFace.DOWN);
        };
    }

    private boolean hasNearbyAnchor(Block block) {
        if (block.getType() == anchorMaterial) return true;
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (block.getRelative(face).getType() == anchorMaterial) return true;
        }
        return false;
    }

    private void runHorizontal(Block anchor, BlockFace direction, GenBucketDefinition def) {
        final int limit = def.maxLength() > 0 ? def.maxLength() : horizontalLength;
        new BukkitRunnable() {
            int placed = 0;
            Block current = anchor;
            @Override
            public void run() {
                if (placed >= limit) {
                    finishAnchor(anchor, def);
                    cancel();
                    return;
                }
                Block next = current.getRelative(direction);
                if (!next.getType().isAir()) {
                    finishAnchor(anchor, def);
                    cancel();
                    return;
                }
                next.setType(def.placeMaterial(), false);
                current = next;
                placed++;
            }
        }.runTaskTimer(plugin, delayTicks, delayTicks);
    }

    private void runVertical(Block anchor, BlockFace direction, GenBucketDefinition def) {
        new BukkitRunnable() {
            Block current = anchor;
            @Override
            public void run() {
                Block next = current.getRelative(direction);
                if (next.getY() < minY || next.getY() > maxY || !next.getType().isAir()) {
                    finishAnchor(anchor, def);
                    cancel();
                    return;
                }
                next.setType(def.placeMaterial(), false);
                current = next;
            }
        }.runTaskTimer(plugin, delayTicks, delayTicks);
    }

    private void finishAnchor(Block anchor, GenBucketDefinition def) {
        if (anchor.getType() == anchorMaterial) {
            anchor.setType(def.placeMaterial(), false);
        }
    }
}
