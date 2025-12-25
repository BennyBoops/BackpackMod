package net.bennyboops.backpackmod;

import net.minecraft.util.StringIdentifiable;

public enum BackpackColor implements StringIdentifiable {
    BROWN("brown", 0x8B4513),
    BLACK("black", 0x1D1D21),
    RED("red", 0xB02E26),
    GREEN("green", 0x5E7C16),
    BLUE("blue", 0x3C44AA),
    PURPLE("purple", 0x8932B8),
    CYAN("cyan", 0x169C9C),
    LIGHT_GRAY("light_gray", 0x9D9D97),
    GRAY("gray", 0x474F52),
    PINK("pink", 0xF38BAA),
    LIME("lime", 0x80C71F),
    YELLOW("yellow", 0xFED83D),
    LIGHT_BLUE("light_blue", 0x3AB3DA),
    MAGENTA("magenta", 0xC74EBD),
    ORANGE("orange", 0xF9801D),
    WHITE("white", 0xF9FFFE);

    private final String name;
    private final int color;

    BackpackColor(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String asString() {
        return name;
    }

    public static BackpackColor byName(String name) {
        for (BackpackColor color : values()) {
            if (color.name.equals(name)) {
                return color;
            }
        }
        return BROWN; // Default
    }
}