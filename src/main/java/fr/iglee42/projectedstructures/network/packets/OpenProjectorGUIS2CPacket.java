package fr.iglee42.projectedstructures.network.packets;

import net.minecraft.network.FriendlyByteBuf;

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

    public boolean transparency() {
        return transparency;
    }

    public boolean konami() {
        return konami;
    }
}
