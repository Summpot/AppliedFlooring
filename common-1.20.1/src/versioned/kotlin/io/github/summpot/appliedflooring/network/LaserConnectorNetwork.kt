package io.github.summpot.appliedflooring.network

import appeng.api.util.AEColor
import dev.architectury.networking.NetworkManager
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.blockentity.FloorShape
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.blockentity.ReplaceMode
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object LaserConnectorNetwork {

    val ID: ResourceLocation = ResourceLocation(AppliedFlooringMod.MOD_ID, "laser_action")

    const val ACTION_SET_TARGET_OFFSET = 0
    const val ACTION_PLACE_REMOTE_CONNECTOR = 1
    const val ACTION_SET_SHAPE = 2
    const val ACTION_SET_RADIUS = 3
    const val ACTION_SET_COLOR = 4
    const val ACTION_SET_REPLACE_MODE = 5
    const val ACTION_START_BUILD = 6
    const val ACTION_PAUSE_BUILD = 7
    const val ACTION_RESET_BUILD = 8

    fun init() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), ID) { buf, context ->
            val pos = buf.readBlockPos()
            val actionType = buf.readVarInt()
            val intVal1 = buf.readVarInt()
            val intVal2 = buf.readVarInt()

            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val level = player.serverLevel()
                val be = level.getBlockEntity(pos) as? MELaserConnectorBlockEntity ?: return@queue

                when (actionType) {
                    ACTION_SET_TARGET_OFFSET -> be.handleSetTargetOffset(intVal1)
                    ACTION_PLACE_REMOTE_CONNECTOR -> be.handlePlaceRemoteConnector(player)
                    ACTION_SET_SHAPE -> be.handleSetShape(FloorShape.byId(intVal1))
                    ACTION_SET_RADIUS -> be.handleSetRadius(intVal1, intVal2)
                    ACTION_SET_COLOR -> {
                        val colors = AEColor.values()
                        val color = if (intVal1 in colors.indices) colors[intVal1] else AEColor.TRANSPARENT
                        be.handleSetColor(color)
                    }
                    ACTION_SET_REPLACE_MODE -> be.handleSetReplaceMode(ReplaceMode.byId(intVal1))
                    ACTION_START_BUILD -> be.handleStartBuild()
                    ACTION_PAUSE_BUILD -> be.handlePauseBuild()
                    ACTION_RESET_BUILD -> be.handleResetBuild()
                }
            }
        }
    }

    fun sendAction(pos: BlockPos, actionType: Int, intVal1: Int = 0, intVal2: Int = 0) {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        buf.writeBlockPos(pos)
        buf.writeVarInt(actionType)
        buf.writeVarInt(intVal1)
        buf.writeVarInt(intVal2)
        NetworkManager.sendToServer(ID, buf)
    }
}
