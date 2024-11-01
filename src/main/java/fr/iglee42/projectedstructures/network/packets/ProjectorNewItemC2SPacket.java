package fr.iglee42.projectedstructures.network.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

public class ProjectorNewItemC2SPacket {

    private String name,path;
    private UUID playerUUID;

    public ProjectorNewItemC2SPacket(String name, String path, UUID playerUUID) {
        this.name = name;
        this.path = path;
        this.playerUUID = playerUUID;
    }

    public ProjectorNewItemC2SPacket(FriendlyByteBuf buf) {
        int nameLength = buf.readInt();
        char[] nameChars = new char[nameLength];
        for (int i = 0; i < nameLength; i++){
            nameChars[i] = buf.readChar();
        }
        this.name = String.valueOf(nameChars);
        int pathLength = buf.readInt();
        char[] pathChars = new char[pathLength];
        for (int i = 0; i < pathLength; i++){
            pathChars[i] = buf.readChar();
        }
        this.path = String.valueOf(pathChars);
        this.playerUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(name.length());
        for (int i = 0;i < name.length();i++){
            buf.writeChar(name.charAt(i));
        }
        buf.writeInt(path.length());
        for (int i = 0; i < path.length(); i++){
            buf.writeChar(path.charAt(i));
        }
        buf.writeUUID(playerUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            CompoundTag tag = player.getMainHandItem().getOrCreateTag();
            tag.putString("structureName", name);
            tag.putString("structurePath", path);
        });
        return true;
    }
}
