package fr.iglee42.projectedstructures.network.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

public class ProjectorSwitchTransparencyC2SPacket {

    private boolean transparency;
    private UUID playerUUID;

    public ProjectorSwitchTransparencyC2SPacket(boolean transparency, UUID playerUUID) {
        this.transparency = transparency;
        this.playerUUID = playerUUID;
    }

    public ProjectorSwitchTransparencyC2SPacket(FriendlyByteBuf buf) {
        this.transparency = buf.readBoolean();
        this.playerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(transparency);
        buf.writeUUID(playerUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            CompoundTag tag = player.getMainHandItem().getOrCreateTag();
            tag.putBoolean("transparency",transparency);
        });
        return true;
    }
}
