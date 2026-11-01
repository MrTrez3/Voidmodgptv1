package dev.voidpowers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.*;

public class VoidPowers implements ModInitializer {
    public static final String MOD_ID = "voidpowers";
    public static final Identifier ACTION = new Identifier(MOD_ID, "action");
    public static final Identifier FX = new Identifier(MOD_ID, "fx");
    private static final Set<UUID> VOID = new HashSet<>();
    private static final Map<UUID, Integer> FORCE_TICKS = new HashMap<>();
    private static final Map<UUID, Double> BASE_DAMAGE = new HashMap<>();
    private static final Map<UUID, Integer> CONSUMING = new HashMap<>();
    private static final Map<UUID, UUID> HELD = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    @Override public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, response) -> {
            String action = buf.readString(32);
            server.execute(() -> handle(player, action));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) tick(p);
        });
    }

    private static void handle(ServerPlayerEntity p, String action) {
        UUID id = p.getUuid();
        switch (action) {
            case "transform" -> toggle(p);
            case "erase" -> { if (VOID.contains(id)) erase(p); }
            case "flight" -> { if (VOID.contains(id)) { p.getAbilities().allowFlying = !p.getAbilities().allowFlying; p.getAbilities().flying = p.getAbilities().allowFlying; p.sendAbilitiesUpdate(); } }
            case "force" -> { if (VOID.contains(id)) FORCE_TICKS.put(id, 100); }
            case "throw" -> { if (VOID.contains(id)) throwHeld(p); }
            case "telekinesis" -> { if (VOID.contains(id)) telekinesis(p); }
            case "blast" -> { if (VOID.contains(id)) blast(p); }
            case "teleport" -> { if (VOID.contains(id)) teleport(p); }
        }
    }

    private static void toggle(ServerPlayerEntity p) {
        if (VOID.remove(p.getUuid())) {
            p.getAbilities().allowFlying = false; p.getAbilities().flying = false; p.sendAbilitiesUpdate();
            EntityAttributeInstance damage = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (damage != null) damage.setBaseValue(BASE_DAMAGE.getOrDefault(p.getUuid(), 1.0));
            BASE_DAMAGE.remove(p.getUuid()); p.sendMessage(net.minecraft.text.Text.literal("§7Forma Void desativada."), true);
        } else {
            VOID.add(p.getUuid());
            EntityAttributeInstance damage = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (damage != null) { BASE_DAMAGE.put(p.getUuid(), damage.getBaseValue()); damage.setBaseValue(Math.max(12, damage.getBaseValue() * 3)); }
            p.sendMessage(net.minecraft.text.Text.literal("§5§lVOID FORM: ON"), true);
            fx(p, "transform", p.getX(), p.getY(), p.getZ());
        }
    }

    private static void tick(ServerPlayerEntity p) {
        UUID id = p.getUuid();
        
            p.fallDistance = 0;
            if (VOID.contains(id)) {
            if (p.age % (p.getAbilities().flying ? 5 : 12) == 0) fx(p, "aura", p.getX(), p.getY(), p.getZ());
            UUID heldId = HELD.get(id);
            if (heldId != null && p.getWorld() instanceof ServerWorld sw) {
                Entity e = sw.getEntity(heldId);
                if (e == null || !e.isAlive() || p.squaredDistanceTo(e) > 900) HELD.remove(id);
                else { Vec3d target = p.getPos().add(p.getRotationVector().multiply(4)).add(0, 1, 0); e.setVelocity(target.subtract(e.getPos()).multiply(0.22)); e.velocityModified = true; }
            }
        }
        FORCE_TICKS.computeIfPresent(id, (k, n) -> { if (n <= 1) return null; forceNearby(p); return n - 1; });
        COOLDOWNS.replaceAll((k,v) -> Math.max(0,v-1));
        if (p.age % 20 == 0) COOLDOWNS.entrySet().removeIf(e -> e.getValue() <= 0);
        CONSUMING.entrySet().removeIf(e -> {
            Entity target = p.getWorld().getEntity(e.getKey());
            if (target == null || !target.isAlive()) return true;
            int t = e.getValue() + 1;
            if (target instanceof LivingEntity living) {
                living.setVelocity(0, 0.015, 0); living.velocityModified = true;
                living.setGlowing(true);
                if (t % 4 == 0) fx(p, "consume", target.getX(), target.getY()+target.getHeight()/2, target.getZ());
            }
            if (t >= 30) {
                double x=target.getX(), y=target.getY(), z=target.getZ();
                String type=net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()).toString(); float yaw=target.getYaw();
                if (p.getWorld() instanceof ServerWorld sw) sw.getPlayers().forEach(other -> sendShadow(other, type, x,y,z,yaw));
                target.discard(); fx(p, "erase_end", x,y,z); return true;
            }
            e.setValue(t); return false;
        });
    }

    private static void erase(ServerPlayerEntity p) {
        LivingEntity target = aimed(p, 24);
        if (target == null || target == p || target.isInvulnerable() || CONSUMING.containsKey(target.getUuid())) return;
        CONSUMING.put(target.getUuid(), 0); target.setGlowing(true);
        fx(p,"consume",target.getX(),target.getY()+target.getHeight()/2,target.getZ());
    }

    private static LivingEntity aimed(ServerPlayerEntity p, double range) {
        Vec3d start=p.getCameraPosVec(1), end=start.add(p.getRotationVector().multiply(range));
        Box box=p.getBoundingBox().stretch(p.getRotationVector().multiply(range)).expand(1.0);
        Entity hit=null; double best=range*range;
        for(Entity e:p.getWorld().getOtherEntities(p,box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
            Box eb=e.getBoundingBox().expand(e.getTargetingMargin()); var result=eb.raycast(start,end);
            if(result.isPresent()) { double d=start.squaredDistanceTo(result.get()); if(d<best) { best=d; hit=e; } }
        }
        if(hit==null) return null;
        BlockHitResult block=p.getWorld().raycast(new RaycastContext(start,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        if(block.getType()!=HitResult.Type.MISS && start.squaredDistanceTo(block.getPos())<best) return null;
        return (LivingEntity)hit;
    }

    private static void telekinesis(ServerPlayerEntity p) {
        UUID id=p.getUuid(); if(HELD.containsKey(id)) { HELD.remove(id); return; }
        LivingEntity target=aimed(p,20); if(target==null || target==p) return; HELD.put(id,target.getUuid()); fx(p,"lift",target.getX(),target.getY(),target.getZ());
    }
    private static void throwHeld(ServerPlayerEntity p) {
        UUID targetId=HELD.remove(p.getUuid()); if(targetId==null)return;
        Entity e=p.getWorld().getEntity(targetId); if(e!=null){ Vec3d v=p.getRotationVector().multiply(2.8).add(0,0.5,0); e.setVelocity(v); e.velocityModified=true; }
    }
    private static void forceNearby(ServerPlayerEntity p) {
        for(Entity e:p.getWorld().getOtherEntities(p,p.getBoundingBox().expand(4),e->e instanceof LivingEntity && !e.isInvulnerable())) {
            Vec3d v=e.getPos().subtract(p.getPos()).normalize().multiply(1.1).add(0,0.35,0); e.setVelocity(v); e.velocityModified=true;
        }
    }
    private static void blast(ServerPlayerEntity p) {
        LivingEntity target=aimed(p,28); if(target==null||target==p)return;
        target.damage(p.getDamageSources().playerAttack(p), 12); Vec3d v=target.getPos().subtract(p.getPos()).normalize().multiply(1.6).add(0,.4,0); target.setVelocity(v); target.velocityModified=true;
        fx(p,"blast",target.getX(),target.getY()+1,target.getZ());
    }
    private static void teleport(ServerPlayerEntity p) {
        Vec3d start=p.getCameraPosVec(1), end=start.add(p.getRotationVector().multiply(32));
        BlockHitResult hit=p.getWorld().raycast(new RaycastContext(start,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        if(hit.getType()==HitResult.Type.MISS)return;
        Vec3d dest=hit.getPos().subtract(p.getRotationVector().multiply(1.2));
        if(p.getWorld().isSpaceEmpty(p,p.getBoundingBox().offset(dest.subtract(p.getPos())))) { fx(p,"teleport",p.getX(),p.getY(),p.getZ()); p.teleport(p.getServerWorld(),dest.x,dest.y,dest.z,p.getYaw(),p.getPitch()); fx(p,"teleport",dest.x,dest.y,dest.z); }
    }
    private static void fx(ServerPlayerEntity source,String kind,double x,double y,double z) {
        if(!(source.getWorld() instanceof ServerWorld sw))return;
        PacketByteBuf b=PacketByteBufs.create(); b.writeString(kind); b.writeDouble(x); b.writeDouble(y); b.writeDouble(z);
        for(ServerPlayerEntity p:sw.getPlayers()) ServerPlayNetworking.send(p,FX,b);
    }
    private static void sendShadow(ServerPlayerEntity p,String type,double x,double y,double z,float yaw) {
        PacketByteBuf b=PacketByteBufs.create(); b.writeString("shadow"); b.writeString(type); b.writeDouble(x); b.writeDouble(y); b.writeDouble(z); b.writeFloat(yaw); ServerPlayNetworking.send(p,FX,b);
    }
    public static boolean isVoid(PlayerEntity p){ return VOID.contains(p.getUuid()); }
}





