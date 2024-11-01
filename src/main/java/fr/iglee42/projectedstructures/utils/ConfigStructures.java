package fr.iglee42.projectedstructures.utils;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigStructures {

    private static Path STRUCTURES_PATH;

    public static void init(){
        Path configPaths = FMLPaths.CONFIGDIR.get().resolve("projectedstructure");

        STRUCTURES_PATH = configPaths.resolve("structures");

        STRUCTURES_PATH.toFile().mkdirs();
    }


    public static List<String> getStructures(){
        return readStructureDir(STRUCTURES_PATH.toFile());
    }

    private static List<String> readStructureDir(File dir){
        List<String> structures = new ArrayList<>();
        if (dir.exists()) {
            for (String file : dir.list()) {
                File f = new File(dir, file);
                if (f.isDirectory()) structures.addAll(readStructureDir(f));
                else if (f.getName().endsWith(".nbt") || f.getName().endsWith(".snbt")) structures.add(file);
            }
        }
        return structures;
    }

    public static List<String> getStructuresPaths(){
        return readStructurePath(STRUCTURES_PATH.toFile());
    }
    private static List<String> readStructurePath(File dir){
        List<String> structures = new ArrayList<>();
        for (String file : dir.list()) {
            File f = new File(dir, file);
            if (f.isDirectory()) structures.addAll(readStructurePath(f));
            else if (f.getName().endsWith(".nbt") || f.getName().endsWith(".snbt")) structures.add(getParentsOfStructure(f));
        }
        return structures;
    }
    private static String getParentsOfStructure(File file){
        StringBuilder out = new StringBuilder();
        out.append(getParent(file));
        out.append("/")
           .append(file.getName());
        return out.toString();
    }

    private static String getParent(File dir){
        if (!dir.getParentFile().getAbsolutePath().equals(STRUCTURES_PATH.toAbsolutePath().toString())){
            return getParent(dir.getParentFile()) + "/" +  dir.getParentFile().getName();
        }
        return "";
    }

   /* public static DynamicTexture getImage(String path) {
        File iconFile = new File(IMAGES_PATH.toFile(),path + ".png");
        boolean flag = !path.isEmpty() && iconFile.exists();
        if (flag) {
            try {
                InputStream inputstream = new FileInputStream(iconFile);

                DynamicTexture dynamictexture1;
                try {
                    NativeImage nativeimage = NativeImage.read(inputstream);
                    Validate.validState(nativeimage.getWidth() == 512, "Must be 512 pixels wide");
                    Validate.validState(nativeimage.getHeight() == 512, "Must be 512 pixels high");
                    DynamicTexture dynamictexture = new DynamicTexture(nativeimage);
                    Minecraft.getInstance().getTextureManager().register(new ResourceLocation(ProjectedStructures.MODID,path), dynamictexture);
                    dynamictexture1 = dynamictexture;
                } catch (Throwable throwable1) {
                    try {
                        inputstream.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }

                    throw throwable1;
                }

                inputstream.close();
                return dynamictexture1;
            } catch (Throwable throwable2) {
                return null;
            }
        } else {
            Minecraft.getInstance().getTextureManager().release(new ResourceLocation(ProjectedStructures.MODID,path));
            return null;
        }
    }
*/
    public static File getStructurePath(String structureName, String path) {
        return new File(STRUCTURES_PATH.toFile(),path.substring(0,path.length() - structureName.length()));
    }

    public static File getStructure(String structurePath) {
        return new File(STRUCTURES_PATH.toFile(),structurePath.substring(1));
    }

    public static Path getFullPath(String structurePath) {
        return STRUCTURES_PATH.resolve(structurePath.substring(1));
    }
}
