package com.tino.reroll.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;

import java.util.*;
import java.util.function.Supplier;

public class TradeInjection {
    private static final Map<ResourceKey<VillagerType>, BiomeTradeData> BIOME_DATA = new HashMap<>();

    static {
        // Final Biome Mapping Summary
        // Swamp: Depth Strider, Respiration, Vanishing, Density | Mending
        // Snow: Aqua Affinity, Looting, Frost Walker, Riptide | Silk Touch, Impaling
        // Jungle: Feather Falling, Proj Prot, Power, Multishot | Unbreaking
        // Taiga: Blast Prot, Fire Aspect, Flame, Loyalty | Fortune, Breach
        // Savanna: Knockback, Binding, Sweep Edge, Lunge | Sharpness, Piercing
        // Desert: Fire Prot, Thorns, Infinity, Lure | Efficiency, Luck of Sea
        // Plains: Punch, Smite, Bane, Quick Charge | Protection, Channeling
        
        BIOME_DATA.put(VillagerType.SWAMP, new BiomeTradeData(
            List.of(Enchantments.DEPTH_STRIDER, Enchantments.RESPIRATION, Enchantments.VANISHING_CURSE, Enchantments.DENSITY),
            List.of(Enchantments.MENDING)
        ));
        BIOME_DATA.put(VillagerType.SNOW, new BiomeTradeData(
            List.of(Enchantments.AQUA_AFFINITY, Enchantments.LOOTING, Enchantments.FROST_WALKER, Enchantments.RIPTIDE),
            List.of(Enchantments.SILK_TOUCH, Enchantments.IMPALING)
        ));
        BIOME_DATA.put(VillagerType.JUNGLE, new BiomeTradeData(
            List.of(Enchantments.FEATHER_FALLING, Enchantments.PROJECTILE_PROTECTION, Enchantments.POWER, Enchantments.MULTISHOT),
            List.of(Enchantments.UNBREAKING)
        ));
        BIOME_DATA.put(VillagerType.TAIGA, new BiomeTradeData(
            List.of(Enchantments.BLAST_PROTECTION, Enchantments.FIRE_ASPECT, Enchantments.FLAME, Enchantments.LOYALTY),
            List.of(Enchantments.FORTUNE, Enchantments.BREACH)
        ));
        BIOME_DATA.put(VillagerType.SAVANNA, new BiomeTradeData(
            List.of(Enchantments.KNOCKBACK, Enchantments.BINDING_CURSE, Enchantments.SWEEPING_EDGE, Enchantments.LUNGE),
            List.of(Enchantments.SHARPNESS, Enchantments.PIERCING)
        ));
        BIOME_DATA.put(VillagerType.DESERT, new BiomeTradeData(
            List.of(Enchantments.FIRE_PROTECTION, Enchantments.THORNS, Enchantments.INFINITY, Enchantments.LURE),
            List.of(Enchantments.EFFICIENCY, Enchantments.LUCK_OF_THE_SEA)
        ));
        BIOME_DATA.put(VillagerType.PLAINS, new BiomeTradeData(
            List.of(Enchantments.PUNCH, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS, Enchantments.QUICK_CHARGE),
            List.of(Enchantments.PROTECTION, Enchantments.CHANNELING)
        ));
    }

