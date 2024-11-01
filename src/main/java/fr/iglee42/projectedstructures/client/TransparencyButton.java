package fr.iglee42.projectedstructures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.iglee42.projectedstructures.ProjectedStructures;
import fr.iglee42.projectedstructures.network.ModMessages;
import fr.iglee42.projectedstructures.network.packets.ProjectorSwitchTransparencyC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class TransparencyButton extends Button {

    public static final ResourceLocation NO_TEXTURE = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/btn_no_transparency.png");
    public static final ResourceLocation TRANSPARENCY_TEXTURE = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/btn_transparency.png");

    private boolean transparency;

    public TransparencyButton(int x, int y,boolean initial) {
        super(x, y, 20, 20, Component.empty(), btn->{}, Supplier::get);
        setTooltip(Tooltip.create(Component.literal("(Des)Activate the fact that blocks can be viewed through walls")));
        this.transparency = initial;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int p_282682_, int p_281714_, float p_282542_) {
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        graphics.blit(transparency ? TRANSPARENCY_TEXTURE : NO_TEXTURE,this.getX(),this.getY(),0,getTextureY(),20,20,20,60);

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private int getTextureY() {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (this.isHoveredOrFocused()) {
            i = 2;
        }

        return  i * 20;
    }

    @Override
    public void onPress() {
        transparency = !transparency;
        ModMessages.sendToServer(new ProjectorSwitchTransparencyC2SPacket(transparency,Minecraft.getInstance().player.getUUID()));
    }
}
