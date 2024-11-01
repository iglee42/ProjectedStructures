package fr.iglee42.projectedstructures.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class StructureButton extends Button {

    private final String structureName,path;

    public StructureButton(int x, int y, int width, int height, Component message, OnPress onPress, String structureName, String path) {
        super(x, y, width, height, message, onPress, Supplier::get);
        this.structureName = structureName;
        this.path = path;
    }

    public String getStructureName() {
        return structureName;
    }

    public String getPath() {
        return path;
    }



    @Override
    public boolean equals(Object obj) {
        return obj instanceof StructureButton bt && bt.getStructureName().equals(structureName) && bt.getPath().equals(path);
    }
}
