package net.pl3x.bukkit.chairs;

import com.mojang.datafixers.types.Type;
import net.minecraft.server.v1_13_R2.ChatComponentText;
import net.minecraft.server.v1_13_R2.DataConverterRegistry;
import net.minecraft.server.v1_13_R2.DataConverterTypes;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.World;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;

public class Chairs extends JavaPlugin {
    @Override
    public void onLoad() {
        Map<Object, Type<?>> dataTypes = (Map<Object, Type<?>>) DataConverterRegistry.a().getSchema(15190).findChoiceType(DataConverterTypes.n).types();
        dataTypes.put("minecraft:chair", dataTypes.get("minecraft:armorstand"));
        EntityTypes.a("chair", EntityTypes.a.a(Chair.class, Chair::new));
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClickChair(PlayerInteractEvent event) {
                Block block = event.getClickedBlock();
                if (block == null || !Tag.STAIRS.isTagged(block.getType())) {
                    return;
                }

                EntityPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
                WorldServer world = ((CraftWorld) block.getWorld()).getHandle();

                Chair chair = new Chair(world, block);
                if (world.addEntity(chair)) {
                    player.yaw = chair.yaw;
                    player.pitch = 0;
                    player.a(chair, true);
                }
            }
        }, this);

        new BukkitRunnable() {
            public void run() {
                Bukkit.getWorlds().forEach(world ->
                        world.getEntitiesByClass(ArmorStand.class).stream()
                                .filter(e -> !e.hasGravity()                 // has no gravity
                                        && e.isMarker()                      // is marker
                                        && !e.isVisible()                    // is invisible
                                        && !e.isCustomNameVisible()          // custom name is hidden
                                        && e.getCustomName().equals("chair") // custom name is chair
                                        && e.getPassengers().isEmpty())      // has no passengers
                                .forEach(org.bukkit.entity.Entity::remove)); // kill it
            }
        }.runTaskTimer(this, 300, 300); // 5 minutes
    }

    public class Chair extends EntityArmorStand {
        private int noRiderTicks = 0;

        public Chair(World world) {
            super(world);
            setMarker(true);
            setNoGravity(true);
            setCustomName(new ChatComponentText("chair"));
            setCustomNameVisible(false);
            setInvisible(true);
        }

        public Chair(World world, Block block) {
            this(world);

            int degrees = 0;
            BlockFace facing = ((Stairs) block.getBlockData()).getFacing();
            if (facing == BlockFace.EAST)
                degrees = 90;
            if (facing == BlockFace.SOUTH)
                degrees = 180;
            if (facing == BlockFace.WEST)
                degrees = -90;

            Vector vec = block.getLocation().toVector() // block location
                    .add(new Vector(0.5, 0.35, 0.5)) // center seat position
                    .add(rotateVectorAroundY(new Vector(0, 0, 0.2), degrees)); // rotate seat position

            setPositionRotation(vec.getX(), vec.getY(), vec.getZ(), degrees, 0);
        }

        @Override
        public void k() {
            if (noRiderTicks > 10) {
                setMarker(false);
                ejectPassengers();
                die();
            }

            EntityPlayer rider = getRider();
            if (rider == null) {
                noRiderTicks++;
            } else {
                noRiderTicks = 0;
                setYawPitch(rider.yaw, 0);
            }

            super.k();
        }

        private EntityPlayer getRider() {
            if (passengers == null || passengers.isEmpty()) {
                return null;
            }
            Entity entity = passengers.get(0);
            return entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        }

        private Vector rotateVectorAroundY(Vector vec, double degrees) {
            double rad = Math.toRadians(degrees);
            double cos = Math.cos(rad);
            double sine = Math.sin(rad);
            double x = vec.getX();
            double z = vec.getZ();
            vec.setX(cos * x - sine * z);
            vec.setZ(sine * x + cos * z);
            return vec;
        }
    }
}
