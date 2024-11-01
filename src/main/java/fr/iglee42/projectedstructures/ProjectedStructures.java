package fr.iglee42.projectedstructures;

import com.mojang.logging.LogUtils;
import fr.iglee42.projectedstructures.network.ModMessages;
import fr.iglee42.projectedstructures.utils.ConfigStructures;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ProjectedStructures.MODID)
public class ProjectedStructures {

    public static final String MODID = "projectedstructures";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ModelProperty<BlockState> PS_BLOCKSTATE = new ModelProperty<>();
    public static final ModelProperty<FluidState> PS_FLUIDSTATE = new ModelProperty<>();

    public ProjectedStructures() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContent.ITEMS.register(bus);
        ModContent.BLOCKS.register(bus);
        ModContent.BLOCK_ENTITIES.register(bus);
       // ModContent.MENUS.register(bus);
        ModMessages.register();
        ConfigStructures.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        bus.addListener(this::addCreative);
       /* DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(ClientEvents::onBlockColors);
            bus.addListener(ClientEvents::onModelBaked);
            bus.addListener(ClientEvents::onTextureStitch);
            bus.addListener(ClientEvents::onTextureStitched);
        });*/
        //TemplateWorldCreator.CREATOR.setValue(TemplateWorld::new);
    }


    public void addCreative(BuildCreativeModeTabContentsEvent event){
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
            event.accept(ModContent.PROJECTOR);
    }



}
