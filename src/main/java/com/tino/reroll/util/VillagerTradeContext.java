package com.tino.reroll.util;

public class VillagerTradeContext {
    private static final ThreadLocal<Integer> CURRENT_LEVEL = new ThreadLocal<>();

    public static void setCurrentLevel(Integer level) {
        CURRENT_LEVEL.set(level);
    }

    public static Integer getCurrentLevel() {
        return CURRENT_LEVEL.get();
    }

    public static void clear() {
        CURRENT_LEVEL.remove();
    }
}
