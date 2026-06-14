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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.minecraft.client.gui.components.AbstractWidget.WIDGETS_LOCATION;

public class ProjectorScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/projector_gui.png");
    private static final ResourceLocation EASTER_EGG = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/easter_egg.png");
    private static final ResourceLocation FILE = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/file.png");
    private static final ResourceLocation FOLDER = new ResourceLocation(ProjectedStructures.MODID,"textures/gui/folder.png");
    private static final int MAX_PAGE_SIZE = 4;

    private final int imgWidth = 256;
    private final int imgHeight = 256;
    private int baseY;

    private int x;
    private int y;

    private StructureButton selectedButton = null;
    private final List<FileSystemNode> currentFolderItems = new ArrayList<>();
    private final List<FileSystemNode> filteredItems = new ArrayList<>();
    private int currentPage = 0;

    private Button previousPageButton;
    private Button nextPageButton;
    private Button validateButton;
    private Button backButton;
    private final boolean initialTransparency;
    private TransparencyButton transparencyButton;

    private EditBox searchField;

    public boolean konami;
    public int konamiTime = 0;

    private FileSystemNode rootNode;
    private FileSystemNode currentNode;
    private final List<String> paths;

    public ProjectorScreen(boolean transparency, boolean konami) {
        super(Component.translatable("item.projectedstructures.projector"));
        this.initialTransparency = transparency;
        this.konami = konami;
        this.paths = new ArrayList<>(ConfigStructures.getStructuresPaths().stream().map(s -> s.substring(1)).toList());
    }

    @Override
    protected void init() {
        super.init();
        x = (this.width - this.imgWidth) / 2;
        y = (this.height - this.imgHeight) / 2;
        baseY = y + 21;
        currentPage = 0;

        searchField = addRenderableWidget(new EditBox(Minecraft.getInstance().font, x + 28, baseY + 127, 78, 16, Component.empty()));
        searchField.setHint(Component.translatable("gui.projectedstructures.search"));
        // Build the file system tree
        buildFileSystemTree();
        currentNode = rootNode;
        updateCurrentFolderItems();

        // Create navigation buttons
        previousPageButton = addRenderableWidget(new Button.Builder(Component.literal("▲"), btn -> {
            if (currentPage - 1 >= 0) {
                currentPage--;
                if (selectedButton != null) selectedButton.active = true;
                selectedButton = null;
            }
        }).pos(x + 6, baseY + 20).size(100, 11).createNarration(Supplier::get).build());

        nextPageButton = addRenderableWidget(new Button.Builder(Component.literal("▼"), btn -> {
            if ((currentPage + 1) * MAX_PAGE_SIZE < filteredItems.size()) {
                currentPage++;
                if (selectedButton != null) selectedButton.active = true;
                selectedButton = null;
            }
        }).pos(x + 6, baseY + 111).size(100, 11).createNarration(Supplier::get).build());

        backButton = addRenderableWidget(new Button.Builder(Component.literal("◀"), btn -> {
            if (currentNode.parent != null) {
                currentNode = currentNode.parent;
                updateCurrentFolderItems();
                currentPage = 0;
                selectedButton = null;
            }
        }).pos(x + 6, baseY).size(20, 20).createNarration(Supplier::get).build());

        validateButton = addRenderableWidget(new Button.Builder(Component.translatable("gui.projectedstructures.validate"), btn -> {
            if (selectedButton != null && selectedButton.getPath() != null) {
                ModMessages.sendToServer(new ProjectorNewItemC2SPacket(selectedButton.getStructureName(), selectedButton.getPath(), Minecraft.getInstance().player.getUUID()));
                Minecraft.getInstance().setScreen(null);
            }
        }).pos(x + 110, y + 147).size(imgWidth - 120, 15).createNarration(Supplier::get).build());

        transparencyButton = addRenderableWidget(new TransparencyButton(x + 6, baseY + 125, initialTransparency));
    }

    private void buildFileSystemTree() {
        rootNode = new FileSystemNode("root", null, true, null);

        for (String path : paths) {
            String[] parts = path.split("/");
            FileSystemNode current = rootNode;
            StringBuilder fullPath = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isEmpty()) continue;

                if (i > 0) fullPath.append("/");
                fullPath.append(part);

                FileSystemNode existing = current.getChild(part);
                if (existing == null) {
                    boolean isFolder = i < parts.length - 1;
                    String structPath = isFolder ? null : fullPath.toString();
                    existing = new FileSystemNode(part, current, isFolder, structPath);
                    current.addChild(existing);
                }
                current = existing;
            }
        }
        rootNode.sortChildren();
    }

    private void updateCurrentFolderItems() {
        currentFolderItems.clear();
        currentFolderItems.addAll(currentNode.children);
        applyFilter();
    }

    private void applyFilter() {
        filteredItems.clear();
        String searchText = searchField.getValue().toLowerCase();

        for (FileSystemNode item : currentFolderItems) {
            if (searchText.isEmpty() || item.name.toLowerCase().contains(searchText)) {
                filteredItems.add(item);
            }
        }

        currentPage = Mth.clamp(currentPage, 0, Math.max(0, (filteredItems.size() - 1) / MAX_PAGE_SIZE));
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
        super.render(poseStack, mouseX, mouseY, pt);

        // Apply search filter
        applyFilter();

        poseStack.enableScissor(x+6, baseY, x+106, baseY + 122);
        // Render folder items
        int visibleIndex = 0;
        for (int i = currentPage * MAX_PAGE_SIZE; i < filteredItems.size() && visibleIndex < MAX_PAGE_SIZE; i++, visibleIndex++) {
            FileSystemNode item = filteredItems.get(i);
            int itemY = baseY + 33 + (visibleIndex * 20);


            // Render item button
            boolean isSelected = selectedButton != null && selectedButton.getPath() != null
                && selectedButton.getPath().equals(item.structurePath);
            int color = isSelected ? 0xFF00FF00 : 0xFFFFFFFF;
            String name = item.name.replaceAll("\\.s*nbt", "");
            String displayName = item.isFolder ? name : ModsUtils.getUpperName(name,"_");

            boolean isHovered = mouseX >= x + 6 && mouseX < x + 106 && mouseY >= itemY - 2 && mouseY < itemY + 18;

            poseStack.blitNineSliced(WIDGETS_LOCATION, x+6, itemY-2, 100, 20, 20, 4, 200, 20, 0, 46 +(isSelected ? 0 : (isHovered ? 2 : 1))*20);
            poseStack.blit(item.isFolder? FOLDER : FILE, x+8, itemY, 0, 0, 16, 16, 16, 16);
            poseStack.enableScissor(x+6, itemY, x+103, itemY + 18);
            poseStack.drawString(font, displayName, x + 25, itemY + 4, color);
            poseStack.disableScissor();
        }

        // Render buttons
        previousPageButton.render(poseStack, mouseX, mouseY, pt);
        nextPageButton.render(poseStack, mouseX, mouseY, pt);
        backButton.render(poseStack, mouseX, mouseY, pt);
        validateButton.render(poseStack, mouseX, mouseY, pt);
        transparencyButton.render(poseStack, mouseX, mouseY, pt);
        searchField.render(poseStack, mouseX, mouseY, pt);

        // Display current path
        String currentPath = getCurrentPath();
        poseStack.drawString(font, Component.translatable("gui.projectedstructures.path",currentPath), x + 27, baseY + 6, ChatFormatting.WHITE.getColor());

        poseStack.disableScissor();
        // Show structure info and preview
        if (selectedButton != null && selectedButton.getPath() != null) {
            renderStructureInfo(poseStack, selectedButton.getPath(), pt);
        } else if (currentFolderItems.isEmpty() && searchField.getValue().isEmpty()) {
            poseStack.drawCenteredString(font, Component.translatable("gui.projectedstructures.folder_empty"), x + 56, y + 56, ChatFormatting.WHITE.getColor());
        } else if (filteredItems.isEmpty() && !searchField.getValue().isEmpty()) {
            poseStack.drawCenteredString(font, Component.translatable("gui.projectedstructures.no_search_results"), x + 56, y + 56, ChatFormatting.WHITE.getColor());
        }

        // Update button states
        this.previousPageButton.active = currentPage > 0;
        this.nextPageButton.active = (currentPage + 1) * MAX_PAGE_SIZE < filteredItems.size();
        this.backButton.active = currentNode.parent != null;
        this.validateButton.visible = selectedButton != null;

        // Render konami easter egg
        if (konamiTime > 0 && konami) {
            renderKonamiText(poseStack, pt);
        }
    }

    private String getCurrentPath() {
        List<String> pathParts = new ArrayList<>();
        FileSystemNode node = currentNode;
        while (node != null && !node.name.equals("root")) {
            pathParts.add(0, node.name);
            node = node.parent;
        }
        return pathParts.isEmpty() ? "/" : "/" + String.join("/", pathParts);
    }

    private void renderStructureInfo(GuiGraphics poseStack, String path, float pt) {
        try {
            StructureTemplate template = Utils.getStructureTemplate(path);
            if (template != null) {
                Vec3i size = template.getSize();
                poseStack.drawString(font, Component.translatable("gui.projectedstructures.name",ModsUtils.getUpperName(selectedButton.getStructureName().replaceAll("\\.s*nbt", ""),"_")), x + 110, y + 110, ChatFormatting.WHITE.getColor());
                poseStack.drawString(font, Component.translatable("gui.projectedstructures.path",selectedButton.getPath()), x + 110, y + 120, ChatFormatting.WHITE.getColor());
                poseStack.drawString(font, Component.translatable("gui.projectedstructures.size",size.getX(),size.getY(),size.getZ()), x + 110, y + 130, ChatFormatting.WHITE.getColor());

                try {
                    if (!(size.getX() > 24 || size.getY() > 24 || size.getZ() > 24)) {
                        poseStack.pose().pushPose();
                        RenderSystem.setShaderColor(1, 1, 1, 1);
                        ClientEvents.ForgeBus.renderStructure(path, poseStack.pose(), x + imgWidth / 2 + 40, y + imgHeight / 2 - 45, (this.width) / 6);
                        poseStack.pose().popPose();
                    } else {
                        poseStack.drawString(font, Component.translatable("gui.projectedstructures.structure_too_large"), x + 110, y + 40, ChatFormatting.RED.getColor());
                        poseStack.drawString(font, Component.translatable("gui.projectedstructures.max_size",24,24,24), x + 110, y + 50, ChatFormatting.RED.getColor());
                    }
                } catch (IOException | CommandSyntaxException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private void renderKonamiText(GuiGraphics poseStack, float pt) {
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


    @Override
    public void resize(Minecraft p_96575_, int p_96576_, int p_96577_) {
        super.resize(p_96575_, p_96576_, p_96577_);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScroll) {
        // Scroll through pages
        int maxPages = Math.max(0, (filteredItems.size() - 1) / MAX_PAGE_SIZE);
        if (pScroll > 0 && currentPage > 0) {
            currentPage--;
            selectedButton = null;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        } else if (pScroll < 0 && currentPage < maxPages) {
            currentPage++;
            selectedButton = null;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }

        // Handle item clicks
        int visibleIndex = 0;
        for (int i = currentPage * MAX_PAGE_SIZE; i < filteredItems.size() && visibleIndex < MAX_PAGE_SIZE; i++, visibleIndex++) {
            FileSystemNode item = filteredItems.get(i);
            int itemY = baseY + 33 + (visibleIndex * 20);

            if (pMouseX >= x + 6 && pMouseX < x + 106 && pMouseY >= itemY - 2 && pMouseY < itemY + 18) {
                if (item.isFolder) {
                    // Navigate into folder
                    currentNode = item;
                    updateCurrentFolderItems();
                    currentPage = 0;
                    selectedButton = null;
                } else {
                    // Select file
                    selectedButton = new StructureButton(0, 0, 0, 0, Component.literal(item.name), null, item.name, item.structurePath);
                }
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Inner class to represent a file system node (file or folder)
     */
    private static class FileSystemNode {
        final String name;
        final FileSystemNode parent;
        final boolean isFolder;
        final String structurePath; // null for folders
        final List<FileSystemNode> children = new ArrayList<>();

        FileSystemNode(String name, FileSystemNode parent, boolean isFolder, String structurePath) {
            this.name = name;
            this.parent = parent;
            this.isFolder = isFolder;
            this.structurePath = structurePath;
        }

        FileSystemNode getChild(String name) {
            return children.stream().filter(c -> c.name.equals(name)).findFirst().orElse(null);
        }

        void addChild(FileSystemNode child) {
            children.add(child);
        }

        void sortChildren() {
            children.sort((a, b) -> {
                if (a.isFolder && !b.isFolder) return -1;
                if (!a.isFolder && b.isFolder) return 1;
                return a.name.compareTo(b.name);
            });
            children.forEach(FileSystemNode::sortChildren);
        }
    }
}
