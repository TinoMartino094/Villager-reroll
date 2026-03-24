package com.tino.reroll.event;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Map;

public class ItemTooltipHandler implements ItemTooltipCallback {
    @Override
    public void getTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        if (stack.is(Items.HEAVY_CORE)) {
            net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && !customData.isEmpty()) {
                net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                net.minecraft.nbt.CompoundTag infusions = tag.getCompoundOrEmpty("villager_reroll:infusions");
                
                for (String key : infusions.keySet()) {
                    int level = infusions.getIntOr(key, 0);
                    net.minecraft.resources.Identifier resId = net.minecraft.resources.Identifier.tryParse(key);
                    if (resId != null) {
                        context.registries().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                            .flatMap(reg -> reg.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, resId)))
                            .ifPresent(h -> {
                                lines.add(Enchantment.getFullname(h, level));
                            });
                    }
                }
            }
        }
    }
}
