package io.github.summpot.appliedflooring.network

import dev.architectury.networking.NetworkManager
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.util.ElevatorTeleportHelper
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object ElevatorNetwork {

    data class C2STeleportPayload(
        val destination: BlockPos,
        val goDown: Boolean = false
    ) : CustomPacketPayload {

        override fun type(): CustomPacketPayload.Type<C2STeleportPayload> = TYPE

        companion object {
            val TYPE = CustomPacketPayload.Type<C2STeleportPayload>(
                ResourceLocation.fromNamespaceAndPath(AppliedFlooringMod.MOD_ID, "elevator_teleport")
            )

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2STeleportPayload> = StreamCodec.of(
                { buf, payload ->
                    buf.writeBoolean(payload.goDown)
                    buf.writeBlockPos(payload.destination)
                },
                { buf ->
                    val goDown = buf.readBoolean()
                    val destination = buf.readBlockPos()
                    C2STeleportPayload(destination, goDown)
                }
            )
        }
    }

    fun init() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2STeleportPayload.TYPE, C2STeleportPayload.STREAM_CODEC) { payload, context ->
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                if (payload.goDown) {
                    ElevatorTeleportHelper.tryTeleport(player, false)
                } else {
                    ElevatorTeleportHelper.tryTeleportTo(player, payload.destination)
                }
            }
        }
    }

    fun sendTeleport(destination: BlockPos) {
        NetworkManager.sendToServer(C2STeleportPayload(destination, false))
    }

    fun sendGoDown() {
        NetworkManager.sendToServer(C2STeleportPayload(BlockPos.ZERO, true))
    }
}
