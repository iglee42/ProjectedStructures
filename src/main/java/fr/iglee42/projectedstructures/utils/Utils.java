package fr.iglee42.projectedstructures.utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;

public class Utils {

    public static StructureTemplate getStructureTemplate(String path) throws IOException, CommandSyntaxException {
        if (path.isEmpty()) return null;
        if (!ConfigStructures.getFullPath(path).toFile().exists()) return null;
        StructureTemplate template = new StructureTemplate();
        CompoundTag nbt;
        if (path.endsWith(".snbt")) {
            nbt = NbtUtils.snbtToStructure(IOUtils.toString(Files.newBufferedReader(ConfigStructures.getFullPath(path))));
        } else {
            nbt = NbtIo.readCompressed(ConfigStructures.getStructure(path));
        }
        if (nbt!= null) {
            template.load(BuiltInRegistries.BLOCK.asLookup(), nbt);
            return template;
        }
        return null;
    }
}
