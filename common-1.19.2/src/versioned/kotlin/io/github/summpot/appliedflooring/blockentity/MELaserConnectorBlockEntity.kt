package io.github.summpot.appliedflooring.blockentity

import appeng.api.networking.GridHelper
import appeng.api.networking.IGridConnection
import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.block.MELaserConnectorBlock
import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class MELaserConnectorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    isDenseCable: Boolean = true,
    currentColor: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlockEntity(type, pos, state, isDenseCable, currentColor) {

    var connectedTargetUp: BlockPos? = null
        private set

    private var activeConnectionUp: IGridConnection? = null
    private var laserScanTicks = 0

    override fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        super.serverTick(level, pos, state)

        laserScanTicks++
        if (laserScanTicks % 10 != 0) return

        if (!mainNode.isReady || isRemoved) {
            cleanupLaserConnection()
            return
        }

        updateLaserConnection(level, pos)
    }

    private fun updateLaserConnection(level: Level, pos: BlockPos) {
        val myNode = mainNode.node
        if (myNode == null) {
            cleanupLaserConnection()
            return
        }

        var foundTarget: BlockPos? = null
        var foundTargetBe: MELaserConnectorBlockEntity? = null
        val maxDist = AppliedFlooringConfig.laserMaxDistance

        for (dy in 1..maxDist) {
            val checkPos = pos.above(dy)
            val checkState = level.getBlockState(checkPos)

            if (checkState.isAir || !checkState.isSolidRender(level, checkPos)) {
                continue
            }

            if (checkState.block is MELaserConnectorBlock) {
                val connectorBlock = checkState.block as MELaserConnectorBlock
                if (connectorBlock.color == this.currentColor) {
                    val targetBe = level.getBlockEntity(checkPos) as? MELaserConnectorBlockEntity
                    if (targetBe != null && targetBe.mainNode.isReady) {
                        foundTarget = checkPos
                        foundTargetBe = targetBe
                    }
                }
                break
            } else {
                break
            }
        }

        if (foundTarget != null && foundTargetBe != null) {
            val targetNode = foundTargetBe.mainNode.node
            if (targetNode != null) {
                if (connectedTargetUp != foundTarget || activeConnectionUp == null) {
                    cleanupLaserConnection()
                    try {
                        val alreadyConnected = myNode.connections.any { it.a() == targetNode || it.b() == targetNode }
                        if (!alreadyConnected) {
                            activeConnectionUp = GridHelper.createGridConnection(myNode, targetNode)
                        }
                        connectedTargetUp = foundTarget
                        val dist = foundTarget.y - pos.y
                        mainNode.setIdlePowerUsage(AppliedFlooringConfig.laserIdleBaseEnergy + dist * AppliedFlooringConfig.laserIdleDistanceEnergy)
                        markForUpdate()
                        markForSave()
                    } catch (_: Exception) {
                        cleanupLaserConnection()
                    }
                }
                return
            }
        }

        if (connectedTargetUp != null || activeConnectionUp != null) {
            cleanupLaserConnection()
        }
    }

    private fun cleanupLaserConnection() {
        if (activeConnectionUp != null) {
            try {
                activeConnectionUp?.destroy()
            } catch (_: Exception) {
            }
            activeConnectionUp = null
        }
        if (connectedTargetUp != null) {
            connectedTargetUp = null
            mainNode.setIdlePowerUsage(0.0)
            markForUpdate()
            markForSave()
        }
    }

    override fun setRemoved() {
        cleanupLaserConnection()
        super.setRemoved()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        if (connectedTargetUp != null) {
            tag.putLong("LaserTargetUp", connectedTargetUp!!.asLong())
        }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        if (tag.contains("LaserTargetUp")) {
            connectedTargetUp = BlockPos.of(tag.getLong("LaserTargetUp"))
        } else {
            connectedTargetUp = null
        }
    }
}
