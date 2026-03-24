package com.tino.reroll.mixin;

import com.tino.reroll.util.VillagerTradeContext;
import com.tino.reroll.duck.TradeLevelDuck;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin implements TradeLevelDuck {
    @Unique
    private int villager_reroll$tradeLevel;

    @Override
    public int villager_reroll$getTradeLevel() {
        return this.villager_reroll$tradeLevel;
    }

    @Override
    public void villager_reroll$setTradeLevel(int level) {
        this.villager_reroll$tradeLevel = level;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/item/trading/ItemCost;Ljava/util/Optional;Lnet/minecraft/world/item/ItemStack;IIZIIFI)V", at = @At("RETURN"))
    private void villager_reroll$init(ItemCost baseCostA, Optional<ItemCost> costB, ItemStack result, int uses, int maxUses, boolean rewardExp, int specialPriceDiff, int demand, float priceMultiplier, int xp, CallbackInfo ci) {
        Integer level = VillagerTradeContext.getCurrentLevel();
        if (level != null) {
            this.villager_reroll$tradeLevel = level;
        }
    }
}
