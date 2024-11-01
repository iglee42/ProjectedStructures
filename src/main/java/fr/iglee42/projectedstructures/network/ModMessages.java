package fr.iglee42.projectedstructures.network;

import fr.iglee42.projectedstructures.ProjectedStructures;
import fr.iglee42.projectedstructures.network.packets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(ProjectedStructures.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ProjectorNewItemC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProjectorNewItemC2SPacket::new)
                .encoder(ProjectorNewItemC2SPacket::toBytes)
                .consumerMainThread(ProjectorNewItemC2SPacket::handle)
                .add();
        net.messageBuilder(ProjectorRotateC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProjectorRotateC2SPacket::new)
                .encoder(ProjectorRotateC2SPacket::toBytes)
                .consumerMainThread(ProjectorRotateC2SPacket::handle)
                .add();
        net.messageBuilder(ProjectorClearStructureC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProjectorClearStructureC2SPacket::new)
                .encoder(ProjectorClearStructureC2SPacket::toBytes)
                .consumerMainThread(ProjectorClearStructureC2SPacket::handle)
                .add();
        net.messageBuilder(ProjectorSwitchTransparencyC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProjectorSwitchTransparencyC2SPacket::new)
                .encoder(ProjectorSwitchTransparencyC2SPacket::toBytes)
                .consumerMainThread(ProjectorSwitchTransparencyC2SPacket::handle)
                .add();
        net.messageBuilder(ProjectorSwitchKonamiC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProjectorSwitchKonamiC2SPacket::new)
                .encoder(ProjectorSwitchKonamiC2SPacket::toBytes)
                .consumerMainThread(ProjectorSwitchKonamiC2SPacket::handle)
                .add();
        net.messageBuilder(ProjectorChangeLayerC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProjectorChangeLayerC2SPacket::new)
                .encoder(ProjectorChangeLayerC2SPacket::toBytes)
                .consumerMainThread(ProjectorChangeLayerC2SPacket::handle)
                .add();
        net.messageBuilder(OpenProjectorGUIS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenProjectorGUIS2CPacket::new)
                .encoder(OpenProjectorGUIS2CPacket::toBytes)
                .consumerMainThread(OpenProjectorGUIS2CPacket::handle)
                .add();

    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}