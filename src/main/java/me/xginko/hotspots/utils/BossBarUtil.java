package me.xginko.hotspots.utils;

import me.xginko.hotspots.Hotspots;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class BossBarUtil {

    public static @NotNull String toJSONString(BossBar boss_bar) {
        JSONObject json = new JSONObject();
        json.put("name", MiniMessage.miniMessage().serialize(boss_bar.name()));
        json.put("color", boss_bar.color().name());
        return json.toJSONString();
    }

    public static @NotNull BossBar fromJSONString(String json_string) throws ParseException, NullPointerException {
        JSONObject json = (JSONObject) new JSONParser().parse(json_string);
        Component name = MiniMessage.miniMessage().deserialize((String) json.get("name"));
        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf((String) json.get("color"));
        } catch (IllegalArgumentException e) {
            color = getRandomBossBarColor();
        }
        return BossBar.bossBar(
                name,
                Hotspots.config().bossbar_reverse_progress ? BossBar.MAX_PROGRESS : BossBar.MIN_PROGRESS, // Will update when ticked
                color,
                Hotspots.config().bossbar_overlay, // Allows applying overlay settings on reload
                Hotspots.config().bossbar_flags // Allows applying flag settings on reload
        );
    }

    public static @NotNull BossBar.Color getRandomBossBarColor() {
        return Util.getRandomElement(Hotspots.config().bossbar_colors);
    }

    public static @NotNull BossBar getBossBar(Component player_name) {
        return getBossBar(player_name, getRandomBossBarColor());
    }

    public static @NotNull BossBar getBossBar(Component player_name, BossBar.Color color) {
        return BossBar.bossBar(
                Hotspots.config().bossbar_title
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%player%").replacement(player_name).build()),
                Hotspots.config().bossbar_reverse_progress ? BossBar.MAX_PROGRESS : BossBar.MIN_PROGRESS,
                color,
                Hotspots.config().bossbar_overlay,
                Hotspots.config().bossbar_flags
        );
    }

    public static @NotNull BossBar getBossBar(String boss_bar_name, BossBar.Color color) {
        return BossBar.bossBar(
                AdventureUtil.MINIMESSAGE_PLAYERINPUT_SAFE.deserialize(AdventureUtil.replaceAmpersand(boss_bar_name)),
                Hotspots.config().bossbar_reverse_progress ? BossBar.MAX_PROGRESS : BossBar.MIN_PROGRESS,
                color,
                Hotspots.config().bossbar_overlay,
                Hotspots.config().bossbar_flags
        );
    }
}
