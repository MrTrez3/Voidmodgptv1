package dev.voidpowers;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;

public class VoidPowersClient implements ClientModInitializer {
    private static final String CATEGORY="key.categories.voidpowers";
    private static final List<KeyBinding> KEYS=new ArrayList<>();
    private static final List<Shadow> SHADOWS=new ArrayList<>();
    private static boolean hud=true;
    private static boolean voidOn=false;
    private static String selected="";
    private record Shadow(Entity entity,long born,long life){}
    private static KeyBinding key(String name,int key) { KeyBinding k=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.voidpowers."+name,InputUtil.Type.KEYSYM,key,CATEGORY)); KEYS.add(k); return k; }
    private static void send(String action) { if(ClientPlayNetworking.canSend(VoidPowers.ACTION)) { var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create(); b.writeString(action); ClientPlayNetworking.send(VoidPowers.ACTION,b); selected=action; if(action.equals("transform")) voidOn=!voidOn; } }
    @Override public void onInitializeClient() {
        key("transform",GLFW.GLFW_KEY_V); key("erase",GLFW.GLFW_KEY_R); key("telekinesis",GLFW.GLFW_KEY_G); key("throw",GLFW.GLFW_KEY_H); key("flight",GLFW.GLFW_KEY_F); key("force",GLFW.GLFW_KEY_B); key("blast",GLFW.GLFW_KEY_C); key("teleport",GLFW.GLFW_KEY_X); key("hud",GLFW.GLFW_KEY_UNKNOWN);
        ClientTickEvents.END_CLIENT_TICK.register(client->{ if(client.player==null)return;
            for(int i=0;i<KEYS.size();i++) while(KEYS.get(i).wasPressed()) {
                if(i==8) { hud=!hud; continue; }
                String[] names={"transform","erase","telekinesis","throw","flight","force","blast","teleport"}; send(names[i]);
            }
            if(client.player.age%5==0 && client.player.isInvisible()) { Vec3d p=client.player.getPos(); client.world.addParticle(ParticleTypes.SMOKE,p.x+(client.world.random.nextDouble()-.5)*.7,p.y+.2+client.world.random.nextDouble()*1.5,p.z+(client.world.random.nextDouble()-.5)*.7,0,-.01,0); }
        });
        ClientPlayNetworking.registerGlobalReceiver(VoidPowers.FX,(client,handler,buf,response)->{
            String kind=buf.readString(32);
            if(kind.equals("shadow")) {
                Identifier id=new Identifier(buf.readString(128)); double x=buf.readDouble(),y=buf.readDouble(),z=buf.readDouble();float yaw=buf.readFloat();
                client.execute(()-> { if(client.world==null)return; EntityType<?> type=Registries.ENTITY_TYPE.get(id); Entity e=type.create(client.world); if(e!=null){e.refreshPositionAndAngles(x,y,z,yaw,0);e.setSilent(true);SHADOWS.add(new Shadow(e,System.currentTimeMillis(),180000)); while(SHADOWS.size()>32)SHADOWS.remove(0);} });
            } else { double x=buf.readDouble(),y=buf.readDouble(),z=buf.readDouble(); client.execute(()->{if(client.world==null)return; for(int i=0;i<12;i++)client.world.addParticle(i%3==0?ParticleTypes.LARGE_SMOKE:ParticleTypes.SMOKE,x+(client.world.random.nextDouble()-.5),y+client.world.random.nextDouble(),z+(client.world.random.nextDouble()-.5),0,.015,0);}); }
        });
        HudRenderCallback.EVENT.register((draw, tickDelta) -> { if (hud) { var mc=net.minecraft.client.MinecraftClient.getInstance(); if (mc.player!=null) { String state=voidOn?"VOID FORM: ON":"VOID FORM: OFF"; draw.drawTextWithShadow(mc.textRenderer,state,8,8,0xFF9B55FF); if(!selected.isEmpty()) draw.drawTextWithShadow(mc.textRenderer,"Poder: "+selected,8,20,0xFFB9A6D8); } } });
        WorldRenderEvents.LAST.register(context->{
            if(context.world()==null)return;
            long now=System.currentTimeMillis();
            SHADOWS.removeIf(s->now-s.born>s.life);
            for(Shadow s:SHADOWS) {
                Entity e=s.entity; double x=e.getX()-context.camera().getPos().x,y=e.getY()-context.camera().getPos().y,z=e.getZ()-context.camera().getPos().z;
                RenderSystem.setShaderColor(.018f,.012f,.028f,.96f);
                context.matrixStack().push();
                try { net.minecraft.client.MinecraftClient.getInstance().getEntityRenderDispatcher().render(e,x,y,z,e.getYaw(),context.tickDelta(),context.matrixStack(),context.consumers(),0xF000F0); }
                catch(Exception ignored) { }
                finally { context.matrixStack().pop(); RenderSystem.setShaderColor(1,1,1,1); }
                if(context.world().random.nextInt(5)==0) context.world().addParticle(ParticleTypes.SMOKE,e.getX()+(context.world().random.nextDouble()-.5)*e.getWidth(),e.getY()+context.world().random.nextDouble()*e.getHeight(),e.getZ()+(context.world().random.nextDouble()-.5)*e.getWidth(),0,.01,0);
            }
        });
    }
    public static boolean hudEnabled(){return hud;}
    public static String selected(){return selected;}
}






