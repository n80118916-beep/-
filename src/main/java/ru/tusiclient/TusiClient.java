package ru.tusiclient;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Mod(modid = "tusiclient", name = "TusiClient", version = "1.0")
public class TusiClient {
    
    public static KeyBinding openGuiKey;
    public static List<Module> modules = new ArrayList<>();
    public static List<String> friends = new ArrayList<>();
    public static Minecraft mc = Minecraft.getMinecraft();
    
    // Цвета темы (настраиваются в GUI)
    public static int THEME_MAIN = new Color(138, 43, 226, 255).getRGB(); // Фиолетовый
    public static int THEME_DARK = new Color(75, 0, 130, 255).getRGB();   // Темно-фиолетовый
    public static int THEME_BG = new Color(12, 12, 16, 255).getRGB();     // Плотный черный фон
    
    public static EntityLivingBase currentTarget = null;
    private static long lastAttack = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openGuiKey = new KeyBinding("Меню TusiClient", Keyboard.KEY_RSHIFT, "TusiClient");
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(openGuiKey);
        friends.add("Tusi");
        
        // Модули
        modules.add(new Module("Auto Explosion", "Ставит ТНТ", "Combat"));
        modules.add(new Module("Friend Protect", "Защита друзей", "Combat"));
        modules.add(new Module("Auto GApple", "Ест яблоко", "Combat"));
        modules.add(new Module("Attack Aura", "Авто-удар", "Combat"));
        modules.add(new Module("Keep Sprint", "Держит спринт", "Combat"));
        modules.add(new Module("Velocity", "Анти-отдача", "Combat"));
        modules.add(new Module("Target ESP", "Квадрат на цели", "Combat"));
        
