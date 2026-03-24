package com.tino.reroll.mixin;

import com.tino.reroll.util.VillagerTradeContext;
import com.tino.reroll.duck.TradeLevelDuck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.npc.villager.VillagerData;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
    @Shadow public abstract VillagerData getVillagerData();

    public VillagerMixin(net.minecraft.world.entity.EntityType<? extends AbstractVillager> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }


    @Inject(method = "updateTrades", at = @At("HEAD"))
    private void villager_reroll$onUpdateTrades(ServerLevel level, CallbackInfo ci) {
        VillagerTradeContext.setCurrentLevel(this.getVillagerData().level());
    }

    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void villager_reroll$afterUpdateTrades(ServerLevel level, CallbackInfo ci) {
        VillagerTradeContext.clear();
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void villager_reroll$interact(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Feature 1: Reroll trades with Configured Item
        if (stack.is(com.tino.reroll.config.ModConfig.INSTANCE.getRerollItem())) {
            if (!this.level().isClientSide()) {
                int currentLevel = this.getVillagerData().level();
                // Restriction: Don't work on Level 1 (Novice) villagers - breaking workstation is free
                if (currentLevel <= 1) {
                    this.makeSound(SoundEvents.VILLAGER_NO);
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                boolean removed = this.getOffers().removeIf(offer -> {
                    int level = ((TradeLevelDuck) offer).villager_reroll$getTradeLevel();
                    return level == currentLevel;
                });
                
                if (removed) {
                    // This will fill missing trades for the current level
                    this.updateTrades((ServerLevel) this.level());
                    
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    this.makeSound(SoundEvents.VILLAGER_YES);
                    
                    // Show happy particles
                    this.level().broadcastEntityEvent(this, (byte) 14);
                    
                    // Force refresh client-side trades
                    this.setTradingPlayer(null); 
                } else {
                    this.makeSound(SoundEvents.VILLAGER_NO);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }

        // Feature 3 Part B: Upgrade trades with Configured Item
        if (stack.is(com.tino.reroll.config.ModConfig.INSTANCE.getInscriptionItem())) {
            if (!this.level().isClientSide()) {
                net.minecraft.world.item.component.CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData == null || customData.isEmpty()) {
                    this.makeSound(SoundEvents.VILLAGER_NO);
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                net.minecraft.nbt.CompoundTag infusions = tag.getCompoundOrEmpty("villager_reroll:infusions");
                if (infusions.isEmpty()) {
                    this.makeSound(SoundEvents.VILLAGER_NO);
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    return;
                }

                boolean upgraded = false;
                MerchantOffers offers = this.getOffers();
                for (int i = 0; i < offers.size(); i++) {
                    MerchantOffer offer = offers.get(i);
                    ItemStack result = offer.getResult().copy();
                    ItemEnchantments tradeEnchants = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                    if (tradeEnchants.isEmpty()) continue;

                    ItemEnchantments.Mutable newEnchants = new ItemEnchantments.Mutable(tradeEnchants);
                    boolean modified = false;
                    int levelIncrease = 0;

                    for (String key : infusions.keySet()) {
                        int coreLevel = infusions.getIntOr(key, 0);
                        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(key);
                        if (id == null) continue;

                        java.util.Optional<Holder.Reference<Enchantment>> enchantOpt = this.level().registryAccess()
                                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, id));

                        if (enchantOpt.isPresent()) {
                            Holder<Enchantment> enchantment = enchantOpt.get();
                            int tradeLevel = tradeEnchants.getLevel(enchantment);
                            // Only upgrade by +1 level per core, even if core has higher level
                            if (tradeLevel > 0 && coreLevel > tradeLevel) {
                                newEnchants.set(enchantment, tradeLevel + 1);
                                levelIncrease++;
                                modified = true;
                            }
                        }
                    }

                    if (modified) {
                        result.set(DataComponents.STORED_ENCHANTMENTS, newEnchants.toImmutable());
                        
                        // Increase price: +3 emeralds per level upgraded
                        ItemCost oldCost = offer.getItemCostA();
                        if (oldCost.item().value() == Items.EMERALD) {
                            int newCount = Math.min(64, oldCost.count() + (levelIncrease * 3));
                            ItemCost newCost = new ItemCost(oldCost.item(), newCount, oldCost.components());
                            
                            MerchantOffer newOffer = new MerchantOffer(
                                newCost,
                                offer.getItemCostB(),
                                result,
                                offer.getUses(),
                                offer.getMaxUses(),
                                offer.getXp(),
                                offer.getPriceMultiplier(),
                                offer.getDemand()
                            );
                            // Transfer metadata to ensure reroll works on upgraded trades
                            ((TradeLevelDuck) newOffer).villager_reroll$setTradeLevel(((TradeLevelDuck) offer).villager_reroll$getTradeLevel());
                            offers.set(i, newOffer);
                        } else {
                            // If not emeralds, just update the result stack in place
                            offer.getResult().set(DataComponents.STORED_ENCHANTMENTS, newEnchants.toImmutable());
                        }
                        upgraded = true;
                    }
                }

                if (upgraded) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    this.makeSound(SoundEvents.VILLAGER_YES);
                    
                    // Show happy particles
                    this.level().broadcastEntityEvent(this, (byte) 14);
                    
                    // Force refresh client-side trades without locking the villager
                    this.setTradingPlayer(null);
                } else {
                    this.makeSound(SoundEvents.VILLAGER_NO);
                }
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
