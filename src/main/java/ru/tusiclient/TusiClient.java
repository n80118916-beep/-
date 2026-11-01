package ru.tusiclient;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
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
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.lwjgl.input.Keyboard;
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
    private static long lastAttack = 0, lastPvpMsg = 0, lastBowShoot = 0, lastPot = 0;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        openGuiKey = new KeyBinding("Меню TusiClient", Keyboard.KEY_RSHIFT, "TusiClient");
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(openGuiKey);
        
        friends.add("Tusi");

        modules.add(new Module("Auto Explosion", "Ставит и взрывает ТНТ", "Combat", false));
        modules.add(new Module("Friend Protect", "Не бьет друзей", "Combat", true));
        modules.add(new Module("Auto GApple", "Ест золотое яблоко", "Combat", false));
        modules.add(new Module("Attack Aura", "Автоматически бьет", "Combat", false));
        modules.add(new Module("Keep Sprint", "Не сбрасывает спринт", "Combat", true));
        modules.add(new Module("PVP Helper", "Пишет /pvp", "Combat", false));
        modules.add(new Module("Bow Spam", "Быстрая стрельба", "Combat", false));
        modules.add(new Module("Auto Pot", "Кидает зелья", "Combat", false));
        modules.add(new Module("Hitboxes", "Увеличивает хитбоксы", "Combat", false));
        modules.add(new Module("Velocity", "Убирает откидывание", "Combat", false));
        
        modules.add(new Module("Fullbright", "Яркость", "Render", true));
        modules.add(new Module("ESP", "Подсветка игроков", "Render", false));
        modules.add(new Module("Tracers", "Линии до игроков", "Render", false));
        modules.add(new Module("NoHurtCam", "Убирает тряску камеры", "Render", true));
        modules.add(new Module("Storage ESP", "Подсветка сундуков", "Render", false));
        modules.add(new Module("Item ESP", "Подсветка предметов", "Render", false));
        modules.add(new Module("Nametags", "Красивые ники", "Render", false));
        modules.add(new Module("Block ESP", "Подсветка руд", "Render", false));
        modules.add(new Module("Chams", "Игроки сквозь стены", "Render", false));
        modules.add(new Module("Freecam", "Свободная камера", "Render", false));

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

        if (isEnabled("Attack Aura")) {
            EntityLivingBase target = findClosestTarget(4.5);
            if (target != null && !(isEnabled("Friend Protect") && target instanceof EntityPlayer && friends.contains(target.getCommandSenderName()))) {
                float[] rot = getRotations(target);
                mc.thePlayer.rotationYaw = rot[0];
                mc.thePlayer.rotationPitch = rot[1];
                if (System.currentTimeMillis() - lastAttack > 450) {
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    mc.thePlayer.swingItem();
                    lastAttack = System.currentTimeMillis();
                }
            }
        }
        if (isEnabled("Auto GApple") && mc.thePlayer.getHealth() <= 15.0F) {
            int slot = findItemInHotbar(Items.golden_apple);
            if (slot != -1) {
                mc.thePlayer.inventory.currentItem = slot;
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem());
            }
        }
        if (isEnabled("Auto Pot") && mc.thePlayer.getHealth() <= 15.0F && System.currentTimeMillis() - lastPot > 1000) {
            int slot = findSplashHealthPotion();
            if (slot != -1) {
                mc.thePlayer.inventory.currentItem = slot;
                mc.thePlayer.rotationPitch = 90.0F;
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem());
                lastPot = System.currentTimeMillis();
            }
        }
        if (isEnabled("Keep Sprint") && (mc.thePlayer.moveForward > 0 || mc.thePlayer.moveStrafing > 0) && !mc.thePlayer.isSneaking()) {
            mc.thePlayer.setSprinting(true);
        }
        if (isEnabled("PVP Helper") && System.currentTimeMillis() - lastPvpMsg > 5000) {
            mc.thePlayer.sendChatMessage("/pvp");
            lastPvpMsg = System.currentTimeMillis();
        }
        if (isEnabled("Bow Spam") && mc.gameSettings.keyBindUseItem.getIsKeyPressed()) {
            ItemStack held = mc.thePlayer.getCurrentEquippedItem();
            if (held != null && held.getItem() == Items.bow && System.currentTimeMillis() - lastBowShoot > 150) {
                mc.thePlayer.stopUsingItem();
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(5, 0, 0, 0, 0));
                lastBowShoot = System.currentTimeMillis();
            }
        }
        if (isEnabled("Auto Explosion")) {
            EntityLivingBase target = findClosestTarget(4.0);
            if (target != null) {
                int tntSlot = findItemInHotbar(Item.getItemFromBlock(Blocks.tnt));
                if (tntSlot != -1) {
                    mc.thePlayer.inventory.currentItem = tntSlot;
                    int x = MathHelper.floor_double(target.posX), y = MathHelper.floor_double(target.posY), z = MathHelper.floor_double(target.posZ);
                    if (mc.theWorld.getBlock(x, y, z) == Blocks.air) {
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, 1, Vec3.createVectorHelper(x, y, z));
                    }
                    int flintSlot = findItemInHotbar(Items.flint_and_steel);
                    if (flintSlot != -1) {
                        mc.thePlayer.inventory.currentItem = flintSlot;
                        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), x, y, z, 1, Vec3.createVectorHelper(x, y, z));
                    }
                }
            }
        }
        if (isEnabled("Fullbright")) mc.gameSettings.gammaSetting = 100.0F;
        else if (mc.gameSettings.gammaSetting == 100.0F) mc.gameSettings.gammaSetting = 1.0F;
        
        if (isEnabled("NoHurtCam")) { mc.thePlayer.hurtTime = 0; mc.thePlayer.maxHurtTime = 0; }
        if (isEnabled("Velocity") && mc.thePlayer.hurtTime > 0) { mc.thePlayer.motionX *= 0.1; mc.thePlayer.motionZ *= 0.1; }
        
        if (isEnabled("Freecam")) {
            mc.thePlayer.noClip = true; mc.thePlayer.capabilities.isFlying = true; mc.thePlayer.motionY = 0;
            if (mc.gameSettings.keyBindJump.getIsKeyPressed()) mc.thePlayer.motionY = 0.5;
            if (mc.gameSettings.keyBindSneak.getIsKeyPressed()) mc.thePlayer.motionY = -0.5;
        } else {
            mc.thePlayer.capabilities.isFlying = false; mc.thePlayer.noClip = false;
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.entityLiving == mc.thePlayer && isEnabled("Velocity")) {
            event.entityLiving.motionX = 0; event.entityLiving.motionZ = 0;
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (isEnabled("Chams") && event.entityPlayer != mc.thePlayer) GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (isEnabled("Chams") && event.entityPlayer != mc.thePlayer) GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        boolean esp = isEnabled("ESP"), tracers = isEnabled("Tracers"), storage = isEnabled("Storage ESP"), items = isEnabled("Item ESP"), blockEsp = isEnabled("Block ESP"), nametags = isEnabled("Nametags"), hitboxes = isEnabled("Hitboxes");
        if (!esp && !tracers && !storage && !items && !blockEsp && !nametags && !hitboxes) return;
        
        GL11.glPushMatrix(); GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GL11.glLineWidth(2.0F);
        float pTicks = event.partialTicks;
        double rX = RenderManager.instance.renderPosX, rY = RenderManager.instance.renderPosY, rZ = RenderManager.instance.renderPosZ;
        
        for (Object obj : mc.theWorld.playerEntities) {
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) continue;
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * pTicks - rX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * pTicks - rY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pTicks - rZ;
            
            if (tracers) { GL11.glColor4f(1F, 0F, 0F, 1F); GL11.glBegin(GL11.GL_LINES); GL11.glVertex3d(0, -0.1, 0); GL11.glVertex3d(x, y + player.getEyeHeight() / 2, z); GL11.glEnd(); }
            if (esp) { GL11.glColor4f(0F, 1F, 0F, 1F); drawBox(x, y, z, player.width / 2, player.height); }
            if (hitboxes) { GL11.glColor4f(1F, 0F, 0F, 1F); drawBox(x, y, z, (player.width / 2) + 0.2, player.height + 0.2); }
            if (nametags) {
                GL11.glPushMatrix(); GL11.glTranslated(x, y + player.height + 0.5, z);
                GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F); GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
                GL11.glScalef(-0.02666667F, -0.02666667F, 0.02666667F); GL11.glDisable(GL11.GL_LIGHTING); GL11.glDepthMask(false);
                String name = player.getDisplayName(); int width = mc.fontRenderer.getStringWidth(name) / 2;
                drawRect(-width - 1, -1, width + 1, 8, 0x80000000); mc.fontRenderer.drawStringWithShadow(name, -width, 0, 0xFFFFFF);
                GL11.glDepthMask(true); GL11.glEnable(GL11.GL_LIGHTING); GL11.glPopMatrix();
            }
        }
        if (storage) {
            GL11.glColor4f(1F, 1F, 0F, 1F);
            for (Object obj : mc.theWorld.loadedTileEntityList) if (obj instanceof TileEntityChest) { TileEntityChest c = (TileEntityChest) obj; drawBox(c.xCoord + 0.5 - rX, c.yCoord + 0.5 - rY, c.zCoord + 0.5 - rZ, 0.5, 1.0); }
        }
        if (items) {
            GL11.glColor4f(1F, 0.5F, 0F, 1F);
            for (Object obj : mc.theWorld.loadedEntityList) if (obj instanceof EntityItem) { EntityItem i = (EntityItem) obj; drawBox(i.lastTickPosX + (i.posX - i.lastTickPosX) * pTicks - rX, i.lastTickPosY + (i.posY - i.lastTickPosY) * pTicks - rY, i.lastTickPosZ + (i.posZ - i.lastTickPosZ) * pTicks - rZ, 0.2, 0.4); }
        }
        if (blockEsp) {
            int radius = 20, px = (int) mc.thePlayer.posX, py = (int) mc.thePlayer.posY, pz = (int) mc.thePlayer.posZ; GL11.glColor4f(0F, 1F, 1F, 1F);
            for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
                Block b = mc.theWorld.getBlock(px + x, py + y, pz + z);
                if (b == Blocks.diamond_ore || b == Blocks.gold_ore || b == Blocks.iron_ore || b == Blocks.emerald_ore) drawBox((px + x) - rX, (py + y) - rY, (pz + z) - rZ, 0.5, 1.0);
            }
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND); GL11.glPopMatrix();
    }

    private void drawBox(double x, double y, double z, double w, double h) {
        GL11.glBegin(GL11.GL_LINE_LOOP); GL11.glVertex3d(x - w, y, z - w); GL11.glVertex3d(x + w, y, z - w); GL11.glVertex3d(x + w, y, z + w); GL11.glVertex3d(x - w, y, z + w); GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP); GL11.glVertex3d(x - w, y + h, z - w); GL11.glVertex3d(x + w, y + h, z - w); GL11.glVertex3d(x + w, y + h, z + w); GL11.glVertex3d(x - w, y + h, z + w); GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES); GL11.glVertex3d(x - w, y, z - w); GL11.glVertex3d(x - w, y + h, z - w); GL11.glVertex3d(x + w, y, z - w); GL11.glVertex3d(x + w, y + h, z - w); GL11.glVertex3d(x + w, y, z + w); GL11.glVertex3d(x + w, y + h, z + w); GL11.glVertex3d(x - w, y, z + w); GL11.glVertex3d(x - w, y + h, z + w); GL11.glEnd();
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

    private float[] getRotations(Entity entity) {
        double diffX = entity.posX - mc.thePlayer.posX, diffY = entity.posY + entity.getEyeHeight() / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()), diffZ = entity.posZ - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        return new float[]{(float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F, (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI)};
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) { ItemStack s = mc.thePlayer.inventory.getStackInSlot(i); if (s != null && s.getItem() == item) return i; }
        return -1;
    }

    private int findSplashHealthPotion() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && s.getItem() == Items.potionitem) {
                List<PotionEffect> effects = Items.potionitem.getEffects(s);
                if (effects != null) for (PotionEffect effect : effects) if (effect.getPotionID() == Potion.heal.id) return i;
            }
        }
        return -1;
    }

    public static boolean isEnabled(String name) { for (Module m : modules) if (m.name.equals(name)) return m.enabled; return false; }
    public static List<Module> getModules(String category) { List<Module> list = new ArrayList<>(); for (Module m : modules) if (m.category.equals(category)) list.add(m); return list; }
    
    public static class Module {
        public String name, desc, category; public boolean enabled;
        public Module(String name, String desc, String category, boolean enabled) { this.name = name; this.desc = desc; this.category = category; this.enabled = enabled; }
    }

    public static class GuiTusiClient extends GuiScreen {
        private int x, y, width = 520, height = 360;
        private final int SIDEBAR = 130;
        private String category = "Combat"; 
        private final int BG = new Color(18, 18, 20, 255).getRGB(), SIDE = new Color(24, 24, 27, 255).getRGB(), MOD = new Color(39, 39, 42, 255).getRGB(), TEXT = new Color(250, 250, 250, 255).getRGB(), DESC = new Color(161, 161, 170, 255).getRGB();

        @Override
        public void initGui() { this.x = (this.width / 2) - (this.width / 2); this.y = (this.height / 2) - (this.height / 2); }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 180).getRGB());
            drawRect(x, y, x + width, y + height, BG); drawRect(x, y, x + SIDEBAR, y + height, SIDE);
            drawRect(x + 15, y + 15, x + 45, y + 45, new Color(180, 40, 40).getRGB());
            fontRendererObj.drawStringWithShadow("TusiClient", x + 55, y + 25, TEXT);
            drawCat("Combat", x + 15, y + 70, mouseX, mouseY); drawCat("Movement", x + 15, y + 100, mouseX, mouseY); drawCat("Render", x + 15, y + 130, mouseX, mouseY);
            drawCat("Player", x + 15, y + 160, mouseX, mouseY); drawCat("Util", x + 15, y + 190, mouseX, mouseY);
            fontRendererObj.drawString("Другие", x + 15, y + 220, DESC);
            drawCat("Configs", x + 15, y + 240, mouseX, mouseY); drawCat("Themes", x + 15, y + 270, mouseX, mouseY);
            drawRect(x + SIDEBAR + 20, y + 20, x + width - 20, y + 40, SIDE);
            fontRendererObj.drawString("Поиск...", x + SIDEBAR + 30, y + 26, DESC);
            List<Module> list = getModules(category);
            int startX = x + SIDEBAR + 20, startY = y + 60, col = 0, row = 0;
            for (Module mod : list) {
                int mx = startX + (col * 190), my = startY + (row * 45);
                if (mod.enabled) drawGradient(mx, my, 180, 38, new Color(59, 130, 246).getRGB(), new Color(139, 92, 246).getRGB());
                else drawRect(mx, my, mx + 180, my + 38, MOD);
                fontRendererObj.drawStringWithShadow(mod.name, mx + 8, my + 6, TEXT);
                GL11.glPushMatrix(); GL11.glScalef(0.75f, 0.75f, 1f); fontRendererObj.drawString(mod.desc, (int)((mx + 8) / 0.75f), (int)((my + 20) / 0.75f), DESC); GL11.glPopMatrix();
                drawRect(mx + 160, my + 8, mx + 170, my + 18, mod.enabled ? new Color(255,255,255,200).getRGB() : SIDE);
                col++; if (col >= 2) { col = 0; row++; }
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        private void drawCat(String name, int cx, int cy, int mx, int my) {
            boolean active = category.equals(name), hover = mx >= cx && mx <= cx + SIDEBAR - 20 && my >= cy && my <= cy + 20;
            if (active) drawGradient(cx, cy, SIDEBAR - 20, 22, new Color(59, 130, 246).getRGB(), new Color(139, 92, 246).getRGB());
            else if (hover) drawRect(cx, cy, cx + SIDEBAR - 20, cy + 22, MOD);
            fontRendererObj.drawString(name, cx + 10, cy + 7, active ? -1 : TEXT);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouse
