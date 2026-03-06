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
        if (player == null || clickedBlock == null || clickedFace == null || def == null) {
            return false;
        }

        BlockFace direction = getDirection(player, clickedFace, def.mode());
        Block start = findGenStart(clickedBlock, direction, def);

        if (start == null) {
            return false;
        }

        if (start.getType() == anchorBlock()) {
            return false;
        }

        // stop vertical-up from starting above max_y
        if (def.mode() == GenMode.VERTICAL_UP && start.getY() > maxY()) {
            return false;
        }

        // stop vertical-down from starting below min_y
        if (def.mode() == GenMode.VERTICAL_DOWN && start.getY() < minY()) {
            return false;
        }

        start.setType(anchorBlock(), false);

        switch (def.mode()) {
            case HORIZONTAL -> runHorizontal(start, direction, def);
            case VERTICAL_UP -> runVertical(start, BlockFace.UP, def);
            case VERTICAL_DOWN -> runVertical(start, BlockFace.DOWN, def);
        }

        if (plugin.isDebug()) {
            plugin.getLogger().info("Started gen " + def.id() + " at " + start.getLocation());
        }

        return true;
    }

    private BlockFace getDirection(Player player, BlockFace clickedFace, GenMode mode) {
        if (mode != GenMode.HORIZONTAL) {
            return clickedFace;
        }

        // Always make horizontal gens go toward the player.
        // If the player clicked a side face, use that side face.
        if (clickedFace == BlockFace.NORTH
                || clickedFace == BlockFace.SOUTH
                || clickedFace == BlockFace.EAST
                || clickedFace == BlockFace.WEST) {
            return clickedFace;
        }

        // If they clicked top/bottom, derive horizontal direction from player position.
        double px = player.getLocation().getX();
        double pz = player.getLocation().getZ();
        double bx = player.getLocation().getBlockX() + 0.5;
        double bz = player.getLocation().getBlockZ() + 0.5;

        double dx = px - bx;
        double dz = pz - bz;

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            return dz >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
    }

    private Block findGenStart(Block clickedBlock, BlockFace direction, GenBucketDefinition def) {
        Material finalMaterial = def.placeMaterial();
        Material anchor = anchorBlock();

        Material clickedType = clickedBlock.getType();
        boolean clickedIsExistingLine = clickedType == anchor || clickedType == finalMaterial;

        Block current;

        if (def.mode() == GenMode.HORIZONTAL) {
            current = clickedBlock.getRelative(direction);
        } else {
            if (clickedIsExistingLine) {
                current = advance(clickedBlock, direction, def.mode());
            } else {
                current = clickedBlock.getRelative(direction);
            }
        }

        for (int i = 0; i < 512; i++) {
            Material type = current.getType();

            if (isReplaceableStart(type)) {
                return current;
            }

            if (type == anchor || type == finalMaterial) {
                current = advance(current, direction, def.mode());
                continue;
            }

            return null;
        }

        return null;
    }

    private Block advance(Block current, BlockFace direction, GenMode mode) {
        return switch (mode) {
            case HORIZONTAL -> current.getRelative(direction);
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
                if (anchor.getType() != anchorBlock()) {
                    cancel();
                    return;
                }

                Block next = current.getRelative(direction);
                int y = next.getY();

                // With max_y: 255, highest placed block is 255, so 256 stays open.
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
