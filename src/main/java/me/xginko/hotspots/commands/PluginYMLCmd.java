package me.xginko.hotspots.commands;

import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.commands.admin.HotspotsCommand;
import me.xginko.hotspots.commands.player.HotspotCommand;
import me.xginko.hotspots.utils.Disableable;
import me.xginko.hotspots.utils.Enableable;
import org.bukkit.command.CommandException;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class PluginYMLCmd extends BaseCommand implements Enableable, Disableable {

    protected static final Set<PluginYMLCmd> COMMANDS = new HashSet<>(2);

    public final PluginCommand pluginCommand;

    protected PluginYMLCmd(@NotNull String label) throws CommandException {
        super(label);
        this.pluginCommand = Hotspots.getInstance().getCommand(label);
        if (pluginCommand == null)
            throw new CommandException("Command '/" + label + "' cannot be enabled because it's not defined in the plugin.yml.");
    }

    public static void disableAll() {
        COMMANDS.forEach(Disableable::disable);
        COMMANDS.clear();
    }

    public static boolean reloadCommands() {
        disableAll();
        boolean success = true;
        try {
            for (PluginYMLCmd pluginYMLCmd : Set.of(new HotspotCommand(), new HotspotsCommand())) {
                pluginYMLCmd.enable();
                COMMANDS.add(pluginYMLCmd);
            }
        } catch (Throwable t) {
            success = false;
        }
        return success;
    }

    @Override
    public void enable() {
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public void disable() {
        VoidCommand voided = new VoidCommand(label());
        pluginCommand.setTabCompleter(voided);
        pluginCommand.setExecutor(voided);
        pluginCommand.unregister(Hotspots.getInstance().getServer().getCommandMap());
    }
}
