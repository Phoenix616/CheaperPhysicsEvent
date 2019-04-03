package de.themoep.cheaperphysicsevent;

/*
 * CheaperPhysicsEvent
 * Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class CheaperPhysicsEvent extends JavaPlugin implements Listener {

    private RegisteredListener[] replacedListenerBaked = new RegisteredListener[0];
    private final List<RegisteredListener> replacedListener = new ArrayList<>();
    private final Set<String> plugins = new HashSet<>();
    
    @Override
    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlockDestroyed(BlockDestroyEvent event) {
        if (replacedListenerBaked.length > 0) {
            BlockPhysicsEvent physicsEvent = new BlockPhysicsEvent(event.getBlock(), event.getNewState());
            for (RegisteredListener listener : replacedListenerBaked) {
                try {
                    listener.callEvent(physicsEvent);
                } catch (EventException e) {
                    e.printStackTrace();
                }
            }
            if (physicsEvent.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        getLogger().log(Level.INFO, "Config:");
        for (String key : getConfig().getKeys(true)) {
            getLogger().log(Level.INFO, " " + key + ": " + getConfig().get(key));
        }

        plugins.clear();
        getConfig().getStringList("plugins").stream().map(String::toLowerCase).forEach(plugins::add);

        for (Iterator<RegisteredListener> it = replacedListener.iterator(); it.hasNext(); ) {
            RegisteredListener listener = it.next();
            if (!plugins.isEmpty() && !plugins.contains(listener.getPlugin().getName().toLowerCase())) {
                it.remove();
                BlockPhysicsEvent.getHandlerList().register(listener);
                getLogger().log(Level.INFO, "Restored previously replaced BlockPhysicsEvent listeners of " + listener.getPlugin().getName());
            }
        }

        replaceListeners();
    }

    private void replaceListeners() {
        for (RegisteredListener listener : BlockPhysicsEvent.getHandlerList().getRegisteredListeners()) {
            if (plugins.isEmpty() || plugins.contains(listener.getPlugin().getName().toLowerCase())) {
                BlockPhysicsEvent.getHandlerList().unregister(listener);
                replacedListener.add(listener);
                getLogger().log(Level.INFO, "Replaced BlockPhysicsEvent listener by " + listener.getPlugin().getName());
            }
        }
        replacedListener.sort(Comparator.comparing(RegisteredListener::getPriority));
        replacedListenerBaked = replacedListener.toArray(new RegisteredListener[0]);
    }

    @EventHandler
    public void onPluginEnable(ServerLoadEvent event) {
        replaceListeners();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        replaceListeners();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        replacedListener.removeIf(listener -> listener.getPlugin().equals(event.getPlugin()));
        replacedListenerBaked = replacedListener.toArray(new RegisteredListener[0]);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("cheaperphysicsevent.command.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            }
        }
        return false;
    }
}
