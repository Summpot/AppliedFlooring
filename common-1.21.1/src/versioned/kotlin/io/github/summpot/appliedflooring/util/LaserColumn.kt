package io.github.summpot.appliedflooring.util

import io.github.summpot.appliedflooring.block.MELaserConnectorBlock
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

object LaserColumn {

    fun collectConnectors(level: Level, start: BlockPos): List<MELaserConnectorBlockEntity> {
        val startBe = level.getBlockEntity(start) as? MELaserConnectorBlockEntity ?: return emptyList()
        val color = startBe.currentColor
        var bottom = startBe
        var guard = 0
        while (guard++ < 64) {
            val below = findNeighbor(level, bottom.blockPos, -1, color) ?: break
            bottom = below
        }

        val result = mutableListOf<MELaserConnectorBlockEntity>()
        var current: MELaserConnectorBlockEntity? = bottom
        while (current != null && result.size < 64) {
            result.add(current)
            current = findNeighbor(level, current.blockPos, 1, color)
        }
        return result
    }

    fun findNeighbor(level: Level, pos: BlockPos, dir: Int, color: appeng.api.util.AEColor): MELaserConnectorBlockEntity? {
        val maxDist = AppliedFlooringConfig.laserMaxDistance
        for (d in 1..maxDist) {
            val check = if (dir > 0) pos.above(d) else pos.below(d)
            val state = level.getBlockState(check)
            if (state.isAir || !state.isSolidRender(level, check)) {
                continue
            }
            val block = state.block as? MELaserConnectorBlock ?: return null
            if (block.color != color) return null
            return level.getBlockEntity(check) as? MELaserConnectorBlockEntity
        }
        return null
    }

    fun playerCanAccess(player: Player, targetPos: BlockPos): Boolean {
        val level = player.level()
        val column = collectConnectors(level, targetPos)
        val positions = if (column.isNotEmpty()) {
            column.map { it.blockPos }
        } else {
            listOf(targetPos)
        }
        return positions.any { pos ->
            player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }
}
