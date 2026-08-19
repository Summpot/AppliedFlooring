package io.github.summpot.appliedflooring.block

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.WirelessChargerFlooringBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class WirelessChargerFlooringBlock(
    properties: BlockBehaviour.Properties,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlock(properties, color, isDense = false) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return WirelessChargerFlooringBlockEntity(ModBlockEntities.WIRELESS_CHARGER_FLOORING_BE.get(), pos, state, color)
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker { lvl, pos, st, be ->
            if (be is WirelessChargerFlooringBlockEntity) {
                be.serverTick(lvl, pos, st)
            }
        }
    }
}
