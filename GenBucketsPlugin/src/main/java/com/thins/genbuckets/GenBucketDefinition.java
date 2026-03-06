package com.thins.genbuckets;

import org.bukkit.Material;

import java.util.List;

public record GenBucketDefinition(
        String id,
        boolean enabled,
        Material itemMaterial,
        Material placeMaterial,
        GenMode mode,
        int slot,
        int maxLength,
        String name,
        List<String> lore
) {
}
