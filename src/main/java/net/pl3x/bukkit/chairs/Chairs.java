package net.pl3x.bukkit.chairs;

import com.mojang.datafixers.types.Type;
import net.minecraft.server.v1_13_R2.ChatComponentText;
import net.minecraft.server.v1_13_R2.DataConverterRegistry;
import net.minecraft.server.v1_13_R2.DataConverterTypes;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EntityTypes;
import net.minecraft.server.v1_13_R2.World;
import net.minecraft.server.v1_13_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
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
                EntityPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
                Block block = event.getClickedBlock();
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking() && block != null && Tag.STAIRS.isTagged(block.getType())) {
                    WorldServer world = ((CraftWorld) block.getWorld()).getHandle();
                    Chair chair = new Chair(world, block);
                    if (world.addEntity(chair)) {
                        player.yaw = chair.yaw;
                        player.pitch = 0;
                        if (player.startRiding(chair)) event.setCancelled(true);
                    }
                }
            }
        }, this);
        getServer().getScheduler().runTaskTimer(this, () -> Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(ArmorStand.class).stream()
                .filter(e -> !e.hasGravity() && e.isMarker() && !e.isVisible() && !e.isCustomNameVisible() && e.getCustomName().equals("chair") && e.getPassengers().isEmpty())
                .forEach(Entity::remove)), 6000, 6000);
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
            int degrees = ((Stairs) block.getBlockData()).getFacing().ordinal() * 90;
            double radians = Math.toRadians(degrees);
            Vector vec = block.getLocation().toVector().add(new Vector(0.5, 0.25, 0.5)).add(new Vector(-Math.sin(radians) * 0.2, 0, Math.cos(radians) * 0.2));
            setPositionRotation(vec.getX(), vec.getY(), vec.getZ(), degrees, 0);
        }

        public void k() {
            if (noRiderTicks > 5) die();
            if (passengers == null || passengers.isEmpty()) noRiderTicks++;
            else setYawPitch(passengers.get(0).yaw, noRiderTicks = 0);
            super.k();
        }
    }
}
