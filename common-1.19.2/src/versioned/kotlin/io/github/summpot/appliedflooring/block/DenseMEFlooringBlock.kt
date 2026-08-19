package io.github.summpot.appliedflooring.block

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.DenseMEFlooringBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class DenseMEFlooringBlock(
    properties: BlockBehaviour.Properties,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlock(properties, color, isDense = true) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return DenseMEFlooringBlockEntity(ModBlockEntities.DENSE_ME_FLOORING_BE.get(), pos, state, color)
    }
}
