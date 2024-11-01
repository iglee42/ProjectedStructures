package fr.iglee42.projectedstructures.network.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

public class ProjectorSwitchKonamiC2SPacket {

    private UUID playerUUID;
    private final boolean konami;

    public ProjectorSwitchKonamiC2SPacket( UUID playerUUID, boolean konami) {
        this.playerUUID = playerUUID;
        this.konami = konami;
    }

    public ProjectorSwitchKonamiC2SPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.konami = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeBoolean(konami);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            CompoundTag tag = player.getMainHandItem().getOrCreateTag();
            tag.putBoolean("konami", konami);
        });
        return true;
    }
}
