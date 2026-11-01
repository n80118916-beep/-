package ru.tusiclient;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = "tusiclient", name = "TusiClient", version = "1.0")
public class TusiClient {
    
    public static KeyBinding openGuiKey;
    public static List<Module> modules = new ArrayList<>();
    public static Minecraft mc = Minecraft.getMinecraft();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openGuiKey = new KeyBinding("Меню TusiClient", Keyboard.KEY_RSHIFT, "TusiClient");
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(openGuiKey);
        
        modules.add(new Module("Fullbright", "Яркость", "Render", true));
        modules.add(new Module("ESP", "Подсветка игроков", "Render", false));
        modules.add(new Module("Tracers", "Линии до игроков", "Render", false));
        modules.add(new Module("Velocity", "Убирает откидывание", "Combat", false));
        modules.add(new Module("NoHurtCam", "Убирает тряску", "Render", true));

        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openGuiKey.isPressed()) {
            mc.displayGuiScreen(new GuiTusiClient());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;
        
        if (isEnabled("Fullbright")) mc.gameSettings.gammaSetting = 100.0F;
        else if (mc.gameSettings.gammaSetting == 100.0F) mc.gameSettings.gammaSetting = 1.0F;
        
        if (isEnabled("NoHurtCam")) { mc.thePlayer.hurtTime = 0; mc.thePlayer.maxHurtTime = 0; }
        if (isEnabled("Velocity") && mc.thePlayer.hurtTime > 0) { mc.thePlayer.motionX *= 0.1; mc.thePlayer.motionZ *= 0.1; }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving == mc.thePlayer && isEnabled("Velocity")) {
            event.entityLiving.motionX = 0; event.entityLiving.motionZ = 0;
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        // Заглушка для будущих функций
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        boolean esp = isEnabled("ESP"), tracers = isEnabled("Tracers");
        if (!esp && !tracers) return;
        
        GL11.glPushMatrix(); 
        GL11.glDisable(GL11.GL_TEXTURE_2D); 
        GL11.glDisable(GL11.GL_DEPTH_TEST); 
        GL11.glEnable(GL11.GL_BLEND); 
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); 
        GL11.glLineWidth(2.0F);
        
        float pTicks = event.partialTicks;
        double rX = RenderManager.instance.renderPosX, rY = RenderManager.instance.renderPosY, rZ = RenderManager.instance.renderPosZ;
        
        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) continue;
            
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * pTicks - rX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * pTicks - rY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pTicks - rZ;
            
            if (tracers) { 
                GL11.glColor4f(1F, 0F, 0F, 1F); 
                GL11.glBegin(GL11.GL_LINES); 
                GL11.glVertex3d(0, -0.1, 0); 
                GL11.glVertex3d(x, y + player.getEyeHeight() / 2, z); 
                GL11.glEnd(); 
            }
            if (esp) { 
                GL11.glColor4f(0F, 1F, 0F, 1F); 
                drawBox(x, y, z, player.width / 2, player.height); 
            }
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST); 
        GL11.glEnable(GL11.GL_TEXTURE_2D); 
        GL11.glDisable(GL11.GL_BLEND); 
        GL11.glPopMatrix();
    }

    private void drawBox(double x, double y, double z, double w, double h) {
        GL11.glBegin(GL11.GL_LINE_LOOP); GL11.glVertex3d(x - w, y, z - w); GL11.glVertex3d(x + w, y, z - w); GL11.glVertex3d(x + w, y, z + w); GL11.glVertex3d(x - w, y, z + w); GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP); GL11.glVertex3d(x - w, y + h, z - w); GL11.glVertex3d(x + w, y + h, z - w); GL11.glVertex3d(x + w, y + h, z + w); GL11.glVertex3d(x - w, y + h, z + w); GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES); GL11.glVertex3d(x - w, y, z - w); GL11.glVertex3d(x - w, y + h, z - w); GL11.glVertex3d(x + w, y, z - w); GL11.glVertex3d(x + w, y + h, z - w); GL11.glVertex3d(x + w, y, z + w); GL11.glVertex3d(x + w, y + h, z + w); GL11.glVertex3d(x - w, y, z + w); GL11.glVertex3d(x - w, y + h, z + w); GL11.glEnd();
    }

    public static boolean isEnabled(String name) { 
        for (Module m : modules) if (m.name.equals(name)) return m.enabled; 
        return false; 
    }
    
    public static List<Module> getModules(String category) { 
        List<Module> list = new ArrayList<>(); 
        for (Module m : modules) if (m.category.equals(category)) list.add(m); 
        return list; 
    }
    
    public static class Module {
        public String name, desc, category; 
        public boolean enabled;
        public Module(String name, String desc, String category, boolean enabled) { 
            this.name = name; this.desc = desc; this.category = category; this.enabled = enabled; 
        }
    }

    public static class GuiTusiClient extends GuiScreen {
        private int x, y, width = 520, height = 360;
        private final int SIDEBAR = 130;
        private String category = "Render"; 
        private final int BG = 0xFF121214, SIDE = 0xFF18181B, MOD = 0xFF27272A, TEXT = 0xFFFAFAFA, DESC = 0xFFA1A1AA;

        @Override
        public void initGui() { this.x = (this.width / 2) - (this.width / 2); this.y = (this.height / 2) - (this.height / 2); }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, this.width, this.height, 0xB4000000);
            drawRect(x, y, x + width, y + height, BG); 
            drawRect(x, y, x + SIDEBAR, y + height, SIDE);
            
            drawRect(x + 15, y + 15, x + 45, y + 45, 0xFFB42828);
            fontRendererObj.drawStringWithShadow("TusiClient", x + 55, y + 25, TEXT);
            
            drawCat("Combat", x + 15, y + 70, mouseX, mouseY); 
            drawCat("Render", x + 15, y + 100, mouseX, mouseY);
            
            List<Module> list = getModules(category);
            int startX = x + SIDEBAR + 20, startY = y + 60, col = 0, row = 0;
            for (Module mod : list) {
                int mx = startX + (col * 190), my = startY + (row * 45);
                if (mod.enabled) drawGradient(mx, my, 180, 38, 0xFF3B82F6, 0xFF8B5CF6);
                else drawRect(mx, my, mx + 180, my + 38, MOD);
                
                fontRendererObj.drawStringWithShadow(mod.name, mx + 8, my + 6, TEXT);
                
                GL11.glPushMatrix(); 
                GL11.glScalef(0.75f, 0.75f, 1f); 
                fontRendererObj.drawString(mod.desc, (int)((mx + 8) / 0.75f), (int)((my + 20) / 0.75f), DESC); 
                GL11.glPopMatrix();
                
                col++; if (col >= 2) { col = 0; row++; }
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        private void drawCat(String name, int cx, int cy, int mx, int my) {
            boolean active = category.equals(name);
            if (active) drawGradient(cx, cy, SIDEBAR - 20, 22, 0xFF3B82F6, 0xFF8B5CF6);
            else if (mx >= cx && mx <= cx + SIDEBAR - 20 && my >= cy && my <= cy + 20) drawRect(cx, cy, cx + SIDEBAR - 20, cy + 22, MOD);
            fontRendererObj.drawString(name, cx + 10, cy + 7, active ? -1 : TEXT);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseY >= y + 70 && mouseY <= y + 92) { category = "Combat"; }
            if (mouseY >= y + 100 && mouseY <= y + 122) { category = "Render"; }
            
            List<Module> list = getModules(category);
            int startX = x + SIDEBAR + 20, startY = y + 60, col = 0, row = 0;
            for (Module mod : list) {
                int mx = startX + (col * 190), my = startY + (row * 45);
                if (mouseX >= mx && mouseX <= mx + 180 && mouseY >= my && mouseY <= my + 38) { mod.enabled = !mod.enabled; }
                col++; if (col >= 2) { col = 0; row++; }
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        private void drawGradient(int x, int y, int w, int h, int c1, int c2) {
            float a1 = (float)(c1 >> 24 & 255) / 255.0F, r1 = (float)(c1 >> 16 & 255) / 255.0F, g1 = (float)(c1 >> 8 & 255) / 255.0F, b1 = (float)(c1 & 255) / 255.0F;
            float a2 = (float)(c2 >> 24 & 255) / 255.0F, r2 = (float)(c2 >> 16 & 255) / 255.0F, g2 = (float)(c2 >> 8 & 255) / 255.0F, b2 = (float)(c2 & 255) / 255.0F;
            GL11.glEnable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(r1, g1, b1, a1); GL11.glVertex2f(x + w, y); GL11.glVertex2f(x, y);
            GL11.glColor4f(r2, g2, b2, a2); GL11.glVertex2f(x, y + h); GL11.glVertex2f(x + w, y + h);
            GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND);
        }
        
        @Override
        public boolean doesGuiPauseGame() { return false; }
    }
    }
