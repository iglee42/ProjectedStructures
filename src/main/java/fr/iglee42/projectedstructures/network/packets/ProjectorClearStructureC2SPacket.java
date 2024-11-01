package fr.iglee42.projectedstructures.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

public class ProjectorClearStructureC2SPacket {

    private final UUID playerUUID;

    public ProjectorClearStructureC2SPacket(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public ProjectorClearStructureC2SPacket(FriendlyByteBuf buf) {
        playerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID).getMainHandItem().getOrCreateTag().remove("structureName");
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID).getMainHandItem().getOrCreateTag().remove("structurePath");
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID).getMainHandItem().getOrCreateTag().remove("rotation");
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID).getMainHandItem().getOrCreateTag().remove("anchor");
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUUID).getMainHandItem().getOrCreateTag().remove("layer");
        });
        return true;
    }
}
