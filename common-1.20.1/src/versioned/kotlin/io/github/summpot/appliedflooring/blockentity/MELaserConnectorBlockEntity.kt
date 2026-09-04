package io.github.summpot.appliedflooring.blockentity

import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.networking.GridHelper
import appeng.api.networking.IGridConnection
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEItemKey
import appeng.api.util.AEColor
import dev.architectury.registry.menu.ExtendedMenuProvider
import io.github.summpot.appliedflooring.block.MELaserConnectorBlock
import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import io.github.summpot.appliedflooring.menu.MELaserConnectorMenu
import io.github.summpot.appliedflooring.registry.ModBlocks
import io.github.summpot.appliedflooring.registry.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class MELaserConnectorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    isDenseCable: Boolean = true,
    currentColor: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlockEntity(type, pos, state, isDenseCable, currentColor), ExtendedMenuProvider {

    var connectedTargetUp: BlockPos? = null
        private set

    private var activeConnectionUp: IGridConnection? = null
    private var laserScanTicks = 0

    // Auto-Floor Builder configuration and state
    var targetYOffset: Int = 4
    var floorShape: FloorShape = FloorShape.SQUARE
    var radiusX: Int = 3
    var radiusZ: Int = 3
    var floorColor: AEColor = currentColor
    var replaceMode: ReplaceMode = ReplaceMode.AIR_ONLY
    var builderStatus: BuilderStatus = BuilderStatus.IDLE
    var placedBlocksCount: Int = 0
    var totalBlocksCount: Int = 0
    var progressIndex: Int = 0

    var currentPlacingPos: BlockPos? = null
        private set

    private var plannedPositions: List<BlockPos> = emptyList()
    private var builderTickCounter = 0

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return MELaserConnectorMenu(containerId, playerInventory, worldPosition, this)
    }

    override fun getDisplayName(): Component {
        return Component.translatable("gui.appliedflooring.laser_connector.title")
    }

    override fun saveExtraData(buf: FriendlyByteBuf) {
        buf.writeBlockPos(worldPosition)
        buf.writeBoolean(isPowered())
    }

    override fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        super.serverTick(level, pos, state)

        laserScanTicks++
        if (laserScanTicks % 10 == 0) {
            if (!mainNode.isReady || isRemoved) {
                cleanupLaserConnection()
            } else {
                updateLaserConnection(level, pos)
            }
        }

        if (builderStatus == BuilderStatus.BUILDING) {
            tickAutoBuilder(level, pos)
        }
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
                            activeConnectionUp = GridHelper.createConnection(myNode, targetNode)
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

    // --- Remote Connector Placement ---

    fun handleSetTargetOffset(dy: Int) {
        val maxDist = AppliedFlooringConfig.laserMaxDistance
        targetYOffset = dy.coerceIn(2, maxDist)
        markForUpdate()
        markForSave()
    }

    fun handlePlaceRemoteConnector(player: ServerPlayer): Boolean {
        val lvl = level ?: return false
        if (lvl.isClientSide) return false

        val targetPos = worldPosition.above(targetYOffset)
        if (targetPos.y >= lvl.maxBuildHeight || targetPos.y < lvl.minBuildHeight) {
            builderStatus = BuilderStatus.BLOCKED
            markForUpdate()
            return false
        }

        val targetState = lvl.getBlockState(targetPos)
        if (!targetState.isAir && !targetState.canBeReplaced()) {
            builderStatus = BuilderStatus.BLOCKED
            markForUpdate()
            return false
        }

        // Check & extract AE power
        val energyCost = AppliedFlooringConfig.laserRemotePlaceEnergy
        if (!player.isCreative && (!isPowered() || !extractEnergy(energyCost))) {
            builderStatus = BuilderStatus.NO_POWER
            markForUpdate()
            return false
        }

        // Check & extract 1x laser connector item
        if (!player.isCreative && !extractConnectorItem(player)) {
            builderStatus = BuilderStatus.NO_ITEMS
            markForUpdate()
            return false
        }

        // Place target laser connector matching source color
        val blockToPlace = if (currentColor == AEColor.TRANSPARENT) {
            ModBlocks.ME_LASER_CONNECTOR.get()
        } else {
            ModBlocks.COLORED_ME_LASER_CONNECTOR[currentColor]?.get() ?: ModBlocks.ME_LASER_CONNECTOR.get()
        }

        lvl.setBlock(targetPos, blockToPlace.defaultBlockState(), Block.UPDATE_ALL)

        // Visual & Audio effects
        lvl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.2f)
        lvl.playSound(null, targetPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.5f)

        if (lvl is ServerLevel) {
            for (dy in 1..targetYOffset) {
                lvl.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    worldPosition.x + 0.5,
                    worldPosition.y + dy + 0.5,
                    worldPosition.z + 0.5,
                    3, 0.1, 0.2, 0.1, 0.05
                )
            }
        }

        updateLaserConnection(lvl, worldPosition)
        builderStatus = BuilderStatus.IDLE
        markForUpdate()
        markForSave()
        return true
    }

    private fun extractConnectorItem(player: ServerPlayer): Boolean {
        // 1. Check player inventory first
        val coloredSupplier = ModBlocks.COLORED_ME_LASER_CONNECTOR[currentColor]
        val coloredItem = coloredSupplier?.get()?.asItem()
        val defaultItem = ModBlocks.ME_LASER_CONNECTOR.get().asItem()

        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (!stack.isEmpty && (stack.item == coloredItem || stack.item == defaultItem)) {
                stack.shrink(1)
                return true
            }
        }

        // 2. Check ME network storage
        val grid = mainNode.grid
        val storage = grid?.storageService?.inventory
        if (storage != null) {
            if (coloredItem != null) {
                val extracted = storage.extract(
                    AEItemKey.of(coloredItem),
                    1,
                    Actionable.MODULATE,
                    IActionSource.empty()
                )
                if (extracted > 0) return true
            }
            val extractedDef = storage.extract(
                AEItemKey.of(defaultItem),
                1,
                Actionable.MODULATE,
                IActionSource.empty()
            )
            if (extractedDef > 0) return true
        }

        return false
    }

    // --- Floor Builder Controls ---

    fun handleSetShape(shape: FloorShape) {
        floorShape = shape
        recalculatePlan()
        markForUpdate()
        markForSave()
    }

    fun handleSetRadius(rx: Int, rz: Int) {
        val maxR = AppliedFlooringConfig.laserBuilderMaxRadius
        radiusX = rx.coerceIn(1, maxR)
        radiusZ = rz.coerceIn(1, maxR)
        recalculatePlan()
        markForUpdate()
        markForSave()
    }

    fun handleSetColor(color: AEColor) {
        floorColor = color
        markForUpdate()
        markForSave()
    }

    fun handleSetReplaceMode(mode: ReplaceMode) {
        replaceMode = mode
        markForUpdate()
        markForSave()
    }

    fun handleStartBuild() {
        if (plannedPositions.isEmpty() || progressIndex >= plannedPositions.size) {
            recalculatePlan()
        }
        builderStatus = BuilderStatus.BUILDING
        markForUpdate()
        markForSave()
    }

    fun handlePauseBuild() {
        if (builderStatus == BuilderStatus.BUILDING) {
            builderStatus = BuilderStatus.PAUSED
            markForUpdate()
            markForSave()
        }
    }

    fun handleResetBuild() {
        builderStatus = BuilderStatus.IDLE
        progressIndex = 0
        placedBlocksCount = 0
        currentPlacingPos = null
        recalculatePlan()
        markForUpdate()
        markForSave()
    }

    private fun recalculatePlan() {
        plannedPositions = LaserFloorPlan.generatePositions(radiusX, radiusZ, floorShape)
        totalBlocksCount = plannedPositions.size
        progressIndex = 0
        placedBlocksCount = 0
    }

    // --- Auto Builder Ticking Engine ---

    private fun tickAutoBuilder(level: Level, pos: BlockPos) {
        builderTickCounter++
        if (builderTickCounter % AppliedFlooringConfig.laserBuilderTickInterval != 0) return

        if (plannedPositions.isEmpty()) {
            recalculatePlan()
            if (plannedPositions.isEmpty()) {
                builderStatus = BuilderStatus.DONE
                markForUpdate()
                return
            }
        }

        val targetY = if (connectedTargetUp != null) connectedTargetUp!!.y else pos.y + targetYOffset

        var blocksPlacedThisTick = 0
        val maxPerTick = AppliedFlooringConfig.laserBuilderBlocksPerTick

        while (progressIndex < plannedPositions.size && blocksPlacedThisTick < maxPerTick) {
            val rel = plannedPositions[progressIndex]
            val targetBlockPos = BlockPos(pos.x + rel.x, targetY, pos.z + rel.z)
            val currentState = level.getBlockState(targetBlockPos)

            // Skip if block is already flooring
            if (currentState.block is io.github.summpot.appliedflooring.block.MEFlooringBlock) {
                progressIndex++
                continue
            }

            // Check replacement rules
            if (replaceMode == ReplaceMode.AIR_ONLY) {
                if (!currentState.isAir && !currentState.canBeReplaced()) {
                    progressIndex++
                    continue
                }
            } else if (replaceMode == ReplaceMode.REPLACE_ALL) {
                if (currentState.getDestroySpeed(level, targetBlockPos) < 0) {
                    // Unbreakable (bedrock, etc.) - skip
                    progressIndex++
                    continue
                }
            }

            // Check & extract AE energy
            val energyCost = AppliedFlooringConfig.laserBuilderEnergyPerBlock
            if (!extractEnergy(energyCost)) {
                builderStatus = BuilderStatus.NO_POWER
                markForUpdate()
                return
            }

            // Check & extract floor item
            if (!extractFlooringItem(floorColor)) {
                builderStatus = BuilderStatus.NO_ITEMS
                markForUpdate()
                return
            }

            // Place floor block
            val blockToPlace = if (floorColor == AEColor.TRANSPARENT) {
                ModBlocks.ME_FLOORING.get()
            } else {
                ModBlocks.COLORED_ME_FLOORING[floorColor]?.get() ?: ModBlocks.ME_FLOORING.get()
            }

            level.setBlock(targetBlockPos, blockToPlace.defaultBlockState(), Block.UPDATE_ALL)
            currentPlacingPos = targetBlockPos
            placedBlocksCount++
            progressIndex++
            blocksPlacedThisTick++

            // Audio & particles
            level.playSound(null, targetBlockPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f)
            if (level is ServerLevel) {
                level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    targetBlockPos.x + 0.5,
                    targetBlockPos.y + 0.5,
                    targetBlockPos.z + 0.5,
                    3, 0.2, 0.2, 0.2, 0.05
                )
            }
        }

        markForUpdate()
        markForSave()

        if (progressIndex >= plannedPositions.size) {
            builderStatus = BuilderStatus.DONE
            currentPlacingPos = null
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.6f)
            markForUpdate()
            markForSave()
        }
    }

    private fun extractEnergy(amount: Double): Boolean {
        val grid = mainNode.grid ?: return false
        val energyService = grid.energyService ?: return false
        val simulated = energyService.extractAEPower(amount, Actionable.SIMULATE, PowerMultiplier.ONE)
        if (simulated >= amount) {
            energyService.extractAEPower(amount, Actionable.MODULATE, PowerMultiplier.ONE)
            return true
        }
        return false
    }

    private fun extractFlooringItem(color: AEColor): Boolean {
        val grid = mainNode.grid ?: return false
        val storage = grid.storageService?.inventory ?: return false

        // 1. Try specified color
        val targetBlock = if (color != AEColor.TRANSPARENT) {
            ModBlocks.COLORED_ME_FLOORING[color]?.get()
        } else {
            null
        }

        if (targetBlock != null) {
            val key = AEItemKey.of(targetBlock.asItem())
            val ext = storage.extract(key, 1, Actionable.MODULATE, IActionSource.empty())
            if (ext > 0) return true
        }

        // 2. Fallback to uncolored ME Flooring
        val defKey = AEItemKey.of(ModBlocks.ME_FLOORING.get().asItem())
        val extDef = storage.extract(defKey, 1, Actionable.MODULATE, IActionSource.empty())
        return extDef > 0
    }

    override fun setRemoved() {
        cleanupLaserConnection()
        builderStatus = BuilderStatus.IDLE
        super.setRemoved()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        if (connectedTargetUp != null) {
            tag.putLong("LaserTargetUp", connectedTargetUp!!.asLong())
        }

        val builderTag = CompoundTag()
        builderTag.putInt("TargetYOffset", targetYOffset)
        builderTag.putInt("Shape", floorShape.id)
        builderTag.putInt("RadiusX", radiusX)
        builderTag.putInt("RadiusZ", radiusZ)
        builderTag.putInt("Color", floorColor.ordinal)
        builderTag.putInt("ReplaceMode", replaceMode.id)
        builderTag.putInt("Status", builderStatus.id)
        builderTag.putInt("ProgressIndex", progressIndex)
        builderTag.putInt("TotalBlocks", totalBlocksCount)
        builderTag.putInt("PlacedBlocks", placedBlocksCount)
        tag.put("AFBuilder", builderTag)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        if (tag.contains("LaserTargetUp")) {
            connectedTargetUp = BlockPos.of(tag.getLong("LaserTargetUp"))
        } else {
            connectedTargetUp = null
        }

        if (tag.contains("AFBuilder")) {
            val builderTag = tag.getCompound("AFBuilder")
            targetYOffset = builderTag.getInt("TargetYOffset").coerceIn(2, 64)
            floorShape = FloorShape.byId(builderTag.getInt("Shape"))
            radiusX = builderTag.getInt("RadiusX").coerceIn(1, 32)
            radiusZ = builderTag.getInt("RadiusZ").coerceIn(1, 32)
            val colorIdx = builderTag.getInt("Color")
            if (colorIdx in AEColor.values().indices) {
                floorColor = AEColor.values()[colorIdx]
            }
            replaceMode = ReplaceMode.byId(builderTag.getInt("ReplaceMode"))
            builderStatus = BuilderStatus.byId(builderTag.getInt("Status"))
            progressIndex = builderTag.getInt("ProgressIndex")
            totalBlocksCount = builderTag.getInt("TotalBlocks")
            placedBlocksCount = builderTag.getInt("PlacedBlocks")
        }
    }

    companion object {
        val INFINITE_AABB = AABB(
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY
        )
    }

    fun getRenderBoundingBox(): AABB {
        return INFINITE_AABB
    }
}
