package me.xginko.hotspots.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.Title;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.LocaleUtil;
import me.xginko.hotspots.utils.MathHelper;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class HotspotsConfig {

    private final @NotNull ConfigFile configFile;

    public final @NotNull Locale default_locale;
    public final @NotNull List<String> bossbar_name_suggestions;
    public final @NotNull List<BossBar.Color> bossbar_colors;
    public final @NotNull Set<BossBar.Flag> bossbar_flags;
    public final @NotNull Component bossbar_title;
    public final @NotNull BossBar.Overlay bossbar_overlay;
    public final @NotNull net.kyori.adventure.title.Title.Times teleport_warmup_times;
    public final @NotNull Duration teleport_warmup_seconds;
    public final double teleport_radius_sqared;
    public final long hotspot_tick_period_millis, hotspot_life_time, disconnect_timeout_seconds, hotspot_max_store_time,
            commands_create_cooldown_millis, commands_join_cooldown_millis, commands_notifs_cooldown_millis,
            commands_confirm_cooldown_millis, commands_confirm_timeout_millis, commands_end_cooldown_millis;
    public final int max_active_hotspots, database_max_pool_size, teleport_radius, bossbar_title_maxlength;
    public final boolean auto_lang, bossbar_reverse_progress;

    public HotspotsConfig() throws Exception {
        // Load config.yml with ConfigMaster
        this.configFile = ConfigFile.loadConfig(new File(Hotspots.getInstance().getDataFolder(), "config.yml"));

        this.configFile.setTitle(new Title().withWidth(80)
                .addSolidLine()
                .addLine(" ", Title.Pos.CENTER)
                .addLine("ᴀʟᴀᴄɪᴛʏ ʜᴏᴛsᴘᴏᴛs", Title.Pos.CENTER)
                .addLine(" ", Title.Pos.CENTER)
                .addSolidLine());

        this.default_locale = LocaleUtil.localeForLanguageTag(getString("general.default-language", "en_us", """
                The default language that will be used if auto-language
                is false or no matching language file was found."""));
        this.auto_lang = getBoolean("general.auto-language", true, """
                If set to true, the plugin will send messages to players
                based on what locale their client is set to.
                This of course requires that there is a translation file
                available for that locale inside the plugins lang folder.""");

        this.database_max_pool_size = Math.max(1, getInt("database.max-pool-size", Bukkit.getMaxPlayers() * 2, """
                Basically this value will determine the maximum number of actual
                connections to the database backend.
                When the pool reaches this size, and no idle connections are available,
                calls to the database will block and time out
                Setting it to the double the amount of max players for the server is
                probably a good idea."""));
        this.hotspot_max_store_time = TimeUnit.MINUTES.toMillis(
                Math.max(1, getLong("database.max-hotspot-store-time-minutes", 60, """
                Time in minutes a hotspot will be kept in database after creation.""")));

        this.commands_create_cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, getLong("commands.create.cooldown-seconds", 1, """
                Time in seconds a player needs to wait before they can
                use the /hotspot create command again.""")));
        this.commands_join_cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, getLong("commands.join.cooldown-seconds", 1, """
                Time in seconds a player needs to wait before they can
                use the /hotspot join command again.""")));
        this.commands_notifs_cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, getLong("commands.notifs.cooldown-seconds", 1, """
                Time in seconds a player needs to wait before they can
                use the /hotspot notifs command again.""")));
        this.commands_confirm_cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, getLong("commands.confirm.cooldown-seconds", 1, """
                Time in seconds a player needs to wait before they can
                use the /hotspot confirm command again.""")));
        this.commands_confirm_timeout_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, getLong("commands.confirm.timeout-seconds", 16, """
                Time in millis /hotspot confirm can be used to confirm a previous
                /hotspot create command.""")));
        this.commands_end_cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, getLong("commands.end.cooldown-seconds", 3, """
                Time in seconds a player needs to wait before they can
                use the /hotspot end command again.""")));

        this.teleport_radius = Math.max(1, getInt("teleport.location.horizontal-radius", 8, """
                Radius in blocks in XZ-direction around the center location of the
                hotspot that will be used for random teleportation."""));
        this.teleport_radius_sqared = MathHelper.square(teleport_radius);
        this.teleport_warmup_seconds = Duration.ofSeconds(Math.max(1, getLong("teleport.warmup.seconds", 12, """
                Seconds a player will have to stand still and take no damage
                to teleport to a hotspot.""")));
        this.teleport_warmup_times = net.kyori.adventure.title.Title.Times.times(
                Duration.ofMillis(Math.max(0, getLong("teleport.warmup.title.fade-in-millis", 0,
                        "Title fade in time in milliseconds."))),
                Duration.ofMillis(Math.max(0, getLong("teleport.warmup.title.stay-millis", 1200,
                        "Title stay time in milliseconds."))),
                Duration.ofMillis(Math.max(0, getLong("teleport.warmup.title.fade-out-millis", 50,
                        "Title fade out time in milliseconds."))));
        
        this.hotspot_life_time = TimeUnit.SECONDS.toMillis(Math.max(1, getLong("hotspots.life-time-seconds", 900, """
                Duration in seconds a hotspot should last before expiring.""")));
        this.hotspot_tick_period_millis = Math.max(1, getLong("hotspots.tick-interval-millis", 1000, """
                The interval in milli seconds the hotspots should be ticked.
                A higher number could give better performance results during load
                but will make the logic more inaccurate the higher you set it."""));
        this.max_active_hotspots = Math.max(1, getInt("hotspots.max-active-hotspots", 3, """
                The amount of hotspots that can be active at the same time.
                If a player attempts to create a hotspot that would exceed
                this number, the action will simply be denied."""));
        this.disconnect_timeout_seconds = Math.max(1, getLong("hotspots.timeout-on-disconnect-in-seconds", 30, """
                How many seconds a hotspot will remain active after the creator
                left the server for whatever reason."""));

        this.bossbar_reverse_progress = getBoolean("hotspots.bossbar.reverse-progress", true, """
                If set to true, will go from (visually) full hp to no hp.""");
        this.bossbar_title = MiniMessage.miniMessage().deserialize(AdventureUtil.replaceAmpersand(getString(
                "hotspots.bossbar.title.default", "<#C000C0>%player%'s Hotspot", """
                        The title string that should be shown above the BossBar if.
                        the player didn't specify a name.
                        Format is using MiniMessage tags:
                        https://docs.advntr.dev/minimessage/format.html
                        The %player% placeholder will be replaced with the player's
                        display name.""")));
        this.bossbar_title_maxlength = Math.max(1, getInt("hotspots.bossbar.title.max-length", 64, """
                The max length of the name String passed to the create command.
                Set to a higher number so players can use more complex MiniMessage tags."""));
        this.bossbar_name_suggestions = getList("hotspots.bossbar.title.suggestions", List.of(
                "<#C000C0><bold>Magenta Hotspot",
                "<#00EDFF><obfuscated>Obfuscated Hotspot",
                "<gradient:#5e4fa2:#f79459:red>Gradient Hotspot",
                "<rainbow>Rainbow Hotspot"
        ), """
                The suggestions players will see in their tabcompletes when using
                the create command. This is mainly to let them know they can customize
                their hotspot and show them how.""");
        String colors_docs = "https://jd.advntr.dev/api/4.9.3/net/kyori/adventure/bossbar/BossBar.Color.html";
        this.bossbar_colors = getList("hotspots.bossbar.colors", Arrays.stream(BossBar.Color.values()).map(Enum::name).toList(), """
                Valid colors the a hotspot BossBar can have.
                Colors will be chosen randomly from this list on creation,
                should the player not specify it themselves.""" +
                "\nAvailable options:\n" + String.join(", ", Arrays.stream(BossBar.Color.values()).map(Enum::name).toList()))
                .stream()
                .distinct()
                .map(configuredColor -> {
                    try {
                        return BossBar.Color.valueOf(configuredColor);
                    } catch (IllegalArgumentException e) {
                        Hotspots.logger().warn("BossBar Color '{}' not recognized. Please use valid enums from: {}", configuredColor, colors_docs);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        String overlay_docs = "https://jd.advntr.dev/api/4.9.3/net/kyori/adventure/bossbar/BossBar.Overlay.html";
        final String configured_overlay = getString("hotspots.bossbar.overlay-type", BossBar.Overlay.PROGRESS.name(), """
                What overlay type the hotspot BossBar should be."""
                + "\nAvailable options:\n" + String.join(", ", Arrays.stream(BossBar.Overlay.values()).map(Enum::name).toList()));
        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(configured_overlay);
        } catch (IllegalArgumentException e) {
            Hotspots.logger().warn("BossBar Overlay '{}' not recognized. Please use valid enums from: {}", configured_overlay, overlay_docs);
            overlay = BossBar.Overlay.PROGRESS;
        }
        this.bossbar_overlay = overlay;

        String flags_docs = "https://jd.advntr.dev/api/4.9.3/net/kyori/adventure/bossbar/BossBar.Flag.html";
        this.bossbar_flags = getList("hotspots.bossbar.flags", List.of(BossBar.Flag.PLAY_BOSS_MUSIC.name()),
                "Flags to launch the boss bar with." +
                "\nAvailable options:\n" + String.join(", ", Arrays.stream(BossBar.Flag.values()).map(Enum::name).toList()))
                .stream()
                .map(configuredFlag -> {
                    try {
                        return BossBar.Flag.valueOf(configuredFlag);
                    } catch (IllegalArgumentException e) {
                        Hotspots.logger().warn("BossBar Flag '{}' not recognized. Please use valid enums from: {}", configuredFlag, flags_docs);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BossBar.Flag.class)));
    }

    public boolean saveConfig() {
        try {
            this.configFile.save();
            return true;
        } catch (Exception e) {
            Hotspots.logger().error("Failed to save config file!", e);
            return false;
        }
    }

    public ConfigFile master() {
        return this.configFile;
    }

    public boolean getBoolean(@NotNull String path, boolean def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getBoolean(path, def);
    }

    public boolean getBoolean(@NotNull String path, boolean def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getBoolean(path, def);
    }

    public @NotNull String getString(@NotNull String path, @NotNull String def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getString(path, def);
    }

    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getString(path, def);
    }

    public double getDouble(@NotNull String path, double def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getDouble(path, def);
    }

    public double getDouble(@NotNull String path, double def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getDouble(path, def);
    }

    public int getInt(@NotNull String path, int def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getInteger(path, def);
    }

    public int getInt(@NotNull String path, int def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getInteger(path, def);
    }

    public long getLong(@NotNull String path, long def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getLong(path, def);
    }

    public long getLong(@NotNull String path, long def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getLong(path, def);
    }

    public @NotNull <T> List<T> getList(@NotNull String path, @NotNull List<T> def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getList(path);
    }

    public @NotNull <T> List<T> getList(@NotNull String path, @NotNull List<T> def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getList(path);
    }
}
