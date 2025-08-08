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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private StructureButton selectedButton = null;
    private final List<StructureButton> buttons = new ArrayList<>();
    private final List<StructureButton> showedButtons = new ArrayList<>();
    private int currentPage = 0;

    private Button previousPageButton;
    private Button nextPageButton;
    private Button validateButton;
    private final boolean initialTransparency;
    private TransparencyButton transparencyButton;

    private EditBox searchField;

    public boolean konami;
    public int konamiTime = 0;

    private List<String> paths;

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
        paths = new ArrayList<>(ConfigStructures.getStructuresPaths().stream().map(s->s.substring(1)).toList());
        paths.sort((s1,s2)-> {
                boolean s1Slash = s1.contains("/");
                boolean s2Slash = s2.contains("/");

                if (s1Slash && !s2Slash) {
                    return -1;
                } else if (!s1Slash && s2Slash) {
                    return 1;
                } else {
                    return s1.compareTo(s2);
                }
            });
        previousPageButton = addRenderableWidget(new Button.Builder(Component.literal("▲"),btn->{
            if (currentPage - 1 >= 0) {
                currentPage--;
                if (selectedButton != null)selectedButton.active = true;
                selectedButton = null;
            }
        }).pos(x + 6, baseY).size( 100, 11).createNarration( Supplier::get).build());
        nextPageButton = addRenderableWidget(new Button.Builder(Component.literal("▼"),btn->{
            if ((currentPage + 1) * MAX_PAGE_SIZE  < buttons.size()) {
                currentPage++;
                if (selectedButton != null)selectedButton.active = true;
                selectedButton = null;
            }
        }).pos(x + 6, baseY+111).size( 100, 11).createNarration(Supplier::get).build());
        validateButton = addRenderableWidget(new Button.Builder(Component.literal("Validate"),btn->{
            ModMessages.sendToServer(new ProjectorNewItemC2SPacket(selectedButton.getStructureName(),selectedButton.getPath(), Minecraft.getInstance().player.getUUID()));
            Minecraft.getInstance().setScreen(null);
        }).pos(x+120,y + 147).size(imgWidth - 130,15).createNarration(Supplier::get).build());

        transparencyButton = addRenderableWidget(new TransparencyButton(x + 6,baseY + 125,initialTransparency));
        searchField = addRenderableWidget(new EditBox(Minecraft.getInstance().font,x + 28,baseY + 127,78,16,Component.empty()));
        searchField.setHint(Component.literal("Search"));

        renderables.clear();

        paths.forEach(p->{
            int index = paths.indexOf(p);
            String[] splittedPath = p.split("/");
            StructureButton b = new StructureButton(x + 6, baseY + 11 + (index % MAX_PAGE_SIZE) * 20, 100, 20, Component.literal(ModsUtils.getUpperName(splittedPath[splittedPath.length - 1],"_").replaceAll("\\.s*nbt","")), bt -> selectedButton = (StructureButton) bt,splittedPath[splittedPath.length - 1],p);
            buttons.add(b);
            addWidget(b);
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


        super.render(poseStack,mouseX,mouseY,pt);
        showedButtons.clear();
        buttons.forEach(b->{
            if (searchField.getValue().isEmpty()){
                showedButtons.add(b);
                b.active = true;
            } else {
                if (b.getStructureName().contains(searchField.getValue()))
                    showedButtons.add(b);
            }
            if (!showedButtons.contains(b))b.active = false;
        });

        currentPage = Mth.clamp(currentPage,0,showedButtons.size() / MAX_PAGE_SIZE);

        showedButtons.forEach(b->{
            b.active = !b.equals(selectedButton);
            b.setY(baseY + 11 + (showedButtons.indexOf(b) % MAX_PAGE_SIZE) * 20);
            b.visible = buttons.indexOf(b) >= currentPage * MAX_PAGE_SIZE && showedButtons.indexOf(b) < currentPage * MAX_PAGE_SIZE + MAX_PAGE_SIZE;
            b.render(poseStack, mouseX, mouseY, pt);
        });
        previousPageButton.render(poseStack, mouseX, mouseY, pt);
        nextPageButton.render(poseStack, mouseX, mouseY, pt);
        validateButton.render(poseStack, mouseX, mouseY, pt);
        transparencyButton.render(poseStack, mouseX, mouseY, pt);
        searchField.render(poseStack, mouseX, mouseY, pt);
        if (paths == null || paths.isEmpty()){
            poseStack.drawCenteredString(font,"There is no structure !",x + 50,y+30, Color.WHITE.getRGB());
        }
        if (selectedButton != null){
            this.showedButtons.forEach(b->b.active = !b.equals(selectedButton));
            //renderStructureImage(selectedButton.getPath(),poseStack);
            renderScrollingString(poseStack,font,Component.literal("Name : " + ModsUtils.getUpperName(selectedButton.getStructureName(),"_").replaceAll("\\.s*nbt","")),x + 120, y +110,x+imgWidth - 8, y + 119, ChatFormatting.WHITE.getColor());
            renderScrollingString(poseStack,font,Component.literal("Path : " + selectedButton.getPath()),x + 120, y + 122,x+imgWidth - 8, y + 131 , ChatFormatting.WHITE.getColor());
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
        this.nextPageButton.active = (currentPage + 1) * MAX_PAGE_SIZE < showedButtons.size();
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

    @Override
    public void resize(Minecraft p_96575_, int p_96576_, int p_96577_) {
        this.buttons.clear();
        this.showedButtons.clear();
        super.resize(p_96575_, p_96576_, p_96577_);
    }

    private void renderStructureImage(String path, PoseStack stack) {
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
    protected static void renderScrollingString(GuiGraphics p_281620_, Font p_282651_, Component p_281467_, int p_283621_, int p_282084_, int p_283398_, int p_281938_, int p_283471_) {
        int i = p_282651_.width(p_281467_);
        int j = (p_282084_ + p_281938_ - 9) / 2 + 1;
        int k = p_283398_ - p_283621_;
        if (i > k) {
            int l = i - k;
            double d0 = (double) Util.getMillis() / 1000.0D;
            double d1 = Math.max((double)l * 0.5D, 3.0D);
            double d2 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * d0 / d1)) / 2.0D + 0.5D;
            double d3 = Mth.lerp(d2, 0.0D, (double)l);
            p_281620_.enableScissor(p_283621_, p_282084_, p_283398_, p_281938_);
            p_281620_.drawString(p_282651_, p_281467_, p_283621_ - (int)d3, j, p_283471_);
            p_281620_.disableScissor();
        } else {
            p_281620_.drawCenteredString(p_282651_, p_281467_, (p_283621_ + p_283398_) / 2, j, p_283471_);
        }

    }
}
