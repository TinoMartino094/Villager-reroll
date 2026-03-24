package com.tino.reroll.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tino.reroll.VillagerReroll;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("villager-reroll.json").toFile();

    public String rerollItemId = "minecraft:recovery_compass";
    public String inscriptionItemId = "minecraft:heavy_core";

    public static ModConfig INSTANCE = new ModConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                VillagerReroll.LOGGER.error("Failed to load config!", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            VillagerReroll.LOGGER.error("Failed to save config!", e);
        }
    }

    public Item getRerollItem() {
        return BuiltInRegistries.ITEM.get(Identifier.tryParse(rerollItemId))
                .map(net.minecraft.core.Holder::value)
                .orElse(net.minecraft.world.item.Items.RECOVERY_COMPASS);
    }

    public Item getInscriptionItem() {
        return BuiltInRegistries.ITEM.get(Identifier.tryParse(inscriptionItemId))
                .map(net.minecraft.core.Holder::value)
                .orElse(net.minecraft.world.item.Items.HEAVY_CORE);
    }
}
