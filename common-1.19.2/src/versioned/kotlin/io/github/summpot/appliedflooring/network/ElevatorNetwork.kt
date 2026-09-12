package io.github.summpot.appliedflooring.network

import dev.architectury.networking.NetworkManager
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.util.ElevatorTeleportHelper
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object ElevatorNetwork {

    val ID: ResourceLocation = ResourceLocation(AppliedFlooringMod.MOD_ID, "elevator_teleport")

    fun init() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), ID) { buf, context ->
            val goDown = buf.readBoolean()
            val destination = buf.readBlockPos()
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                if (goDown) {
                    ElevatorTeleportHelper.tryTeleport(player, false)
                } else {
                    ElevatorTeleportHelper.tryTeleportTo(player, destination)
                }
            }
        }
    }

    fun sendTeleport(destination: BlockPos) {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBoolean(false)
        buf.writeBlockPos(destination)
        NetworkManager.sendToServer(ID, buf)
    }

    fun sendGoDown() {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBoolean(true)
        buf.writeBlockPos(BlockPos.ZERO)
        NetworkManager.sendToServer(ID, buf)
    }
}
