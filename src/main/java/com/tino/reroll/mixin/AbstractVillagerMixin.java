package com.tino.reroll.mixin;

import com.tino.reroll.util.TradeInjection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin extends AgeableMob {
    @Unique
    private int reroll$lastOffersSize;

    protected AbstractVillagerMixin(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addOffersFromTradeSet", at = @At("HEAD"))
    private void reroll$captureBeforeTrades(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> resourceKey, CallbackInfo ci) {
        this.reroll$lastOffersSize = offers.size();
    }

    @Inject(method = "addOffersFromTradeSet", at = @At("TAIL"))
    private void reroll$injectLibrarianTrades(ServerLevel level, MerchantOffers offers, ResourceKey<TradeSet> resourceKey, CallbackInfo ci) {
        // Only inject if Trade Rebalance is enabled
        if (!level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
            return;
        }

        // Check if this is a Librarian
        if (!((Object)this instanceof Villager villager)) {
            return;
        }

        // Determine level from the resourceKey
        int tradeLevel = 0;
        if (resourceKey.equals(TradeSets.LIBRARIAN_LEVEL_1)) tradeLevel = 1;
        else if (resourceKey.equals(TradeSets.LIBRARIAN_LEVEL_2)) tradeLevel = 2;
        else if (resourceKey.equals(TradeSets.LIBRARIAN_LEVEL_3)) tradeLevel = 3;
        else if (resourceKey.equals(TradeSets.LIBRARIAN_LEVEL_4)) tradeLevel = 4;
        else if (resourceKey.equals(TradeSets.LIBRARIAN_LEVEL_5)) tradeLevel = 5;

        if (tradeLevel > 0) {
            // Inject our randomized pools/modifications
            TradeInjection.injectTrades(level, villager.getVillagerData().type().unwrapKey().orElse(null), tradeLevel, offers, this.reroll$lastOffersSize);
        }
    }
}
