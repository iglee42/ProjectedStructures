package fr.iglee42.projectedstructures.items;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.iglee42.igleelib.api.utils.InventoryUtil;
import fr.iglee42.projectedstructures.ProjectedStructures;
import fr.iglee42.projectedstructures.ModContent;
import fr.iglee42.projectedstructures.client.ProjectorScreen;
import fr.iglee42.projectedstructures.network.ModMessages;
import fr.iglee42.projectedstructures.network.packets.OpenProjectorGUIS2CPacket;
import fr.iglee42.projectedstructures.network.packets.ProjectorClearStructureC2SPacket;
import fr.iglee42.projectedstructures.network.packets.ProjectorRotateC2SPacket;
import fr.iglee42.projectedstructures.network.packets.ProjectorSwitchKonamiC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.*;

import static fr.iglee42.projectedstructures.client.ClientEvents.ForgeBus.renderStructure;

public class ProjectorItem extends Item {
    public ProjectorItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level p_41422_, List<Component> components, TooltipFlag p_41424_) {
        if (!hasStructure(stack)) components.add(append("Right Click","Select the Structure"));
        else {
            String name = getStructure(stack);
            String firstChar = String.valueOf(name.charAt(0));
            components.add(append("Selected Structure",firstChar.toUpperCase() + name.substring(1).replaceAll("\\.s*nbt","")));
            if (hasAnchor(stack)){
                BlockPos anchor = getAnchor(stack);
                components.add(append("Anchored at",anchor.getX()+ " "+anchor.getY()+" " + anchor.getZ()));
            }
            components.add(Component.empty());
            components.add(append("Left Click","Clear the selected Structure"));
            components.add(append("Right Click","Open the GUI"));
            if (!hasAnchor(stack))components.add(append("Right Click on a Block","Anchor the Structure "));
            else {
                components.add(append("Sneak + Right Click","Remove the anchor"));
            }
            components.add(append("Sneak + Scroll","Rotate the Structure"));
           // components.add(append("When place + Shift & Right Click","Replace ghost blocks"));
        }
    }

    private Component append(String base,String adding){
        return Component.literal(base).withStyle(ChatFormatting.GOLD).append(Component.literal(" : ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.literal(adding).withStyle(ChatFormatting.YELLOW));
    }

    public static boolean hasStructure(ItemStack stack){
        return stack.getOrCreateTag().contains("structureName");
    }
    public static String getStructure(ItemStack stack){
        return stack.getOrCreateTag().getString("structureName");
    }
    public static String getStructurePath(ItemStack stack){
        return stack.getOrCreateTag().getString("structurePath");
    }
    public static boolean hasAnchor(ItemStack stack){
        return stack.getOrCreateTag().contains("anchor");
    }
    public static BlockPos getAnchor(ItemStack stack){
        return NbtUtils.readBlockPos(stack.getOrCreateTag().getCompound("anchor"));
    }
    public static boolean hasRotation(ItemStack stack){
        return stack.getOrCreateTag().contains("rotation");
    }
    public static Rotation getRotation(ItemStack stack){
        return Rotation.valueOf(stack.getOrCreateTag().getString("rotation"));
    }
    public static boolean getTransparency(ItemStack stack){
        return !stack.getOrCreateTag().contains("transparency") || stack.getOrCreateTag().getBoolean("transparency");
    }

    public static boolean getKonami(ItemStack stack){
        return stack.getOrCreateTag().contains("konami") && stack.getOrCreateTag().getBoolean("konami");
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        if (!player.isCrouching()) {
            if (!hasAnchor(stack) && Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK) {
                HitResult result = Minecraft.getInstance().hitResult;
                Vec3 loc = level.getBlockState(BlockPos.containing(result.getLocation())).isAir() ? result.getLocation().subtract(0, 1, 0) : result.getLocation();
                BlockPos pos = new BlockPos((int) loc.x, (int) (loc.y + 1), (int) loc.z);
                stack.getOrCreateTag().put("anchor",NbtUtils.writeBlockPos(pos));
            } else {
                ServerPlayer sp = (ServerPlayer) player;
                ModMessages.sendToPlayer(new OpenProjectorGUIS2CPacket(getTransparency(stack),getKonami(stack)),sp);
            }
        } else {
            if (hasAnchor(stack)){
                stack.getOrCreateTag().remove("anchor");
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (selected && stack.getOrCreateTag().contains("structureName") && entity instanceof Player player) {
            /*if (Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK) {
                HitResult result = Minecraft.getInstance().hitResult;
                Vec3 loc = level.getBlockState(new BlockPos(result.getLocation())).isAir() ? result.getLocation().subtract(0, 1, 0) : result.getLocation();
                StructureTemplateManager structureManager = player.getServer().getStructureManager();
                StructureTemplate s = null;
                try {
                    InputStream stream = new FileInputStream(ConfigStructures.getStructure(stack.getOrCreateTag().getString("structurePath")));
                    s = structureManager.readStructure(NbtIo.readCompressed(stream));
                    stream.close();
                } catch (Exception ignored) {}
                CompoundTag pST = stack.getOrCreateTag().getCompound("structurePlacementSettings");
                StructurePlaceSettings settings = new StructurePlaceSettings()
                        .setRotation(pST != null && pST.contains("rotation") ? Rotation.valueOf(pST.getString("rotation")) : Rotation.NONE);
                BlockPos pos = new BlockPos(loc.x , loc.y + 1,loc.z);
                if (level.getBlockState(pos.offset(0,-1,0)).is(ModContent.GHOST_BLOCK.get())) pos = pos.offset(0,-1,0);
                if (s != null) {
                    StructureTemplate.processBlockInfos(level,pos,pos,settings,settings.getRandomPalette(s.palettes, pos).blocks(),s).forEach(sbi -> {
                        if (!sbi.state.isAir()) {
                            if (level.getBlockState(sbi.pos).is(Blocks.AIR)) {
                                /*level.setBlockAndUpdate(sbi.pos, ModContent.GHOST_BLOCK.get().defaultBlockState());
                                level.getBlockEntity(sbi.pos, ModContent.GHOST_BLOCK_ENTITY.get()).ifPresent(g -> {
                                    ((GhostBlockEntity) g).setStockedBlock(sbi.state);
                                    ((GhostBlockEntity) g).setDispearTime(50);
                                });
                            }
                        }
                    });
                }
            }*/
        }
    }


    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = ProjectedStructures.MODID,value = Dist.CLIENT)
    public static class ClientRenderHandler {

        @SubscribeEvent
        public static void renderLevelStage(RenderLevelStageEvent event) throws IOException, CommandSyntaxException {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
                LocalPlayer p = Minecraft.getInstance().player;
                ClientLevel level = p.clientLevel;
                if (InventoryUtil.hasPlayerStackInInventory(p, ModContent.PROJECTOR.get())) {
                    int slot = InventoryUtil.getFirstInventoryIndex(p, ModContent.PROJECTOR.get());
                    ItemStack stack = p.getInventory().getItem(slot);
                    if (ProjectorItem.hasStructure(stack)) {
                        String structurePath = ProjectorItem.getStructurePath(stack);
                        Vec3 renderView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                        event.getPoseStack().pushPose();
                        event.getPoseStack().translate(-renderView.x, -renderView.y, -renderView.z);
                        Rotation rotation = hasRotation(stack) ? getRotation( stack) : Rotation.NONE;
                        if (!hasAnchor(stack) && Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK){
                            HitResult result = Minecraft.getInstance().hitResult;
                            Vec3 loc = level.getBlockState(BlockPos.containing(result.getLocation())).isAir() ? result.getLocation().subtract(0, 1, 0) : result.getLocation();
                            BlockPos pos = new BlockPos((int) loc.x, (int) (loc.y + 1), (int) loc.z);
                            event.getPoseStack().translate(pos.getX(),pos.getY(),pos.getZ());
                            renderStructure(structurePath,event.getPoseStack(),rotation,pos);
                        } else if (hasAnchor(stack)){
                            BlockPos anchor = getAnchor(stack);
                            event.getPoseStack().translate(anchor.getX(),anchor.getY(), anchor.getZ());
                            renderStructure(structurePath,event.getPoseStack(),rotation,anchor);
                        }

                        event.getPoseStack().popPose();
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = ProjectedStructures.MODID, value = Dist.CLIENT)
    public static class ClientInputHandler {
        @SubscribeEvent
        public static void handleScroll(InputEvent.MouseScrollingEvent event) {
            double delta = event.getScrollDelta();
            if (Minecraft.getInstance().player.getMainHandItem().is(ModContent.PROJECTOR.get()) && Minecraft.getInstance().player.isCrouching()){
                if (delta > 0){
                    ModMessages.sendToServer(new ProjectorRotateC2SPacket(true,Minecraft.getInstance().player.getUUID()));
                } else if (delta < 0){
                    ModMessages.sendToServer(new ProjectorRotateC2SPacket(false,Minecraft.getInstance().player.getUUID()));
                }
                event.setCanceled(true);
            }
        }
        @SubscribeEvent
        public static void mouseClick(InputEvent.InteractionKeyMappingTriggered event){
            if (!event.isAttack()) return;
            if (Minecraft.getInstance().player.getMainHandItem().is(ModContent.PROJECTOR.get()) && Minecraft.getInstance().player.getMainHandItem().getOrCreateTag().contains("structureName"))
                ModMessages.sendToServer(new ProjectorClearStructureC2SPacket(Minecraft.getInstance().player.getUUID()));
        }
        private static LinkedList<Integer> lastKeyPressed = new LinkedList<>();
        @SubscribeEvent
        public static void keyPress(InputEvent.Key event){
            if (event.getModifiers() != 0 || event.getAction() != GLFW.GLFW_PRESS) return;
            lastKeyPressed.add(event.getKey());
            if (lastKeyPressed.size() > 10) lastKeyPressed.poll();
            if (checkKonamiCode()){
                if (Minecraft.getInstance().screen instanceof ProjectorScreen screen){
                    screen.konami = !screen.konami;
                    screen.konamiTime = 240;
                    ModMessages.sendToServer(new ProjectorSwitchKonamiC2SPacket(Minecraft.getInstance().player.getUUID(),screen.konami));
                }
            }
        }

        private static boolean checkKonamiCode(){
            //AZERTY KEYBOARD
            return lastKeyPressed.size() == 10 && lastKeyPressed.get(0).equals(GLFW.GLFW_KEY_UP) &&
                    lastKeyPressed.get(1).equals(GLFW.GLFW_KEY_UP) &&
                    lastKeyPressed.get(2).equals(GLFW.GLFW_KEY_DOWN) &&
                    lastKeyPressed.get(3).equals(GLFW.GLFW_KEY_DOWN) &&
                    lastKeyPressed.get(4).equals(GLFW.GLFW_KEY_LEFT) &&
                    lastKeyPressed.get(5).equals(GLFW.GLFW_KEY_RIGHT) &&
                    lastKeyPressed.get(6).equals(GLFW.GLFW_KEY_LEFT) &&
                    lastKeyPressed.get(7).equals(GLFW.GLFW_KEY_RIGHT) &&
                    lastKeyPressed.get(8).equals(GLFW.GLFW_KEY_B) &&
                    lastKeyPressed.get(9).equals(GLFW.GLFW_KEY_Q);
        }
    }
}