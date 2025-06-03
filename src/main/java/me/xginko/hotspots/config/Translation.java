package me.xginko.hotspots.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.Title;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.LocaleUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public final class Translation {

    private final @NotNull ConfigFile langFile;
    public final @Nullable String translator;

    public final @NotNull List<Component> hotspot_cmd_overview, create_confirm;

    public final @NotNull Component cmd_no_permission, cmd_cooldown, plugin_busy, not_hosting_hotspot,
            create_success, create_failed_hotspot_limit, create_failed_color, create_failed_name_duplicate,
            create_failed_already_running, create_failed_height_limit, create_failed_spawn_too_close,
            create_failed_spawn_too_far, create_failed_name_too_long, create_failed_out_of_reach, join_success,
            join_failed_not_found, join_failed_teleport, join_warmup_title, join_warmup_subtitle, join_warmup_cancelled,
            join_failed_specify_name, notifs_disabled, notifs_enabled, end_success, confirm_nothing_pending,
            hotspot_ended_cancelled, hotspot_ended_expired, hotspot_ended_death, hotspot_started, hotspot_queued,
            hotspot_timeout, create_failed_playtime;

    public Translation(@NotNull Locale locale) throws Exception {
        Hotspots plugin = Hotspots.getInstance();
        String langFileName = LocaleUtil.languageTagForLocale(locale) + ".yml";
        File langYML = new File(plugin.getDataFolder() + "/lang", langFileName);
        File langDirectory = langYML.getParentFile();
        if (!langDirectory.exists()) {
            Files.createDirectories(langDirectory.toPath());
        }
        // Check if the file already exists and save the one from the plugin's resources folder if it does not
        if (!langYML.exists()) {
            plugin.saveResource("lang/" + langFileName, false);
        }
        // Finally, load the lang file with configmaster
        this.langFile = ConfigFile.loadConfig(langYML);

        this.langFile.setTitle(new Title().withWidth(120)
                .addSolidLine()
                .addLine(" ", Title.Pos.CENTER)
                .addLine("ʜᴏᴛsᴘᴏᴛs", Title.Pos.CENTER)
                .addLine("Translation for locale: " + locale, Title.Pos.CENTER)
                .addLine(" ", Title.Pos.CENTER)
                .addLine("Please use MiniMessage format: https://docs.advntr.dev/minimessage/format.html", Title.Pos.CENTER)
                .addLine(" ", Title.Pos.CENTER)
                .addSolidLine());

        this.langFile.addDefault("translator", "xginko");
        this.translator = langFile.getString("translator");

        // Broadcasts
        this.hotspot_started = getTranslation("hotspot.started",
                "<green>Hotspot '%hotspot_name%' has started. Teleport with <click:run_command:hotspot join %hotspot_name%>/hotspot join %hotspot_name%.");
        this.hotspot_queued = getTranslation("hotspot.queued",
                "<gray>Hotspot '%hotspot_name%' has been queued.");
        this.hotspot_timeout = getTranslation("hotspot.timeout",
                "<yellow>Hotspot '%hotspot_name%' has timed out due to %hotspot_owner% having left the game for longer than %time%.");
        this.hotspot_ended_cancelled = getTranslation("hotspot.ended.cancelled",
                "<gray>Hotspot %hotspot_name% has ended early.");
        this.hotspot_ended_expired = getTranslation("hotspot.ended.expired",
                "<gray>Hotspot %hotspot_name% has ended.");
        this.hotspot_ended_death = getTranslation("hotspot.ended.death",
                "<#C000C0>Hotspot %hotspot_name% has ended because %hotspot_owner% died.");

        // Commands
        this.cmd_no_permission = getTranslation("command.no-permission",
                "<red>You don't have permission to use this command.");
        this.cmd_cooldown = getTranslation("command.cooldown",
                "<gray>You are on cooldown. Please wait %time% before trying again.");
        this.plugin_busy = getTranslation("command.plugin-busy",
                "<yellow>Hotspots busy. Try again in a few seconds.");
        this.not_hosting_hotspot = getTranslation("command.not-hosting-hotspot",
                "<#C000C0>You don't have a hotspot running.");

        this.hotspot_cmd_overview = getListTranslation("command.hotspot.overview",
                "",
                "                          <#C000C0><bold>ʜᴏᴛsᴘᴏᴛs",
                "",
                "  <#C000C0>/hotspot join <name> <dark_gray>- <gray>Teleports you to a hotspot.",
                "  <#C000C0>/hotspot create (color) (name) <dark_gray>- <gray>Creates a hotspot.",
                "  <#C000C0>/hotspot end <dark_gray>- <gray>Ends your hotspot.",
                "  <#C000C0>/hotspot notifs off/on <dark_gray>- <gray>Toggles hotspot messages off/on.",
                "");

        this.create_success = getTranslation("command.hotspot.create.success",
                "<green>Created hotspot %hotspot_name%.");
        this.create_confirm = getListTranslation("command.hotspot.create.confirm",
                "",
                "  <yellow>Your Hotspot will be created with the following title:",
                "  %hotspot_title%",
                "  <gray>Confirm with <click:suggest_command:/hotspot confirm>/hotspot confirm.",
                "");
        this.create_failed_name_duplicate = getTranslation("command.hotspot.create.failed.name-exists",
                "<red>A Hotspot with a similar name is already running or queued. Try with a different name.");
        this.create_failed_name_too_long = getTranslation("command.hotspot.create.failed.name-too-long",
                "<red>The name you specified is too long.");
        this.create_failed_color = getTranslation("command.hotspot.create.failed.wrong-color",
                "<red>Color '%color_arg%' not recognized or allowed.");
        this.create_failed_hotspot_limit = getTranslation("command.hotspot.create.failed.limit-reached",
                "<red>The maximum number of hotspots (%max%) has been reached. Please wait for one of them to end and try again.");

        // Playtime
        this.create_failed_playtime = getTranslation("command.hotspot.create.failed.playtime",
                "<red>You need %time% more playtime on this server to use hotspots.");

        // World Bounds
        this.create_failed_height_limit = getTranslation("command.hotspot.create.failed.height-limit",
                "<red>Hotspots in this world are limited to y limits between %lower% and %upper%.");
        this.create_failed_already_running = getTranslation("command.hotspot.create.failed.already-running",
                "<yellow>You've already created a hotspot. End it with <click:run_command:hotspot end>/hotspot end");
        this.create_failed_out_of_reach = getTranslation("command.hotspot.create.failed.out-of-hotspot-reach",
                "<red>You are more than %blocks% blocks away from your initial location or in another world.");
        this.create_failed_spawn_too_close = getTranslation("command.hotspot.create.failed.spawn.too-close",
                "<#C000C0>You are too close to spawn. You need to be at least %min_distance% blocks away.");
        this.create_failed_spawn_too_far = getTranslation("command.hotspot.create.failed.spawn.too-far",
                "<#C000C0>You are too far away from spawn. You can only be %max_distance% blocks away from spawn.");

        this.join_success = getTranslation("command.hotspot.join.success",
                "<green>Teleported you to hotspot: %hotspot_name%!");
        this.join_warmup_title = getTranslation("command.hotspot.join.warmup.title",
                "<yellow>Teleport in %time%");
        this.join_warmup_subtitle = getTranslation("command.hotspot.join.warmup.subtitle",
                "<#C000C0>Don't move or take damage!");
        this.join_warmup_cancelled = getTranslation("command.hotspot.join.warmup.cancelled",
                "<red>Warmup cancelled!");
        this.join_failed_specify_name = getTranslation("command.hotspot.join.failed.specify-name",
                "<gray>Please specify a hotspot or player.");
        this.join_failed_not_found = getTranslation("command.hotspot.join.failed.not-found",
                "<red>No hotspot or player found with the name %name%");
        this.join_failed_teleport = getTranslation("command.hotspot.join.failed.teleport",
                "<red>Could not teleport you to hotspot. Try again in a few seconds.");

        this.end_success = getTranslation("command.hotspot.end.success",
                "<green>Hotspot '%hotspot%' ended successfully.");
        this.confirm_nothing_pending = getTranslation("command.hotspot.confirm.nothing-to-confirm",
                "<gray>Nothing to confirm.");

        this.notifs_disabled = getTranslation("command.hotspot.notifs.hidden",
                "<red>Hiding hotspot messages.");
        this.notifs_enabled = getTranslation("command.hotspot.notifs.shown",
                "<green>Showing hotspot messages.");

        try {
            this.langFile.save();
        } catch (Exception e) {
            Hotspots.logger().error("Failed to save translation file for {}.", locale, e);
        }
    }

    private @NotNull Component getTranslation(@NotNull String path, @NotNull String defaultTranslation) {
        this.langFile.addDefault(path, defaultTranslation);
        return MiniMessage.miniMessage().deserialize(AdventureUtil.replaceAmpersand(this.langFile.getString(path, defaultTranslation)));
    }

    private @NotNull List<Component> getListTranslation(@NotNull String path, @NotNull String... defaultTranslation) {
        this.langFile.addDefault(path, defaultTranslation);
        return this.langFile.getStringList(path).stream().map(AdventureUtil::replaceAmpersand).map(MiniMessage.miniMessage()::deserialize).toList();
    }
}