package io.github.summpot.appliedflooring.network

import appeng.api.util.AEColor
import dev.architectury.networking.NetworkManager
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.blockentity.FloorShape
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.blockentity.ReplaceMode
import io.github.summpot.appliedflooring.util.LaserColumn
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object LaserConnectorNetwork {

    const val ACTION_SET_TARGET_OFFSET = 0
    const val ACTION_PLACE_REMOTE_CONNECTOR = 1
    const val ACTION_SET_SHAPE = 2
    const val ACTION_SET_RADIUS = 3
    const val ACTION_SET_COLOR = 4
    const val ACTION_SET_REPLACE_MODE = 5
    const val ACTION_START_BUILD = 6
    const val ACTION_PAUSE_BUILD = 7
    const val ACTION_RESET_BUILD = 8

    data class C2SActionPayload(
        val pos: BlockPos,
        val actionType: Int,
        val intVal1: Int = 0,
        val intVal2: Int = 0
    ) : CustomPacketPayload {

        override fun type(): CustomPacketPayload.Type<C2SActionPayload> = TYPE

        companion object {
            val TYPE = CustomPacketPayload.Type<C2SActionPayload>(
                ResourceLocation.fromNamespaceAndPath(AppliedFlooringMod.MOD_ID, "laser_action")
            )

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, C2SActionPayload> = StreamCodec.of(
                { buf, payload ->
                    buf.writeBlockPos(payload.pos)
                    buf.writeVarInt(payload.actionType)
                    buf.writeVarInt(payload.intVal1)
                    buf.writeVarInt(payload.intVal2)
                },
                { buf ->
                    C2SActionPayload(
                        buf.readBlockPos(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt()
                    )
                }
            )
        }
    }

    fun init() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), C2SActionPayload.TYPE, C2SActionPayload.STREAM_CODEC) { payload, context ->
            context.queue {
                val player = context.player as? ServerPlayer ?: return@queue
                val level = player.level()
                if (!LaserColumn.playerCanAccess(player, payload.pos)) return@queue
                val be = level.getBlockEntity(payload.pos) as? MELaserConnectorBlockEntity ?: return@queue

                when (payload.actionType) {
                    ACTION_SET_TARGET_OFFSET -> be.handleSetTargetOffset(payload.intVal1)
                    ACTION_PLACE_REMOTE_CONNECTOR -> be.handlePlaceRemoteConnector(player)
                    ACTION_SET_SHAPE -> be.handleSetShape(FloorShape.byId(payload.intVal1))
                    ACTION_SET_RADIUS -> be.handleSetRadius(payload.intVal1, payload.intVal2)
                    ACTION_SET_COLOR -> {
                        val colors = AEColor.values()
                        val color = if (payload.intVal1 in colors.indices) colors[payload.intVal1] else AEColor.TRANSPARENT
                        be.handleSetColor(color)
                    }
                    ACTION_SET_REPLACE_MODE -> be.handleSetReplaceMode(ReplaceMode.byId(payload.intVal1))
                    ACTION_START_BUILD -> be.handleStartBuild()
                    ACTION_PAUSE_BUILD -> be.handlePauseBuild()
                    ACTION_RESET_BUILD -> be.handleResetBuild()
                }
            }
        }
    }

    fun sendAction(pos: BlockPos, actionType: Int, intVal1: Int = 0, intVal2: Int = 0) {
        NetworkManager.sendToServer(C2SActionPayload(pos, actionType, intVal1, intVal2))
    }
}
