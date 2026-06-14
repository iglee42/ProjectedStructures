package fr.iglee42.projectedstructures.network.packets;

import fr.iglee42.projectedstructures.client.ProjectorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenProjectorGuiHandler {

    public static void handle(OpenProjectorGUIS2CPacket packet, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{
            Minecraft.getInstance().setScreen(new ProjectorScreen(packet.transparency(),packet.konami()));
        });
    }
}
