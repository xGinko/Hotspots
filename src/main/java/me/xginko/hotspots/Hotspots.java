package me.xginko.hotspots;

import com.destroystokyo.paper.exception.ServerPluginEnableDisableException;
import com.google.common.collect.ImmutableMap;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.hotspots.commands.PluginYMLCmd;
import me.xginko.hotspots.config.HotspotsConfig;
import me.xginko.hotspots.config.Translation;
import me.xginko.hotspots.data.Database;
import me.xginko.hotspots.data.SQLiteDatabase;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.utils.AdventureUtil;
import me.xginko.hotspots.utils.LocaleUtil;
import me.xginko.hotspots.utils.Util;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class Hotspots extends JavaPlugin {

    private static Hotspots instance;
    private static ComponentLogger logger;

    private static HotspotsConfig config;
    private static Map<Locale, Translation> translations;
    private static Database database;

    private AtomicInteger exceptionCount;
    private ScheduledTask exceptionCountResetTask;

    @Override
    public void onLoad() {
        // Disable info logging for shaded libs because it does not provide additional value to the user and makes startup log look ugly.
        String shadedLibs = getClass().getPackage().getName() + ".libs";
        Configurator.setLevel(shadedLibs + ".reflections.Reflections", Level.WARN);
        Configurator.setLevel(shadedLibs + ".zaxxer.hikari.pool.PoolBase", Level.WARN);
        Configurator.setLevel(shadedLibs + ".zaxxer.hikari.pool.HikariPool", Level.WARN);
        Configurator.setLevel(shadedLibs + ".zaxxer.hikari.HikariDataSource", Level.WARN);
        Configurator.setLevel(shadedLibs + ".zaxxer.hikari.HikariConfig", Level.WARN);
        Configurator.setLevel(shadedLibs + ".zaxxer.hikari.util.DriverDataSource", Level.WARN);
    }

    @Override
    public void onEnable() {
        instance = this;
        logger = getComponentLogger();
        exceptionCount = new AtomicInteger();
        exceptionCountResetTask = getServer().getAsyncScheduler().runAtFixedRate(instance,
                resetTask -> exceptionCount.set(0), 30, 30, TimeUnit.SECONDS);

        Util.getStartupLogo().forEach(logger::info);

        try {
            startup(reloadConfiguration(), "config read");
            startup(reloadTranslations(), translations.size() + (translations.size() == 1 ? " translation" : " translations"));
            startup(reloadDatabase(), "database");
            startup(Manager.reloadManagers(), "managers");
            startup(Module.reloadModules(), "modules");
            startup(PluginYMLCmd.reloadCommands(), "commands");
            startup(PluginPermission.registerAll(), "permissions");
            startup(config.saveConfig(), "config save");

            logger.info(Util.centerWithSpaces(Component.text(" All set.",
                    Style.style(AdventureUtil.ALACITY_MAGENTA, TextDecoration.BOLD)), 58));
        } catch (Throwable t) {
            logger.error("Error during enable. Disabling plugin.", t);
            getServer().getPluginManager().disablePlugin(instance);
        }
    }

    private void startup(boolean success, String string) throws ServerPluginEnableDisableException {
        logger.info(Util.startupResultLog(string, success, 58));
        if (!success) throw new ServerPluginEnableDisableException(new Exception(string), instance);
    }

    @Override
    public void onDisable() {
        PluginYMLCmd.disableAll();
        Module.disableAll();
        Manager.disableAll();
        PluginPermission.unregisterAll();
        if (database != null) {
            database.disable();
            database = null;
        }
        if (exceptionCountResetTask != null) {
            exceptionCountResetTask.cancel();
            exceptionCountResetTask = null;
            exceptionCount = null;
        }
        translations = null;
        config = null;
        logger = null;
        instance = null;
    }

    public void onException() {
        if (exceptionCount.incrementAndGet() > 15) {
            logger.error("Encountered more than 15 errors during the last 30 seconds! Soft-disabling.");
            softDisablePlugin();
        }
    }

    public static @NotNull Hotspots getInstance() {
        return instance;
    }

    public static @NotNull Database database() {
        return database;
    }

    public static @NotNull HotspotsConfig config() {
        return config;
    }

    public static @NotNull ComponentLogger logger() {
        return logger;
    }

    public static @NotNull Translation translation(Pointered pointered) {
        return translation(pointered.pointers().getOrDefault(Identity.LOCALE, config.default_locale));
    }

    public static @NotNull Translation translation(Locale locale) {
        if (config.auto_lang) return translations.getOrDefault(locale, translations.get(config.default_locale));
        return translations.get(config.default_locale);
    }

    public void softDisablePlugin() {
        Module.disableAll();
        Manager.disableAll();
        if (database != null) database.disable();
        logger.info(Component.text("Entered Soft-disabled state. Re-enable by running ", AdventureUtil.ALACITY_MAGENTA)
                .append(Component.text("/hotspots reload", Style.style(AdventureUtil.WHITE, TextDecoration.BOLD))));
    }

    public boolean reloadPlugin() {
        PluginYMLCmd.disableAll();
        Module.disableAll();
        Manager.disableAll();

        return      reloadConfiguration()
                &&  reloadTranslations()
                &&  reloadDatabase()
                &&  Manager.reloadManagers()
                &&  Module.reloadModules()
                &&  PluginYMLCmd.reloadCommands()
                &&  config.saveConfig();
    }

    public boolean reloadDatabase() {
        if (database != null) database.disable();
        database = new SQLiteDatabase();
        return database.createTables();
    }

    public boolean reloadConfiguration() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            config = new HotspotsConfig();
            return true;
        } catch (Exception e) {
            logger.error("Error loading config!", e);
            return false;
        }
    }

    public boolean reloadTranslations() {
        List<Locale> availableTranslations = getAvailableTranslations();

        // Remove all other translations if dynamic language switching is unwanted anyway
        if (!config.auto_lang) {
            availableTranslations.removeIf(locale -> locale != config.default_locale);
        }

        // Try to load available translation(s)
        Map<Locale, Translation> newTranslations = new HashMap<>(availableTranslations.size());
        for (Locale locale : availableTranslations) {
            try {
                newTranslations.put(locale, new Translation(locale));
            } catch (Exception e) {
                logger.warn("Translation for '{}' could not be loaded.", LocaleUtil.languageTagForLocale(locale), e);
            }
        }

        // Make sure we were able to parse at least one translation before applying new translations
        if (newTranslations.isEmpty()) {
            logger.error("Couldn't load any new translations.");
        } else {
            if (newTranslations.containsKey(config.default_locale)) { // Ensure default lang is available
                translations = ImmutableMap.copyOf(newTranslations);
                return true;
            } else {
                logger.error("Can't find translation for default language '{}'!", config.default_locale);
            }
        }

        // If we are here, new translation loading failed.

        if (translations == null) {
            // If we have no previous translations to fall back to, soft-disable to avoid possible exception spamming
            // from all translations being null
            logger.error("Unable to load translations. Soft-Disabling.");
            softDisablePlugin();
        }

        return false;
    }

    private @NotNull List<Locale> getAvailableTranslations() {
        try (final JarFile pluginJar = new JarFile(getFile())) {
            Stream<String> fileNames = null;

            // Check local folder first
            File langFolder = new File(getDataFolder(), "/lang");
            if (langFolder.exists()) {
                if (langFolder.isDirectory()) {
                    File[] files = langFolder.listFiles();
                    if (files != null && files.length > 0) {
                        fileNames = Arrays.stream(files).map(File::getName);
                    }
                } else {
                    String rename = langFolder.getPath() + ".non-directory";
                    logger.warn("Found weird non-directory file '{}'. Renaming to '{}'.", langFolder.getPath(), rename);
                    if (!langFolder.renameTo(new File(rename))) {
                        throw new FileAlreadyExistsException("Failed to rename file where lang directory is supposed to be.");
                    }
                }
            }

            // Try to create lang directory if it doesn't already exist
            Files.createDirectories(langFolder.toPath());

            if (fileNames == null) {
                // If folder did not exist or was empty, we look for language files inside the plugin jar
                fileNames = pluginJar.stream().map(ZipEntry::getName);
            }

            return fileNames
                    .map(LocaleUtil.TRANSLATION_YML_PATTERN::matcher) // Checks for files looking like "en_us.yml"
                    .filter(Matcher::find) // Keep the matching files
                    .map(matcher -> matcher.group(1)) // Isolate "en_us"
                    .distinct() // remove any duplicates
                    .sorted() // sort alphabetically
                    .map(LocaleUtil::localeForLanguageTag) // turn string into best matching Locale
                    .collect(Collectors.toList()); // Don't use toList() because we need it to be mutable
        } catch (IOException e) {
            logger.error("Failed while searching for available translations!", e);
            return Collections.emptyList();
        }
    }
}
