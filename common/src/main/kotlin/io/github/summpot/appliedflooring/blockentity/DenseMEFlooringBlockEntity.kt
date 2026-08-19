package io.github.summpot.appliedflooring.blockentity

import appeng.api.util.AEColor
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class DenseMEFlooringBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlockEntity(type, pos, state, isDenseCable = true, currentColor = color)
