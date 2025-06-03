package me.xginko.hotspots.commands.player;

import com.google.common.collect.ImmutableList;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.commands.BaseCommand;
import me.xginko.hotspots.commands.PluginYMLCmd;
import me.xginko.hotspots.commands.player.subcommands.ConfirmSubCmd;
import me.xginko.hotspots.commands.player.subcommands.CreateSubCmd;
import me.xginko.hotspots.commands.player.subcommands.EndSubCmd;
import me.xginko.hotspots.commands.player.subcommands.JoinSubCmd;
import me.xginko.hotspots.commands.player.subcommands.NotifsSubCmd;
import me.xginko.hotspots.events.HotspotStartTickEvent;
import me.xginko.hotspots.events.HotspotStopTickEvent;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.utils.Disableable;
import me.xginko.hotspots.utils.Enableable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class HotspotCommand extends PluginYMLCmd implements Listener {

    private final @NotNull List<BaseCommand> subCommands;
    private final @NotNull List<String> subCommandTabCompletes;
    private static List<String> hotspotTabCompletes;

    public HotspotCommand() {
        super("hotspot");
        hotspotTabCompletes = new CopyOnWriteArrayList<>();
        subCommands = ImmutableList.of(new NotifsSubCmd(), new JoinSubCmd(), new CreateSubCmd(), new ConfirmSubCmd(), new EndSubCmd());
        subCommandTabCompletes = ImmutableList.copyOf(subCommands.stream().map(BaseCommand::label).toList());
    }

    @Override
    public void enable() {
        for (BaseCommand subCommand : subCommands) {
            if (subCommand instanceof Enableable enableable)
                enableable.enable();
        }

        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);

        if (Manager.get(HotspotManager.class) != null) {
            for (Hotspot hotspot : Manager.get(HotspotManager.class).getActiveHotspots()) {
                hotspotTabCompletes.add(hotspot.getName());
                hotspotTabCompletes.add(hotspot.getOwnerName());
            }
        }

        Hotspots plugin = Hotspots.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        for (BaseCommand subCommand : subCommands) {
            if (subCommand instanceof Disableable disableable)
                disableable.disable();
        }

        HandlerList.unregisterAll(this);
        if (hotspotTabCompletes != null) {
            hotspotTabCompletes.clear();
            hotspotTabCompletes = null;
        }

        VoidCommand voided = new VoidCommand(label());
        pluginCommand.setTabCompleter(voided);
        pluginCommand.setExecutor(voided);
        pluginCommand.unregister(Hotspots.getInstance().getServer().getCommandMap());
    }

    public static List<String> getHotspotTabCompletes() {
        return hotspotTabCompletes;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onHotspotStartTick(HotspotStartTickEvent event) {
        if (!hotspotTabCompletes.contains(event.getHotspot().getName()))
            hotspotTabCompletes.add(event.getHotspot().getName());
        if (!hotspotTabCompletes.contains(event.getHotspot().getOwnerName()))
            hotspotTabCompletes.add(event.getHotspot().getOwnerName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onHotspotStopTick(HotspotStopTickEvent event) {
        hotspotTabCompletes.remove(event.getHotspot().getName());
        hotspotTabCompletes.remove(event.getHotspot().getOwnerName());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return subCommandTabCompletes.stream()
                    .filter(cmd -> cmd.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length > 1) {
            for (BaseCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.label())) {
                    return subCommand.onTabComplete(sender, command, alias, args);
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length >= 1) {
            for (BaseCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.label())) {
                    return subCommand.onCommand(sender, command, alias, args);
                }
            }
        }

        Hotspots.translation(sender).hotspot_cmd_overview.forEach(sender::sendMessage);
        return true;
    }
}