    public static void injectTrades(ServerLevel level, ResourceKey<VillagerType> type, int villagerLevel, MerchantOffers offers, int previousSize) {
        BiomeTradeData data = BIOME_DATA.get(type);
        if (data == null) return;

        if (villagerLevel >= 1 && villagerLevel <= 3) {
            for (int i = offers.size() - 1; i >= previousSize; i--) {
                if (offers.get(i).getResult().is(Items.ENCHANTED_BOOK)) {
                    offers.set(i, createRandomEnchantedBookTrade(level, data.commonBooks, villagerLevel));
                }
            }
        } else if (villagerLevel == 4) {
            while (offers.size() > previousSize) offers.remove(offers.size() - 1);

            List<Supplier<MerchantOffer>> pool = new ArrayList<>();
            pool.add(() -> createSimpleTrade(new ItemCost(Items.WRITABLE_BOOK, 2), new ItemStack(Items.EMERALD), 12, 30, 4));
            pool.add(() -> createSimpleTrade(new ItemCost(Items.EMERALD, 5), new ItemStack(Items.CLOCK), 12, 15, 4));
            pool.add(() -> createSimpleTrade(new ItemCost(Items.EMERALD, 4), new ItemStack(Items.COMPASS), 12, 15, 4));
            
            final List<ResourceKey<Enchantment>> commonPool = data.commonBooks;
            pool.add(() -> createRandomEnchantedBookTrade(level, commonPool, 4));

            net.minecraft.util.Util.shuffle(pool, level.getRandom());
            for (int i = 0; i < 2; i++) {
                MerchantOffer offer = pool.get(i).get();
                if (offer != null) offers.add(offer);
            }
        } else if (villagerLevel == 5) {
            while (offers.size() > previousSize) offers.remove(offers.size() - 1);

            List<Supplier<MerchantOffer>> pool = new ArrayList<>();
            pool.add(() -> createSimpleTrade(new ItemCost(Items.EMERALD, 3), new ItemStack(Items.RED_CANDLE), 12, 30, 5));
            pool.add(() -> createSimpleTrade(new ItemCost(Items.EMERALD, 3), new ItemStack(Items.YELLOW_CANDLE), 12, 30, 5));
            for (ResourceKey<Enchantment> ench : data.masterBooks) {
                pool.add(() -> createEnchantedBookTrade(level, ench, 1, 5));
            }

            net.minecraft.util.Util.shuffle(pool, level.getRandom());
            for (int i = 0; i < Math.min(3, pool.size()); i++) {
                MerchantOffer offer = pool.get(i).get();
                if (offer != null) offers.add(offer);
            }
        }
    }

    private static MerchantOffer createRandomEnchantedBookTrade(ServerLevel level, List<ResourceKey<Enchantment>> pool, int tradeLevel) {
        ResourceKey<Enchantment> enchKey = pool.get(level.getRandom().nextInt(pool.size()));
        // All trades set to Level 1 Enchantment (regardless of tier)
        return createEnchantedBookTrade(level, enchKey, 1, tradeLevel);
    }

    private static MerchantOffer createSimpleTrade(ItemCost cost, ItemStack result, int maxUses, int xp, int tradeLevel) {
        MerchantOffer offer = new MerchantOffer(cost, result, maxUses, xp, 0.05f);
        setTradeLevel(offer, tradeLevel);
        return offer;
    }

    private static MerchantOffer createEnchantedBookTrade(ServerLevel level, ResourceKey<Enchantment> enchKey, int enchLevel, int tradeLevel) {
        Optional<Holder.Reference<Enchantment>> ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchKey);
        if (ench.isEmpty()) return null;

        // Force Enchantment Level 1 for all books (Master and Common)
        int finalEnchLevel = 1;

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(ench.get(), finalEnchLevel);
        book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

        // Base price: 5-19
        int baseEmeralds = 5 + level.getRandom().nextInt(15);
        
        // Treasure/Curse doubling (10-38) and always even
        boolean isTreasure = ench.get().is(EnchantmentTags.TREASURE) || ench.get().is(EnchantmentTags.CURSE);
        int finalPrice = isTreasure ? baseEmeralds * 2 : baseEmeralds;

        int xpValue = (tradeLevel == 5) ? 30 : 15;

        MerchantOffer offer = new MerchantOffer(
            new ItemCost(Items.EMERALD, finalPrice),
            Optional.of(new ItemCost(Items.BOOK, 1)),
            book,
            12,
            xpValue,
            0.05f
        );
        setTradeLevel(offer, tradeLevel);
        return offer;
    }

    private static void setTradeLevel(MerchantOffer offer, int level) {
        if (offer instanceof com.tino.reroll.duck.TradeLevelDuck duck) {
            duck.villager_reroll$setTradeLevel(level);
        }
    }

    private record BiomeTradeData(List<ResourceKey<Enchantment>> commonBooks, List<ResourceKey<Enchantment>> masterBooks) {}
}
