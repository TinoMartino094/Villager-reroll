package com.tino.reroll.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.tags.EnchantmentTags;
import java.util.Optional;

@Mixin(VillagerTrade.class)
public abstract class VillagerTradeMixin {
    @Shadow @Final private Optional<HolderSet<Enchantment>> doubleTradePriceEnchantments;

    @Inject(
        method = "getOffer", 
        at = @At("RETURN"), 
        cancellable = true
    )
    private void villager_reroll$nerfBookFinal(LootContext lootContext, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        if (offer != null && offer.getResult().is(Items.ENCHANTED_BOOK)) {
            ItemStack result = offer.getResult();
            
            // 1. Force Level 1 for all enchantments on books
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(enchantments);
                for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                    builder.set(enchantment, 1);
                }
                result.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
            }

            // 2. Determine price: Treasure/Curses = 10-38, Standard = 5-19
            int randomVal = lootContext.getRandom().nextInt(15) + 5;
            boolean isTreasure = enchantments != null && enchantments.keySet().stream().anyMatch(e -> e.is(EnchantmentTags.TREASURE) || e.is(EnchantmentTags.CURSE));
            
            int finalEmeraldCount = isTreasure ? randomVal * 2 : randomVal;
            
            // 3. Create a clean emerald cost
            net.minecraft.world.item.trading.ItemCost newEmeraldCost = new net.minecraft.world.item.trading.ItemCost(Items.EMERALD, finalEmeraldCount);
            
            // 4. Return new offer with forced price and level 1 book. We ignore vanilla specialized price doubling here 
            // because we are setting the FINAL base cost ourselves.
            cir.setReturnValue(new MerchantOffer(
                newEmeraldCost,
                offer.getItemCostB(), // Keep the book input
                result,
                offer.getUses(),
                offer.getMaxUses(),
                offer.getXp(),
                offer.getPriceMultiplier()
            ));
        }
    }
}
