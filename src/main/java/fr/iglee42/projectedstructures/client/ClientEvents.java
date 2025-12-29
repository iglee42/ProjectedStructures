package fr.iglee42.projectedstructures.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Axis;
import fr.iglee42.igleelib.api.utils.InventoryUtil;
import fr.iglee42.projectedstructures.ModContent;
import fr.iglee42.projectedstructures.ProjectedStructures;
import fr.iglee42.projectedstructures.items.ProjectorItem;
import fr.iglee42.projectedstructures.network.ModMessages;
import fr.iglee42.projectedstructures.network.packets.ProjectorChangeLayerC2SPacket;
import fr.iglee42.projectedstructures.utils.Utils;
import fr.iglee42.projectedstructures.mixins.AccessorMultiBufferSource;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public class ClientEvents {

    private static final Logger log = LoggerFactory.getLogger(ClientEvents.class);

    @Mod.EventBusSubscriber(modid = ProjectedStructures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public class ModBus {
        public static final KeyMapping nextLayer = new KeyMapping("key.projectedstructures.nextLayer", GLFW.GLFW_KEY_UP,"key.projectedstructures.category");
        public static final KeyMapping previousLayer = new KeyMapping("key.projectedstructures.previousLayer", GLFW.GLFW_KEY_DOWN,"key.projectedstructures.category");
        @SubscribeEvent
        public static void clientStuff(final FMLClientSetupEvent event) {
        }
        @SubscribeEvent
        public static void keyMapping(final RegisterKeyMappingsEvent event) {
            event.register(nextLayer);
            event.register(previousLayer);
        }
    }

    @Mod.EventBusSubscriber(modid = ProjectedStructures.MODID,value = Dist.CLIENT)
    public class ForgeBus {

        private static int ticks = 0;

        public static int ticks() {
            return ticks;
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                ticks += 1;
                if (Minecraft.getInstance().screen instanceof ProjectorScreen screen && screen.konami && screen.konamiTime > 0){
                    screen.konamiTime--;
                }
            }
        }

        @SubscribeEvent
        public static void keyPress(InputEvent.Key event) throws IOException, CommandSyntaxException {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().is(ModContent.PROJECTOR.get())){
                int layer = ProjectorItem.getLayer(Minecraft.getInstance().player.getMainHandItem());
                if (ModBus.previousLayer.isDown()){
                    if (layer > -1){
                        ModMessages.sendToServer(new ProjectorChangeLayerC2SPacket(Minecraft.getInstance().player.getUUID(),layer-1));
                        Minecraft.getInstance().player.displayClientMessage(Component.literal("Layer : " + (layer - 1 == -1 ? "All" : layer - 1)),true);
                    }
                }
                if (ModBus.nextLayer.isDown()){
                    StructureTemplate template = Utils.getStructureTemplate(ProjectorItem.getStructurePath(Minecraft.getInstance().player.getMainHandItem()));
                    if (template != null){
                        if (layer < template.getSize().getY() - 1){
                            ModMessages.sendToServer(new ProjectorChangeLayerC2SPacket(Minecraft.getInstance().player.getUUID(),layer+1));
                            Minecraft.getInstance().player.displayClientMessage(Component.literal("Layer : " + (layer + 1)),true);
                        }
                    }
                }
            }
        }
        public static void renderStructure(String structurePath, PoseStack matrix, Rotation rotation,BlockPos basePos) throws IOException, CommandSyntaxException {
            StructureTemplate template = Utils.getStructureTemplate(structurePath);
            if (template != null){
                MultiBufferSource.BufferSource buffers = initBuffers(Minecraft.getInstance().renderBuffers().bufferSource());
                matrix.pushPose();
                for (StructureTemplate.Palette palette : template.palettes) {
                    for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
                        BlockPos pos = blockInfo.pos();
                        if (getLayer() > -1){
                            if (pos.getY() != getLayer()) continue;
                        }
                        BlockState bs = blockInfo.state();
                        matrix.pushPose();

                        pos = pos.rotate(rotation);

                        if (!Minecraft.getInstance().level.getBlockState(basePos.offset(pos)).equals(bs)){
                            matrix.pushPose();
                            matrix.translate(-0.001,-0.001,-0.001);
                            matrix.scale(1.002f,1.002f,1.002f);
                            renderBlock(bs, pos, matrix, buffers,true,basePos);
                            matrix.popPose();

                            if (!Minecraft.getInstance().level.getBlockState(basePos.offset(pos)).isAir()){
                                matrix.pushPose();
                                matrix.translate(pos.getX(), pos.getY(), pos.getZ());
                                VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
                                Matrix4f mat = matrix.last().pose();
                                Matrix3f normalMat = matrix.last().normal();
                                // draw red outline around the block (unit cube from 0..1)
                                float r = 1f, g = 0f, b = 0f, a = 1f;

                                if (Minecraft.getInstance().level.getBlockState(basePos.offset(pos)).getBlock() == bs.getBlock()){
                                    g = 1f;
                                }
                                // bottom square
                                consumer.vertex(mat, -0.001f, -0.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, 1.001f, -0.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, 1.001f, -0.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, 1.001f, -0.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, 1.001f, -0.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, -0.001f, -0.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, -0.001f, -0.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, -0.001f, -0.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                // top square
                                consumer.vertex(mat, -0.001f, 1.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, 1.001f, 1.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, 1.001f, 1.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, 1.001f, 1.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, 1.001f, 1.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, -0.001f, 1.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, -0.001f, 1.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, -0.001f, 1.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                // vertical edges
                                consumer.vertex(mat, -0.001f, -0.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, -0.001f, 1.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, 1.001f, -0.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, 1.001f, 1.001f, -0.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, 1.001f, -0.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, 1.001f, 1.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                consumer.vertex(mat, -0.001f, -0.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();
                                consumer.vertex(mat, -0.001f, 1.001f, 1.001f).color(r, g, b, a).normal(normalMat,0,1,0).endVertex();

                                matrix.popPose();
                            }
                        }

                        matrix.popPose();

                    }
                }
                buffers.endBatch();
                matrix.popPose();
            }
        }
        public static void renderStructure(String structurePath, PoseStack matrix,int x,int y,int maxSize) throws IOException, CommandSyntaxException {
            StructureTemplate template = Utils.getStructureTemplate(structurePath);
            if (template != null){
                MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
                matrix.pushPose();
                for (StructureTemplate.Palette palette : template.palettes) {
                    for (StructureTemplate.StructureBlockInfo blockInfo : palette.blocks()) {
                        BlockPos pos = blockInfo.pos();
                        BlockState bs = blockInfo.state();
                        matrix.pushPose();
                        Vec3i size = template.getSize();
                        int sizeX = size.getX();
                        int sizeY = size.getY();
                        int sizeZ = size.getZ();
                        float diagonal = (float) Math.sqrt(sizeX * sizeX + sizeZ * sizeZ);
                        float scaleX = maxSize / diagonal;
                        float scaleY = maxSize / sizeY;
                        float scale = -Math.min(scaleX, scaleY);

                        float offX = (float) -sizeX;
                        float offZ = (float) -sizeZ;

                        matrix.translate(x,y,100);
                        matrix.scale(scale, scale, scale);
                        matrix.translate(-(float) sizeX / 2, -(float) sizeY / 2, 0);

                        // Initial eye pos somewhere off in the distance in the -Z direction
                        Vector4f eye = new Vector4f(0, 0, -100, 1);
                        Matrix4f rotMat = new Matrix4f();
                        rotMat.identity();

                        // For each GL rotation done, track the opposite to keep the eye pos accurate
                        matrix.mulPose(Axis.XP.rotationDegrees(-30F));
                        rotMat.rotation(Axis.XP.rotationDegrees(30));




                       //float time = ticks();
                       matrix.translate(-offX, 0, -offZ);
                       //matrix.mulPose(Axis.YP.rotationDegrees(time));
                       //rotMat.rotation(Axis.YP.rotationDegrees(-time));
                        matrix.mulPose(Axis.YP.rotationDegrees(45));
                        rotMat.rotation(Axis.YP.rotationDegrees(-45));
                        matrix.translate(offX, 0, offZ);

                        // Finally apply the rotations
                        eye.mul(rotMat);
                        renderBlock(bs, pos, matrix, buffers,true,Minecraft.getInstance().player.blockPosition());
                        matrix.popPose();
                        /*BlockEntity te = null;
                        if (bs.getBlock() instanceof EntityBlock) {
                            te = ((EntityBlock) bs.getBlock()).newBlockEntity(pos, bs);
                        }

                        if (te != null) {
                            te.setLevel(FakeLevel.getInstance());

                            // fake cached state in case the renderer checks it as we don't want to query the actual world
                            //noinspection deprecation
                            te.setBlockState(bs);

                            matrix.pushPose();
                            try {
                                BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(te);
                                if (renderer != null) {
                                    renderer.render(te, 0, matrix, buffers, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
                                }
                            } catch (Exception ignored) {
                            } finally {
                                matrix.popPose();
                            }
                        }*/

                    }
                }
                buffers.endBatch();
                matrix.popPose();
            }
        }

        private static void renderBlock(BlockState state, BlockPos pos, PoseStack matrix, MultiBufferSource.BufferSource buffers,boolean shouldTranslate,BlockPos basePos) {
            if (state.liquid()){
                matrix.pushPose();
                if (shouldTranslate)matrix.translate(pos.getX(), pos.getY(), pos.getZ());
                matrix.translate(0,2/16f,0);
                Lighting.setupForFlatItems();
                if (!state.getFluidState().isEmpty() && Minecraft.getInstance().level != null){
                    Minecraft.getInstance().getBlockRenderer().renderLiquid(pos,Minecraft.getInstance().level,new LiquidBlockVertexConsumer(buffers.getBuffer(ItemBlockRenderTypes.getRenderLayer(state.getFluidState())),matrix,pos),state,state.getFluidState());
                }
                matrix.popPose();
                return;
            }
            if (state.getRenderShape() == RenderShape.MODEL) {
                if (shouldTranslate)matrix.translate(pos.getX(), pos.getY(), pos.getZ());
                BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
                BakedModel model = blockRenderer.getBlockModel(state);
                for (RenderType layer : model.getRenderTypes(state, FakeLevel.getInstance().random, ModelData.EMPTY)) {
                    matrix.pushPose();
                    Lighting.setupForFlatItems();
                    blockRenderer.renderSingleBlock(state, matrix, buffers, (int) (LightTexture.FULL_BLOCK * 0.8), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, layer);
                    if (!state.getFluidState().isEmpty() && Minecraft.getInstance().level != null){
                        matrix.translate(0,2/16f,0);
                        Minecraft.getInstance().getBlockRenderer().renderLiquid(pos,Minecraft.getInstance().level,new LiquidBlockVertexConsumer(buffers.getBuffer(ItemBlockRenderTypes.getRenderLayer(state.getFluidState())),matrix,pos),state,state.getFluidState());
                    }
                    matrix.popPose();
                }
            }

            BlockEntity te = null;
            if (state.getBlock() instanceof EntityBlock) {
                te = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);
            }

            if (te != null) {
                te.setLevel(FakeLevel.getInstance());

                // fake cached state in case the renderer checks it as we don't want to query the actual world
                //noinspection deprecation
                te.setBlockState(state);

                matrix.pushPose();
                if (shouldTranslate)matrix.translate(pos.getX(), pos.getY(), pos.getZ());
                try {
                    BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(te);
                    if (renderer != null) {
                        renderer.render(te, 0, matrix, buffers, LightTexture.pack(15, 15), OverlayTexture.NO_OVERLAY);
                    }
                } catch (Exception ignored) {
                } finally {
                    matrix.popPose();
                }
            }
        }

        private static MultiBufferSource.BufferSource initBuffers(MultiBufferSource.BufferSource original) {
            BufferBuilder fallback = ((AccessorMultiBufferSource) original).getFallbackBuffer();
            Map<RenderType, BufferBuilder> layerBuffers = ((AccessorMultiBufferSource) original).getFixedBuffers();
            Map<RenderType, BufferBuilder> remapped = new Object2ObjectLinkedOpenHashMap<>();
            for (Map.Entry<RenderType, BufferBuilder> e : layerBuffers.entrySet()) {
                remapped.put(GhostRenderLayer.remap(e.getKey()), e.getValue());
            }
            return new GhostBuffers(fallback, remapped);
        }

        private static int getLayer(){
            Player p = Minecraft.getInstance().player;
            if (InventoryUtil.hasPlayerStackInInventory(p, ModContent.PROJECTOR.get())) {
                int slot = InventoryUtil.getFirstInventoryIndex(p, ModContent.PROJECTOR.get());
                ItemStack stack = p.getInventory().getItem(slot);
                if (ProjectorItem.hasStructure(stack)) {
                    return ProjectorItem.getLayer(stack);
                }
            }
            return -1;
        }


        private static class GhostBuffers extends MultiBufferSource.BufferSource {
            protected GhostBuffers(BufferBuilder fallback, Map<RenderType, BufferBuilder> layerBuffers) {
                super(fallback, layerBuffers);
            }

            @Override
            public VertexConsumer getBuffer(RenderType type) {
                return super.getBuffer(GhostRenderLayer.remap(type));
            }
        }

        private static class GhostRenderLayer extends RenderType {
            private static final Map<RenderType, RenderType> remappedTypes = new IdentityHashMap<>();

            private GhostRenderLayer(RenderType original) {
                super(String.format("%s_%s_ghost", original.toString(), ProjectedStructures.MODID), original.format(), original.mode(), original.bufferSize(), original.affectsCrumbling(), true, () -> {
                    original.setupRenderState();

                    if (checkTransparency())RenderSystem.disableDepthTest();
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1, 1, 1, 0.5F);
                }, () -> {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    RenderSystem.disableBlend();
                    if (checkTransparency()) RenderSystem.enableDepthTest();
                    original.clearRenderState();
                });
            }

            private static boolean checkTransparency(){
                Player p = Minecraft.getInstance().player;
                if (InventoryUtil.hasPlayerStackInInventory(p, ModContent.PROJECTOR.get())) {
                    int slot = InventoryUtil.getFirstInventoryIndex(p, ModContent.PROJECTOR.get());
                    ItemStack stack = p.getInventory().getItem(slot);
                    if (ProjectorItem.hasStructure(stack)) {
                        return ProjectorItem.getTransparency(stack);
                    }
                }
                return true;
            }

            public static RenderType remap(RenderType in) {
                if (in instanceof GhostRenderLayer) {
                    return in;
                } else {
                    return remappedTypes.computeIfAbsent(in, GhostRenderLayer::new);
                }
            }
        }
    }
}
