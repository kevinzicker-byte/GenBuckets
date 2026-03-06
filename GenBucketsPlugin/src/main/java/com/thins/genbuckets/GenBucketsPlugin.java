package com.thins.genbuckets;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GenBucketsPlugin extends JavaPlugin {
    private BucketRegistry bucketRegistry;
    private GenManager genManager;
    private GuiManager guiManager;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalState();

        PluginCommand command = getCommand("gen");
        if (command != null) {
            GenCommand genCommand = new GenCommand(this);
            command.setExecutor(genCommand);
            command.setTabCompleter(genCommand);
        }

        Bukkit.getPluginManager().registerEvents(new BucketListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        getLogger().info("GenBuckets enabled.");
    }

    public void reloadLocalState() {
        reloadConfig();
        this.bucketRegistry = new BucketRegistry(this);
        this.genManager = new GenManager(this);
        this.guiManager = new GuiManager(this);
        this.debug = getConfig().getBoolean("settings.debug_default", false);
    }

    public BucketRegistry getBucketRegistry() {
        return bucketRegistry;
    }

    public GenManager getGenManager() {
        return genManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
