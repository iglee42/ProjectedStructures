package fr.iglee42.projectedstructures.network.packets;

import fr.iglee42.projectedstructures.client.ProjectorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenProjectorGUIS2CPacket {


    private final boolean transparency;
    private final boolean konami;

    public OpenProjectorGUIS2CPacket(boolean transparency,boolean konami) {
        this.transparency = transparency;
        this.konami = konami;
    }

    public OpenProjectorGUIS2CPacket(FriendlyByteBuf buf){
        this.transparency = buf.readBoolean();
        this.konami = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(transparency);
        buf.writeBoolean(konami);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{
            Minecraft.getInstance().setScreen(new ProjectorScreen(transparency,konami));
        });
        return true;
    }
}
