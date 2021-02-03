package io.github.addoncommunity.galactifun.api.universe.world;

import io.github.addoncommunity.galactifun.api.mob.Alien;
import io.github.addoncommunity.galactifun.api.universe.CelestialBody;
import io.github.addoncommunity.galactifun.api.universe.attributes.Atmosphere;
import io.github.addoncommunity.galactifun.api.universe.attributes.DayCycle;
import io.github.addoncommunity.galactifun.api.universe.attributes.Gravity;
import io.github.addoncommunity.galactifun.base.milkyway.solarsystem.earth.Earth;
import io.github.addoncommunity.galactifun.core.GalacticRegistry;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.cscorelib2.collections.RandomizedSet;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A class representing any celestial object with a world
 *
 * @author Seggan
 * @author Mooy1
 */
public abstract class CelestialWorld extends ACelestialWorld {

    private static final double MIN_BORDER = 600D;
    
    @Nonnull
    private final RandomizedSet<Alien> species = new RandomizedSet<>();
    
    @Getter
    private final int alienSpawnChance;
    
    @Getter
    private final int avgHeight;
    
    public CelestialWorld(@Nonnull String name, long distance, long surfaceArea, @Nonnull Gravity gravity, @Nonnull Material material,
                          @Nonnull DayCycle dayCycle, @Nonnull AWorldTerrain terrain, @Nonnull Atmosphere atmosphere, int alienSpawnChance, int avgHeight, @Nonnull CelestialBody... celestialBodies) {
        super(name, distance, surfaceArea, gravity, material, dayCycle, terrain, atmosphere, celestialBodies);
        this.alienSpawnChance = alienSpawnChance;
        this.avgHeight = avgHeight;
    }
    
    /**
     * Sets up and creates the world
     */
    @Nonnull
    @Override
    protected final World createWorld() {

        String worldName = ChatColor.stripColor(this.name).toLowerCase(Locale.ROOT).replace(' ', '_');

        // fetch or create world
        World world = new WorldCreator(worldName)
                .generator(((AWorldTerrain) this.terrain).createGenerator(this))
                .environment(this.atmosphere.getEnvironment())
                .createWorld();

        Validate.notNull(world, "There was an error loading the world for " + this.name);

        // load effects
        this.dayCycle.applyEffects(world);
        this.atmosphere.applyEffects(world);

        // border
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(Math.max(MIN_BORDER, Math.sqrt(this.surfaceArea) * Earth.BORDER_SURFACE_RATIO));

        // register
        GalacticRegistry.register(world, this);

        // block storage
        if (BlockStorage.getStorage(world) == null) {
            new BlockStorage(world);
        }

        return world;
    }
    
    /**
     * The material for the block that should be generated at the specified x, y, z value of the chunk.
     *
     * The top value is used so that you can set the top 3 blocks to a different material for ex.
     */
    @Nonnull
    public abstract Material generateBlock(@Nonnull Random random, int top, int x, int y, int z);

    /**
     * The biome that should be used for the chunk at the specified x and z
     */
    @Nonnull
    public abstract Biome generateBiome(@Nonnull Random random, int chunkX, int chunkZ);

    /**
     * Add all chunk populators to this list
     */
    public abstract void getPopulators(@Nonnull List<BlockPopulator> populators);

    public final void addSpecies(@Nonnull Alien alien) {
        this.species.add(alien, alien.getChance());
    }
    
    /**
     * Ticks the world
     */
    public void tickWorld() {
        // time
        this.dayCycle.applyTime(this.world);

        // player effects
        for (Player p : this.world.getPlayers()) {
            applyEffects(p);
        }

        // other stuff?
    }

    /**
     * All effects that should be applied to the player
     */
    public void applyEffects(@Nonnull Player p) {
        // apply gravity
        this.gravity.applyGravity(p);

        // apply atmospheric effects TODO needs to be improved a lot
        for (PotionEffectType effectType : this.atmosphere.getNormalEffects()) {
            effectType.createEffect(180, 0).apply(p);
        }

        // other stuff?
    }

    /**
     * Careful when overriding, this spawns aliens
     */
    public void onMobSpawn(@Nonnull CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            e.setCancelled(true);
            if (ThreadLocalRandom.current().nextInt(100) <= this.alienSpawnChance) {
                Alien alien = this.species.getRandom();
                if (alien.canSpawn(e.getLocation())) {
                    alien.spawn(e.getLocation());
                }
            }
        }
    }
    
}
