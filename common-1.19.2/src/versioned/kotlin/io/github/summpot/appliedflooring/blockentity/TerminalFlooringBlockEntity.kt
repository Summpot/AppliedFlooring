package io.github.summpot.appliedflooring.blockentity

import appeng.api.util.AEColor
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class TerminalFlooringBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlockEntity(type, pos, state, isDenseCable = false, currentColor = color) {

    override fun onEntitySteppedOn(entity: Entity) {
        super.onEntitySteppedOn(entity)
        val lvl = entity.level
        if (entity is Player && lvl != null && !lvl.isClientSide) {
            // Player standing on terminal flooring gains localized direct wireless network link
        }
    }

    fun openTerminalGui(player: Player): Boolean {
        val grid = mainNode.grid ?: return false
        val topPart = getPart(net.minecraft.core.Direction.UP)
        return false
    }
}
