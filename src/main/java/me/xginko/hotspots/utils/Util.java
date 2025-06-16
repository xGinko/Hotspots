package me.xginko.hotspots.utils;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Random;

public final class Util {

    public static final Random RANDOM = new Random();

    public static <T> T getRandomElement(List<T> list) {
        return list.size() == 1 ? list.get(0) : list.get(RANDOM.nextInt(list.size()));
    }

    public static @NotNull Color colorFromHexString(String hexString) throws NumberFormatException {
        return Color.fromRGB(Integer.parseInt(hexString.replace("#", ""), 16));
    }

    public static @NotNull String formatDuration(@NotNull Duration duration) {
        int minutes = duration.toMinutesPart();
        int hours = duration.toHoursPart();
        long days = duration.toDaysPart();

        if (days > 0) {
            return String.format("%02dd %02dh %02dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%02dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, duration.toSecondsPart());
        } else {
            return String.format("%02ds", duration.toSecondsPart());
        }
    }

    public static @NotNull List<Component> getStartupLogo() {
        return ImmutableList.of(
                Component.text("╭──────────────╮ ", AdventureUtil.ALACITY_MAGENTA),
                Component.text("│   pdPPYYba,  │   _  _       _                   _       ", AdventureUtil.ALACITY_MAGENTA),
                Component.text("│         `Y8  │  | || | ___ | |_  ___ _ __  ___ | |_  ___", AdventureUtil.ALACITY_MAGENTA),
                Component.text("│  ,adPPPPP88  │  | __ |/ _ \\|  _|(_-<| '_ \\/ _ \\|  _|(_-<", AdventureUtil.ALACITY_MAGENTA),
                Component.text("│  88,    ,88  │  |_||_|\\___/ \\__|/__/| .__/\\___/ \\__|/__/", AdventureUtil.ALACITY_MAGENTA),
                Component.text("│  `\"8bbdP\"Y8  │                      |_|       ", AdventureUtil.ALACITY_MAGENTA),
                Component.text("╰──────────────╯ ", AdventureUtil.ALACITY_MAGENTA)
        );
    }

    public static Component startupResultLog(String string, boolean success, int totalLength) {
        return Component.text(" ".repeat(((totalLength - string.length()) / 2) + 1) + string + " ", AdventureUtil.WHITE)
                .append(success ? Component.text("\uD83D\uDDF8", AdventureUtil.ALACITY_MAGENTA)
                                : Component.text("\uD800\uDD02", AdventureUtil.RED));
    }

    public static Component centerWithSpaces(Component component, int logoWidth) {
        int spaces_per_side = (logoWidth - PlainTextComponentSerializer.plainText().serialize(component).length()) / 2;
        return Component.text(" ".repeat(spaces_per_side + 1)).append(component);
    }
}