        modules.add(new Module("Fullbright", "Яркость", "Render"));
        modules.add(new Module("ESP", "Подсветка", "Render"));
        modules.add(new Module("Tracers", "Линии", "Render"));
        modules.add(new Module("NoHurtCam", "Без тряски", "Render"));
        modules.add(new Module("Chams", "Сквозь стены", "Render"));

        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openGuiKey.isPressed()) mc.displayGuiScreen(new GuiTusiClient());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer) return;

        if (isEnabled("Attack Aura")) {
            currentTarget = findClosestTarget(4.5);
            if (currentTarget != null && !(isEnabled("Friend Protect") && currentTarget instanceof EntityPlayer && friends.contains(currentTarget.getCommandSenderName()))) {
                mc.thePlayer.rotationYaw = (float)(Math.atan2(currentTarget.posZ - mc.thePlayer.posZ, currentTarget.posX - mc.thePlayer.posX) * 180.0D / Math.PI) - 90.0F;
                mc.thePlayer.rotationPitch = (float)-(Math.atan2(currentTarget.posY + currentTarget.getEyeHeight() / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()), mc.thePlayer.getDistanceToEntity(currentTarget)) * 180.0D / Math.PI);
                if (System.currentTimeMillis() - lastAttack > 150) {
                    mc.playerController.attackEntity(mc.thePlayer, currentTarget);
                    mc.thePlayer.swingItem();
                    lastAttack = System.currentTimeMillis();
                }
            }
        } else currentTarget = null;
        
        if (isEnabled("Auto GApple") && mc.thePlayer.getHealth() <= 15.0F) {
            int slot = findItemInHotbar(Items.golden_apple);
            if (slot != -1) { mc.thePlayer.inventory.currentItem = slot; mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem()); }
        }

        if (isEnabled("Keep Sprint") && (mc.thePlayer.moveForward > 0 || mc.thePlayer.moveStrafing > 0) && !mc.thePlayer.isSneaking()) mc.thePlayer.setSprinting(true);
        if (isEnabled("Fullbright")) mc.gameSettings.gammaSetting = 100.0F; else if (mc.gameSettings.gammaSetting == 100.0F) mc.gameSettings.gammaSetting = 1.0F;
        if (isEnabled("NoHurtCam")) { mc.thePlayer.hurtTime = 0; mc.thePlayer.maxHurtTime = 0; }
        if (isEnabled("Velocity") && mc.thePlayer.hurtTime > 0) { mc.thePlayer.motionX *= 0.1; mc.thePlayer.motionZ *= 0.1; }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving == mc.thePlayer && isEnabled("Velocity")) { event.entityLiving.motionX = 0; event.entityLiving.motionZ = 0; }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            Gui.drawRect(8, 8, 78, 22, THEME_BG);
            Gui.drawRect(8, 8, 10, 22, THEME_MAIN);
            mc.fontRenderer.drawStringWithShadow("Tusi", 14, 11, -1);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!isEnabled("Target ESP") || currentTarget == null) return;
        
        GL11.glPushMatrix(); 
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glLineWidth(2.0F);
        
        float pTicks = event.partialTicks;
        double rX = RenderManager.instance.renderPosX, rY = RenderManager.instance.renderPosY, rZ = RenderManager.instance.renderPosZ;
        double x = currentTarget.lastTickPosX + (currentTarget.posX - currentTarget.lastTickPosX) * pTicks - rX;
        double y = currentTarget.lastTickPosY + (currentTarget.posY - currentTarget.lastTickPosY) * pTicks - rY;
        double z = currentTarget.lastTickPosZ + (currentTarget.posZ - currentTarget.lastTickPosZ) * pTicks - rZ;
        
        float r = (float)(THEME_MAIN >> 16 & 255) / 255.0F;
        float g = (float)(THEME_MAIN >> 8 & 255) / 255.0F;
        float b = (float)(THEME_MAIN & 255) / 255.0F;
        GL11.glColor4f(r, g, b, 1.0F); 
        
        GL11.glBegin(GL11.GL_LINE_LOOP); GL11.glVertex3d(x - 0.5, y, z - 0.5); GL11.glVertex3d(x + 0.5, y, z - 0.5); GL11.glVertex3d(x + 0.5, y, z + 0.5); GL11.glVertex3d(x - 0.5, y, z + 0.5); GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP); GL11.glVertex3d(x - 0.5, y + 2.0, z - 0.5); GL11.glVertex3d(x + 0.5, y + 2.0, z - 0.5); GL11.glVertex3d(x + 0.5, y + 2.0, z + 0.5); GL11.glVertex3d(x - 0.5, y + 2.0, z + 0.5); GL11.glEnd();
        
        GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND); GL11.glPopMatrix();
    }

    private EntityLivingBase findClosestTarget(double range) {
        EntityLivingBase target = null; double dist = range;
        for (Object obj : mc.theWorld.loadedEntityList) if (obj instanceof EntityLivingBase) {
            EntityLivingBase e = (EntityLivingBase) obj;
            if (e == mc.thePlayer || e.isDead || e.getHealth() <= 0) continue;
            if (e instanceof EntityPlayer && ((EntityPlayer) e).capabilities.isCreativeMode) continue;
            double d = mc.thePlayer.getDistanceToEntity(e); if (d < dist) { dist = d; target = e; }
        }
        return target;
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) { ItemStack s = mc.thePlayer.inventory.getStackInSlot(i); if (s != null && s.getItem() == item) return i; }
        return -1;
    }

    public static boolean isEnabled(String name) { for (Module m : modules) if (m.name.equals(name)) return m.enabled; return false; }
    public static List<Module> getModules(String category) { List<Module> list = new ArrayList<>(); for (Module m : modules) if (m.category.equals(category)) list.add(m); return list; }
    
    public static class Module {
        public String name, desc, category; public boolean enabled;
        public Module(String name, String desc, String category) { this.name = name; this.desc = desc; this.category = category; this.enabled = false; }
    }

    public static class GuiTusiClient extends GuiScreen {
        private int x, y, width = 340, height = 220, SIDEBAR = 100;
        private String category = "Combat";
        private int scrollOffset = 0;
        private final int SIDE_BG = 0xFF16161C, MOD_BG = 0xFF22222A, MOD_HOVER = 0xFF2F2F3A;

        @Override
        public void initGui() { 
            this.x = (this.width / 2) - (width / 2); 
            this.y = (this.height / 2) - (height / 2); 
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, this.width, this.height, 0x88000000); 
            
            // Тень и основа
            drawRoundedRect(x - 2, y - 2, width + 4, height + 4, 0x40000000);
            drawRoundedRect(x, y, width, height, THEME_BG);
            drawRoundedRect(x, y, SIDEBAR, height, SIDE_BG);
            
            // Логотип
            drawRoundedRect(x + 8, y + 8, 20, 20, THEME_MAIN);
            fontRendererObj.drawStringWithShadow("Tusi", x + 32, y + 13, 0xFFFFFFFF);
            
            // Категории
            drawCat("Combat", x + 8, y + 40, mouseX, mouseY);
            drawCat("Render", x + 8, y + 65, mouseX, mouseY);

            // Панель цветов (справа)
            int rightX = x + width - 22;
            drawRoundedRect(rightX, y + 35, 12, 12, 0xFF8A2BE2); 
            drawRoundedRect(rightX, y + 52, 12, 12, 0xFFFF0000); 
            drawRoundedRect(rightX, y + 69, 12, 12, 0xFF00FF00); 

            List<Module> list = getModules(category);
            int startX = x + SIDEBAR + 8, startY = y + 8, col = 0, row = 0;
            
            for (Module mod : list) {
                int mx = startX + (col * 112), my = startY + (row * 34) + scrollOffset;
                
                if (my > y + 5 && my < y + height - 25) {
                    boolean hover = mouseX >= mx && mouseX <= mx + 105 && mouseY >= my && mouseY <= my + 26;
                    
                    if (mod.enabled) drawRoundedRect(mx, my, 105, 26, THEME_DARK);
                    else drawRoundedRect(mx, my, 105, 26, hover ? MOD_HOVER : MOD_BG);
                    
                    fontRendererObj.drawStringWithShadow(mod.name, mx + 6, my + 9, 0xFFFFFFFF);
                    
                    int toggleX = mx + 85;
                    drawRoundedRect(toggleX, my + 9, 14, 8, mod.enabled ? THEME_MAIN : 0xFF3F3F46);
                    drawCircle(toggleX + (mod.enabled ? 9 : 3), my + 13, 2.5f, 0xFFFFFFFF);
                }
                col++; 
                if (col >= 2) { col = 0; row++; }
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        private void drawCat(String name, int cx, int cy, int mx, int my) {
            boolean active = category.equals(name);
            boolean hover = mx >= cx && mx <= cx + SIDEBAR - 15 && my >= cy && my <= cy + 18;
            
            if (active) drawRoundedRect(cx, cy, SIDEBAR - 15, 18, THEME_MAIN);
            else if (hover) drawRoundedRect(cx, cy, SIDEBAR - 15, 18, MOD_BG);
            
            fontRendererObj.drawString(name, cx + 6, cy + 5, active ? 0xFFFFFFFF : 0xFFA1A1AA);
        }

        @Override
        public void handleMouseInput() {
            super.handleMouseInput();
            int i = Mouse.getEventDWheel();
            if (i != 0) {
                if (i > 0) scrollOffset += 15; else scrollOffset -= 15;
                if (scrollOffset > 0) scrollOffset = 0;
                if (scrollOffset < -((getModules(category).size() / 2) * 34) + 130) {
                    scrollOffset = -((getModules(category).size() / 2) * 34) + 130;
                }
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (keyCode == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseY >= y + 40 && mouseY <= y + 58 && mouseX >= x + 8 && mouseX <= x + SIDEBAR) category = "Combat";
            if (mouseY >= y + 65 && mouseY <= y + 83 && mouseX >= x + 8 && mouseX <= x + SIDEBAR) { category = "Render"; scrollOffset = 0; }

            int rightX = x + width - 22;
            if (mouseX >= rightX && mouseX <= rightX + 12) {
                if (mouseY >= y + 35 && mouseY <= y + 47) { THEME_MAIN = 0xFF8A2BE2; THEME_DARK = 0xFF4B0082; }
                if (mouseY >= y + 52 && mouseY <= y + 64) { THEME_MAIN = 0xFFFF0000; THEME_DARK = 0xFF8B0000; }
                if (mouseY >= y + 69 && mouseY <= y + 81) { THEME_MAIN = 0xFF00FF00; THEME_DARK = 0xFF008000; }
            }
            
            List<Module> list = getModules(category);
            int startX = x + SIDEBAR + 8, startY = y + 8, col = 0, row = 0;
            
            for (Module mod : list) {
                int mx = startX + (col * 112), my = startY + (row * 34) + scrollOffset;
                
                if (my > y + 5 && my < y + height - 25) {
                    if (mouseX >= mx && mouseX <= mx + 105 && mouseY >= my && mouseY <= my + 26) { 
                        mod.enabled = !mod.enabled; 
                    }
                }
                col++; 
                if (col >= 2) { col = 0; row++; }
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        private void drawCircle(float x, float y, float radius, int color) {
            float a = (float)(color >> 24 & 255) / 255.0F, r = (float)(color >> 16 & 255) / 255.0F, g = (float)(color >> 8 & 255) / 255.0F, b = (float)(color & 255) / 255.0F;
            GL11.glEnable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glColor4f(r, g, b, a);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(x, y);
            for (int i = 0; i <= 360; i++) { double angle = Math.toRadians(i); GL11.glVertex2f((float)(x + Math.sin(angle) * radius), (float)(y + Math.cos(angle) * radius)); }
            GL11.glEnd(); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND);
        }

        private void drawRoundedRect(int x, int y, int w, int h, int color) {
            float a = (float)(color >> 24 & 255) / 255.0F, r = (float)(color >> 16 & 255) / 255.0F, g = (float)(color >> 8 & 255) / 255.0F, b = (float)(color & 255) / 255.0F;
            GL11.glEnable(GL11.GL_BLEND); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glColor4f(r, g, b, a);
            
            int radius = 3;
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(x + radius, y + radius);
            for(int i = 0; i <= 90; i += 10) { double angle = Math.toRadians(i); GL11.glVertex2f((float)(x + radius + Math.sin(angle) * radius), (float)(y + radius - Math.cos(angle) * radius)); }
            GL11.glEnd();
            
            drawRect(x + radius, y, x + w - radius, y + h, color);
            drawRect(x, y + radius, x + radius, y + h - radius, color);
            drawRect(x + w - radius, y + radius, x + w, y + h - radius, color);
            
            GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND);
        }
        
        @Override
        public boolean doesGuiPauseGame() { return false; }
    }
                            }
