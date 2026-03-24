package com.tino.reroll;

import com.tino.reroll.event.ItemTooltipHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerReroll implements ModInitializer {
	public static final String MOD_ID = "villager-reroll";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Villager Reroll initialized!");
		ItemTooltipCallback.EVENT.register(new ItemTooltipHandler());
	}
}