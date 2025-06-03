package me.xginko.hotspots.modules.extras;

import com.destroystokyo.paper.ParticleBuilder;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.hotspots.Hotspots;
import me.xginko.hotspots.Hotspot;
import me.xginko.hotspots.events.HotspotStartTickEvent;
import me.xginko.hotspots.events.HotspotStopTickEvent;
import me.xginko.hotspots.managers.HotspotManager;
import me.xginko.hotspots.managers.Manager;
import me.xginko.hotspots.modules.Module;
import me.xginko.hotspots.utils.MathHelper;
import me.xginko.hotspots.utils.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class HotspotParticleSphere extends Module implements Listener {

    private final Particle particle_type;
    private final int particle_update_ticks, particle_spawns, particle_count;

    private Map<UUID, ScheduledTask> particle_spheres;

    public HotspotParticleSphere() {
        super("extras.sphere-effect", true, """
                Enables a spherical particle effect around the center of the hotspot.\s
                Uses teleport radius for visual radius.""");
        this.particle_update_ticks = config.getInt(configPath + ".update-ticks", 1, """
                Update time in ticks.""");
        this.particle_spawns = config.getInt(configPath + ".particle-spawns", 300, """
                The amount of spawns around the sphere shape each time the task ticks.\s
                Affects effect density.""");
        this.particle_count = config.getInt(configPath + ".particle-count", 3, """
                The amount of particles that will be spawned at once each time the task ticks.\s
                Affects effect density.""");
        String particle_docs = "https://jd.papermc.io/paper/1.21/org/bukkit/Particle.html";
        final String configured_particle = config.getString(configPath + ".particle-type", Particle.WITCH.name(), """
                What particle type to use.""");
        Particle particle;
        try {
            particle = Particle.valueOf(configured_particle);
        } catch (IllegalArgumentException e) {
            warn("Particle '" + configured_particle + "' not recognized. Please use valid enums from: " + particle_docs);
            particle = Particle.WITCH;
        }
        this.particle_type = particle;
    }

    @Override
    public void enable() {
        particle_spheres = new ConcurrentHashMap<>(Hotspots.config().max_active_hotspots);
        Manager.get(HotspotManager.class).getActiveHotspots().forEach(this::startParticleTask);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (particle_spheres != null) {
            particle_spheres.forEach((hotspotID, particleTask) -> particleTask.cancel());
            particle_spheres.clear();
            particle_spheres = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onHotspotStartTick(HotspotStartTickEvent event) {
        startParticleTask(event.getHotspot());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onHotspotStopTick(HotspotStopTickEvent event) {
        stopParticleTask(event.getHotspot().getOwnerUUID());
    }

    private void stopParticleTask(UUID owner) {
        if (particle_spheres.containsKey(owner)) {
            particle_spheres.remove(owner).cancel();
        }
    }

    private void startParticleTask(Hotspot hotspot) {
        particle_spheres.computeIfAbsent(hotspot.getOwnerUUID(), k -> plugin.getServer().getRegionScheduler().runAtFixedRate(
                plugin,
                hotspot.getCenterLocation(),
                new SphereParticlesEffect(hotspot.getCenterLocation(), Hotspots.config().teleport_radius, particle_type, particle_spawns, particle_count),
                1L,
                particle_update_ticks
        ));
    }

    public static class SphereParticlesEffect extends ParticleBuilder implements Consumer<ScheduledTask> {

        private final @NotNull Location center, nextParticleLoc;
        private final double radius;
        private final int amount;

        public SphereParticlesEffect(@NotNull Location center, double radius, @NotNull Particle particleType, int amount, int countPerSpawn) {
            super(particleType);
            this.center = center;
            this.nextParticleLoc = center.clone(); // Clone so we don't end up altering center location in the accept method
            this.radius = radius;
            this.amount = amount;
            this.count(countPerSpawn);
        }

        @Override
        public void accept(ScheduledTask particleTask) {
            try {
                for (int i = 0; i < amount; i++) {
                    double theta = Util.RANDOM.nextDouble() * 2 * Math.PI;
                    double phi = MathHelper.acos(2 * Util.RANDOM.nextDouble() - 1);
                    double radSinPhi = radius * MathHelper.sin(phi);
                    location(nextParticleLoc.set(
                            MathHelper.fma(radSinPhi, MathHelper.cos(theta), center.getX()),
                            MathHelper.fma(radSinPhi, MathHelper.sin(theta), center.getY()),
                            MathHelper.fma(radius, MathHelper.cos(phi), center.getZ())
                    ));
                    spawn();
                }
            } catch (Throwable t) {
                particleTask.cancel();
                Hotspots.logger().error("Cancelled particle task due to error: {}", t.getLocalizedMessage());
                Hotspots.getInstance().onException();
            }
        }
    }
}
