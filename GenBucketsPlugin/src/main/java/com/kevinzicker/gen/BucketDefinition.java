package com.kevinzicker.gen;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class BucketDefinition {
    private final String id;
    private final boolean enabled;
    private final int slot;
    private final Material place;
    private final BucketMode mode;
    private final String name;
    private final List<String> lore;

    public BucketDefinition(String id, boolean enabled, int slot, Material place, BucketMode mode, String name, List<String> lore) {
        this.id = id;
        this.enabled = enabled;
        this.slot = slot;
        this.place = place;
        this.mode = mode;
        this.name = name;
        this.lore = lore;
    }

    public static BucketDefinition fromSection(String id, ConfigurationSection section) {
        boolean enabled = section.getBoolean("enabled", true);
        int slot = section.getInt("slot", 0);
        Material place = Material.matchMaterial(section.getString("place", "COBBLESTONE"));
        if (place == null) {
            place = Material.COBBLESTONE;
        }
        BucketMode mode;
        try {
            mode = BucketMode.valueOf(section.getString("mode", "VERTICAL_UP").toUpperCase());
        } catch (IllegalArgumentException ex) {
            mode = BucketMode.VERTICAL_UP;
        }
        String name = section.getString("name", "&fGen Bucket");
        List<String> lore = new ArrayList<>(section.getStringList("lore"));
        return new BucketDefinition(id, enabled, slot, place, mode, name, lore);
    }

    public String id() { return id; }
    public boolean enabled() { return enabled; }
    public int slot() { return slot; }
    public Material place() { return place; }
    public BucketMode mode() { return mode; }
    public String name() { return name; }
    public List<String> lore() { return lore; }
}
