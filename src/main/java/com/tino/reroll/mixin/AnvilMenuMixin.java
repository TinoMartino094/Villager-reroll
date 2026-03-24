package com.tino.reroll.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow @Final private DataSlot cost;
    @Unique private boolean villager_reroll$shouldPreserveCore = false;
    @Unique private ItemStack villager_reroll$preservedStack = ItemStack.EMPTY;

    public AnvilMenuMixin(@Nullable MenuType<?> type, int containerId, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.inventory.ContainerLevelAccess access, net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition slotDefinition) {
        super(type, containerId, inventory, access, slotDefinition);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void villager_reroll$onTakeHead(net.minecraft.world.entity.player.Player player, ItemStack carried, CallbackInfo ci) {
        ItemStack input = this.inputSlots.getItem(0);
        ItemStack addition = this.inputSlots.getItem(1);
        // If it's our recipe and we have a stack, prepare to preserve the rest
        if (input.is(com.tino.reroll.config.ModConfig.INSTANCE.getInscriptionItem()) && addition.is(Items.ENCHANTED_BOOK) && input.getCount() > 1) {
            this.villager_reroll$shouldPreserveCore = true;
            this.villager_reroll$preservedStack = input.copy();
            this.villager_reroll$preservedStack.shrink(1);
        } else {
            this.villager_reroll$shouldPreserveCore = false;
        }
    }

    @Inject(method = "onTake", at = @At("RETURN"))
    private void villager_reroll$onTakeReturn(net.minecraft.world.entity.player.Player player, ItemStack carried, CallbackInfo ci) {
        if (this.villager_reroll$shouldPreserveCore) {
            this.inputSlots.setItem(0, this.villager_reroll$preservedStack);
            this.villager_reroll$shouldPreserveCore = false;
            this.villager_reroll$preservedStack = ItemStack.EMPTY;
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void villager_reroll$createResult(CallbackInfo ci) {
        ItemStack input = this.inputSlots.getItem(0);
        ItemStack addition = this.inputSlots.getItem(1);

        if (input.is(com.tino.reroll.config.ModConfig.INSTANCE.getInscriptionItem()) && addition.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments inputEnchants = input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            ItemEnchantments additionEnchants = addition.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

            if (additionEnchants.isEmpty()) return;

            ItemStack result = input.copy();
            result.setCount(1); // Only produce one inscribed core at a time
            result.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Inscribed Heavy Core")
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE).withStyle(s -> s.withItalic(false)));
            
            int[] totalCost = {0};
            java.util.List<net.minecraft.network.chat.Component> loreLines = new java.util.ArrayList<>();

            net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
                net.minecraft.nbt.CompoundTag infusions = tag.getCompoundOrEmpty("villager_reroll:infusions");
                
                for (java.util.Map.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>, Integer> entry : additionEnchants.entrySet()) {
                    net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment = entry.getKey();
                    String id = enchantment.getRegisteredName();
                    int levelToAdd = entry.getValue();
                    int currentLevel = infusions.getIntOr(id, 0);

                    int finalLevel;
                    if (currentLevel == levelToAdd) {
                        finalLevel = currentLevel + 1;
                    } else {
                        finalLevel = Math.max(currentLevel, levelToAdd);
                    }

                    int maxLevel = enchantment.value().getMaxLevel();
                    if (finalLevel > maxLevel) {
                        finalLevel = maxLevel;
                    }

                    infusions.putInt(id, finalLevel);
                    totalCost[0] += finalLevel * enchantment.value().getAnvilCost();
                }
                tag.put("villager_reroll:infusions", infusions);

                // Update Lore based on final infusions
                for (String key : infusions.keySet()) {
                    int level = infusions.getIntOr(key, 0);
                    net.minecraft.resources.Identifier resId = net.minecraft.resources.Identifier.tryParse(key);
                    if (resId != null) {
                        this.player.level().registryAccess().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .flatMap(reg -> reg.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, resId)))
                            .ifPresent(h -> {
                                loreLines.add(h.value().description().copy()
                                    .append(" " + net.minecraft.network.chat.Component.translatable("enchantment.level." + level).getString())
                                    .withStyle(net.minecraft.ChatFormatting.AQUA));
                            });
                    }
                }
            });

            if (!loreLines.isEmpty()) {
                result.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(loreLines));
            }

            this.resultSlots.setItem(0, result);
            this.cost.set(Math.max(1, totalCost[0]));
            this.broadcastChanges();
            ci.cancel();
        }
    }
}
