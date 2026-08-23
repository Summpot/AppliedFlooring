package io.github.summpot.appliedflooring.block

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.MEElevatorBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import io.github.summpot.appliedflooring.util.ElevatorTeleportHelper
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class MEElevatorBlock(
    properties: BlockBehaviour.Properties,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlock(properties, color) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return MEElevatorBlockEntity(ModBlockEntities.ME_ELEVATOR_BE.get(), pos, state, currentColor = color)
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker { lvl, pos, st, be ->
            if (be is MEElevatorBlockEntity) {
                be.serverTick(lvl, pos, st)
            }
        }
    }

    fun teleport(player: ServerPlayer, up: Boolean): Boolean {
        return ElevatorTeleportHelper.tryTeleport(player, up)
    }
}
