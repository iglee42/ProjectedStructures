package fr.iglee42.projectedstructures.network.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

public class ProjectorChangeLayerC2SPacket {

    private UUID playerUUID;
    private final int layer;

    public ProjectorChangeLayerC2SPacket(UUID playerUUID, int layer) {
        this.playerUUID = playerUUID;
        this.layer = layer;
    }

    public ProjectorChangeLayerC2SPacket(FriendlyByteBuf buf) {
        this.playerUUID = buf.readUUID();
        this.layer = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeInt(layer);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            CompoundTag tag = player.getMainHandItem().getOrCreateTag();
            tag.putInt("layer", layer);
        });
        return true;
    }
}
