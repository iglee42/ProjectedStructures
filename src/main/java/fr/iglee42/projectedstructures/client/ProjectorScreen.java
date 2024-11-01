package fr.iglee42.projectedstructures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.iglee42.igleelib.api.utils.ModsUtils;
import fr.iglee42.projectedstructures.ProjectedStructures;
import fr.iglee42.projectedstructures.network.ModMessages;
import fr.iglee42.projectedstructures.network.packets.ProjectorNewItemC2SPacket;
import fr.iglee42.projectedstructures.utils.ConfigStructures;
import fr.iglee42.projectedstructures.utils.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ProjectorScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/projector_gui.png");
    private static final ResourceLocation EASTER_EGG = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/easter_egg.png");
    private static final int MAX_PAGE_SIZE = 5;

    private final int imgWidth = 256;
    private final int imgHeight = 256;
    private int baseY;


    private int x;
    private int y;
    private List<String> structures;

    private StructureButton selectedButton = null;
    private final List<List<StructureButton>> pages = new ArrayList<>();
    private int currentPage = 0;

    private Button previousPageButton;
    private Button nextPageButton;
    private Button validateButton;
    private final boolean initialTransparency;
    private TransparencyButton transparencyButton;

    public boolean konami;
    public int konamiTime = 0;

    public ProjectorScreen(boolean transparency,boolean konami) {
        super(Component.translatable("item.projectedstructures.projector"));
        this.initialTransparency = transparency;
        this.konami = konami;
    }


    @Override
    protected void init() {
        super.init();
        x = (this.width - this.imgWidth) / 2;
        y = (this.height - this.imgHeight) / 2;
        baseY = y + 21;
        //ModMessages.sendToServer(new ProjectorStructuresSyncC2SPacket());
        structures = new ArrayList<>(ConfigStructures.getStructures());
        Collections.reverse(structures);
        previousPageButton = addRenderableWidget(new Button.Builder(Component.literal("▲"),btn->{
            if (currentPage - 1 >= 0) {
                currentPage--;
                renderables.clear();
                selectedButton = null;
            }
        }).pos(x + 6, baseY).size( 100, 11).createNarration( Supplier::get).build());
        nextPageButton = addRenderableWidget(new Button.Builder(Component.literal("▼"),btn->{
            if (currentPage + 1 < pages.size()) {
                currentPage++;
                renderables.clear();
                selectedButton = null;
            }
        }).pos(x + 6, baseY+111).size( 100, 11).createNarration(Supplier::get).build());
        validateButton = addRenderableWidget(new Button.Builder(Component.literal("Validate"),btn->{
            ModMessages.sendToServer(new ProjectorNewItemC2SPacket(selectedButton.getStructureName(),selectedButton.getPath(), Minecraft.getInstance().player.getUUID()));
            Minecraft.getInstance().setScreen(null);
        }).pos(x+120,y + 147).size(imgWidth - 130,15).createNarration(Supplier::get).build());

        transparencyButton = addRenderableWidget(new TransparencyButton(x + 6,baseY + 125,initialTransparency));

        pages.clear();
        renderables.clear();
        AtomicInteger currentPageIndex = new AtomicInteger(0);
        AtomicInteger pageIndex = new AtomicInteger(0);
        List<String> paths = ConfigStructures.getStructuresPaths();
        List<String> allStructures = new ArrayList<>(structures);

        structures.forEach(r->{
            String path = paths.stream().filter(s->s.endsWith(r)).findFirst().orElse("Unknown Path");
            paths.remove(path);
            StructureButton b = new StructureButton(x + 6, baseY + 11 + currentPageIndex.get() * 20, 100, 20, Component.literal(r.replaceAll("\\.s*nbt","")), bt -> selectedButton = (StructureButton) bt,r,path);
            allStructures.remove(r);
            if (pages.size() < pageIndex.get() + 1)
                pages.add(new ArrayList<>());
            pages.get(pageIndex.get()).add(b);
            if (currentPageIndex.incrementAndGet() == 5) {
                currentPageIndex.set(0);
                pageIndex.incrementAndGet();
            }
        });
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        if (!konami)graphics.blit(BACKGROUND, x, y + 15, 0, 0, this.imgWidth, this.imgHeight);
        else graphics.blit(EASTER_EGG, x, y + 15, 0, 0, this.imgWidth, this.imgHeight);
    }


    @Override
    public void render(@NotNull GuiGraphics poseStack, int mouseX, int mouseY, float pt) {
        this.renderBackground(poseStack);

        if (currentPage >= pages.size()) currentPage = pages.size() - 1;
        this.pages.stream().filter(l->pages.indexOf(l) != currentPage).forEach(l->l.forEach(b->b.active = false));
        if (!pages.isEmpty()) {
            this.pages.get(currentPage).forEach(b -> {
                if (selectedButton != b) b.active = true;
                if (!renderables.contains(b)) addRenderableWidget(b);
            });
        }
        super.render(poseStack,mouseX,mouseY,pt);
        previousPageButton.render(poseStack, mouseX, mouseY, pt);
        nextPageButton.render(poseStack, mouseX, mouseY, pt);
        validateButton.render(poseStack, mouseX, mouseY, pt);
        transparencyButton.render(poseStack, mouseX, mouseY, pt);
        if (structures == null || structures.isEmpty()){
            poseStack.drawCenteredString(font,"There is no structure !",x + 50,y+30, Color.WHITE.getRGB());
        }
        if (selectedButton != null){
            this.pages.forEach(l->l.stream().filter(b -> selectedButton.equals(b)).findFirst().ifPresent(b->b.active = false));
            //renderStructureImage(selectedButton.getPath(),poseStack);
            poseStack.drawString(font,"Name : " + ModsUtils.getUpperName(selectedButton.getStructureName(),"_").replaceAll("\\.s*nbt",""),x + 120, y +110, ChatFormatting.WHITE.getColor());
            poseStack.drawString(font,"Path : " + selectedButton.getPath().substring(1),x + 120, y + 122 , ChatFormatting.WHITE.getColor());
            try {
                Vec3i size = Utils.getStructureTemplate(selectedButton.getPath()).getSize();
                poseStack.drawString(font, "Size : " +size.getX() + "x"+size.getY()+"x"+size.getZ(), x + 120, y + 134, ChatFormatting.WHITE.getColor());
                try {
                    if (!(size.getX() > 24 || size.getY() > 24 || size.getZ() > 24) ) {
                        poseStack.pose().pushPose();
                        RenderSystem.setShaderColor(1, 1, 1, 1);
                        ClientEvents.ForgeBus.renderStructure(selectedButton.getPath(), poseStack.pose(), x + imgWidth / 2 + 40, y + imgHeight / 2 - 45, (this.width) / 6);
                        poseStack.pose().popPose();
                    } else {
                        poseStack.drawString(font,"The structure size is",x + 120, y +40, ChatFormatting.RED.getColor());
                        poseStack.drawString(font,"too big to be rendered",x + 120, y +50, ChatFormatting.RED.getColor());
                        poseStack.drawString(font,"Max size : 24x24x24",x + 120, y +60, ChatFormatting.RED.getColor());
                    }
                } catch (IOException | CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }

            } catch (Exception ignored){}

        }
        this.previousPageButton.active = currentPage > 0;
        this.nextPageButton.active = currentPage < pages.size() -1 ;
        this.validateButton.visible = selectedButton != null;

        if (konamiTime > 0 && konami) {
            String meme = "KONAMI";
            RenderSystem.disableDepthTest();
            poseStack.pose().pushPose();
            int fullWidth = Minecraft.getInstance().font.width(meme);
            int left = width;
            double widthPerTick = (fullWidth + width) / 240;
            double currWidth = left - widthPerTick * (240 - (konamiTime - pt)) * 3.2;

            float cycle = (System.currentTimeMillis() % 5000L) / 5000.0f + pt;
            cycle = cycle - (float) Math.floor(cycle);

            int red = (int) (Math.sin(cycle * Math.PI * 2) * 127 + 128);
            int green = (int) (Math.sin((cycle + 0.33) * Math.PI * 2) * 127 + 128);
            int blue = (int) (Math.sin((cycle + 0.66) * Math.PI * 2) * 127 + 128);

            int color = (255 << 24) | (red << 16) | (green << 8) | blue;

            poseStack.pose().translate(currWidth, y + 215, 0);
            poseStack.pose().scale(4, 4, 4);
            poseStack.drawString(Minecraft.getInstance().font, meme, 0, 0, color);
            poseStack.pose().popPose();
            RenderSystem.enableDepthTest();
        }

    }

    private void renderStructureImage(String path,PoseStack stack) {
        /*if (ConfigStructures.getImage(path) != null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, new ResourceLocation(ProjectedStructures.MODID,path));
            RenderSystem.enableBlend();
            blit(stack, centerX - 110, centerY - 92, 0.0F, 0.0F, 80, 80, 80, 80);
            RenderSystem.disableBlend();
        } else {
            drawCenteredString(stack,font,"Missing image file : ",centerX - 69, centerY - 65 , ChatFormatting.WHITE.getColor());
            drawCenteredString(stack,font,selectedButton.getPath().substring(1) +".png",centerX - 71, centerY - 55, ChatFormatting.WHITE.getColor());
        }*/
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
