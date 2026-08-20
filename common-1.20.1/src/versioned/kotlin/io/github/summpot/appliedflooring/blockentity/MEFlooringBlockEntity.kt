package io.github.summpot.appliedflooring.blockentity

import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.implementations.items.IAEItemPowerStorage
import appeng.api.networking.GridFlags
import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IInWorldGridNodeHost
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.energy.IEnergyService
import appeng.api.parts.IFacadeContainer
import appeng.api.parts.IPart
import appeng.api.parts.IPartHost
import appeng.api.parts.IPartItem
import appeng.api.parts.SelectedPart
import appeng.api.util.AECableType
import appeng.api.util.AEColor
import appeng.api.util.DimensionalBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.EnumSet

open class MEFlooringBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    val isDenseCable: Boolean = true,
    var currentColor: AEColor = AEColor.TRANSPARENT
) : BlockEntity(type, pos, state), IInWorldGridNodeHost, IPartHost {

    private val parts: Array<IPart?> = arrayOfNulls(6)
    private var tickCounter = 0

    val mainNode: IManagedGridNode = GridHelper.createManagedNode(this, NodeListener)
        .setFlags(GridFlags.PREFERRED)
        .setIdlePowerUsage(0.0)
        .setInWorldNode(true)
        .setExposedOnSides(EnumSet.allOf(Direction::class.java))

    init {
        mainNode.setGridColor(currentColor)
        mainNode.setTagName("flooring")
    }

    object NodeListener : IGridNodeListener<MEFlooringBlockEntity> {
        override fun onInWorldConnectionChanged(nodeOwner: MEFlooringBlockEntity, node: IGridNode) {
            nodeOwner.markForUpdate()
            nodeOwner.updatePowerState()
        }

        override fun onSaveChanges(nodeOwner: MEFlooringBlockEntity, node: IGridNode) {
            nodeOwner.markForSave()
            nodeOwner.updatePowerState()
        }
    }

    open fun onEntitySteppedOn(entity: Entity) {
    }

    fun updatePowerState() {
        val lvl = level ?: return
        if (lvl.isClientSide || isRemoved) return
        val isPowered = mainNode.isReady && (mainNode.grid?.energyService?.isNetworkPowered ?: false)
        val state = blockState
        if (state.hasProperty(io.github.summpot.appliedflooring.block.MEFlooringBlock.POWERED) &&
            state.getValue(io.github.summpot.appliedflooring.block.MEFlooringBlock.POWERED) != isPowered
        ) {
            lvl.setBlock(worldPosition, state.setValue(io.github.summpot.appliedflooring.block.MEFlooringBlock.POWERED, isPowered), net.minecraft.world.level.block.Block.UPDATE_CLIENTS)
        }
    }

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        updatePowerState()

        tickCounter++
        if (tickCounter % 10 != 0) return

        val grid = mainNode.grid ?: return
        val energyService = grid.energyService ?: return

        val checkArea = AABB(pos.above())
        val players = level.getEntitiesOfClass(Player::class.java, checkArea)
        if (players.isEmpty()) return

        for (player in players) {
            chargePlayerItems(player, energyService)
        }
    }

    private fun chargePlayerItems(player: Player, energyService: IEnergyService) {
        val maxTransferPerTick = 10000.0

        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue
            chargeItemStack(stack, energyService, maxTransferPerTick)
        }
    }

    private fun chargeItemStack(stack: ItemStack, energyService: IEnergyService, maxTransfer: Double) {
        val item = stack.item
        if (item is IAEItemPowerStorage) {
            val current = item.getAECurrentPower(stack)
            val max = item.getAEMaxPower(stack)
            val needed = max - current
            if (needed > 0.0) {
                val toExtract = minOf(needed, maxTransfer)
                val extracted = energyService.extractAEPower(toExtract, Actionable.SIMULATE, PowerMultiplier.CONFIG)
                if (extracted > 0.0) {
                    val actualExtracted = energyService.extractAEPower(extracted, Actionable.MODULATE, PowerMultiplier.CONFIG)
                    item.injectAEPower(stack, actualExtracted, Actionable.MODULATE)
                }
            }
        }
    }

    fun initNode() {
        val lvl = level
        if (lvl != null && !lvl.isClientSide && !isRemoved) {
            if (!mainNode.isReady) {
                mainNode.create(lvl, worldPosition)
            }
            for (part in parts) {
                part?.addToWorld()
            }
            updatePowerState()
        }
    }

    override fun setLevel(level: Level) {
        super.setLevel(level)
        initNode()
    }

    override fun getCableConnectionType(dir: Direction?): AECableType {
        if (dir != null) {
            val part = parts[dir.ordinal]
            if (part != null) {
                return part.externalCableConnectionType
            }
        }
        return AECableType.DENSE_SMART
    }

    override fun isBlocked(side: Direction?): Boolean {
        return false
    }

    override fun getCableConnectionLength(cableType: AECableType?): Float {
        return 0.0f
    }

    override fun getGridNode(dir: Direction?): IGridNode? {
        if (dir != null) {
            val part = parts[dir.ordinal]
            if (part != null) {
                val extNode = part.externalFacingNode
                if (extNode != null) return extNode
            }
        }
        return mainNode.node
    }

    override fun setRemoved() {
        super.setRemoved()
        mainNode.destroy()
        for (part in parts) {
            part?.removeFromWorld()
        }
    }

    override fun clearRemoved() {
        super.clearRemoved()
        initNode()
    }

    override fun getFacadeContainer(): IFacadeContainer? {
        return null
    }

    override fun getPart(side: Direction?): IPart? {
        if (side == null) return null
        return parts[side.ordinal]
    }

    override fun canAddPart(part: ItemStack?, side: Direction?): Boolean {
        if (side == null || part == null) return false
        val item = part.item
        if (item !is IPartItem<*>) return false
        val dummy = item.createPart()
        if (dummy is appeng.api.implementations.parts.ICablePart) return false
        return parts[side.ordinal] == null
    }

    override fun <T : IPart?> addPart(partItem: IPartItem<T>?, side: Direction?, owner: Player?): T? {
        if (side == null || partItem == null) return null
        if (parts[side.ordinal] != null) return null
        val part = partItem.createPart() ?: return null
        part.setPartHostInfo(side, this, this)
        if (owner != null) {
            part.onPlacement(owner)
        }
        parts[side.ordinal] = part
        val lvl = level
        if (lvl != null && !lvl.isClientSide && !isRemoved) {
            part.addToWorld()
        }
        markForUpdate()
        markForSave()
        @Suppress("UNCHECKED_CAST")
        return part as T
    }

    override fun <T : IPart?> replacePart(
        partItem: IPartItem<T>?,
        side: Direction?,
        owner: Player?,
        hand: InteractionHand?
    ): T? {
        if (side == null || partItem == null) return null
        removePartFromSide(side)
        return addPart(partItem, side, owner)
    }

    override fun removePartFromSide(side: Direction?) {
        if (side == null) return
        val p = parts[side.ordinal]
        if (p != null) {
            p.removeFromWorld()
            parts[side.ordinal] = null
            markForUpdate()
            markForSave()
        }
    }

    override fun removePart(part: IPart?): Boolean {
        if (part == null) return false
        for (i in parts.indices) {
            if (parts[i] == part) {
                part.removeFromWorld()
                parts[i] = null
                markForUpdate()
                markForSave()
                return true
            }
        }
        return false
    }

    fun addAdditionalDrops(drops: MutableList<ItemStack>, wrenched: Boolean) {
        for (part in parts) {
            if (part != null) {
                part.addPartDrop(drops, wrenched)
                part.addAdditionalDrops(drops, wrenched)
            }
        }
    }

    override fun markForUpdate() {
        val lvl = level
        if (lvl != null && !lvl.isClientSide) {
            val state = blockState
            lvl.sendBlockUpdated(worldPosition, state, state, 3)
        }
    }

    override fun getLocation(): DimensionalBlockPos {
        return DimensionalBlockPos(level, worldPosition)
    }

    override fun getBlockEntity(): BlockEntity {
        return this
    }

    override fun getColor(): AEColor {
        return currentColor
    }

    override fun clearContainer() {
        for (i in parts.indices) {
            parts[i]?.removeFromWorld()
            parts[i] = null
        }
    }

    override fun selectPartLocal(pos: Vec3?): SelectedPart {
        if (pos == null) return SelectedPart()
        val dx = pos.x - 0.5
        val dy = pos.y - 0.5
        val dz = pos.z - 0.5
        val absX = kotlin.math.abs(dx)
        val absY = kotlin.math.abs(dy)
        val absZ = kotlin.math.abs(dz)
        val side = when {
            absY >= absX && absY >= absZ -> if (dy > 0) Direction.UP else Direction.DOWN
            absX >= absY && absX >= absZ -> if (dx > 0) Direction.EAST else Direction.WEST
            else -> if (dz > 0) Direction.SOUTH else Direction.NORTH
        }
        val part = parts[side.ordinal]
        return if (part != null) SelectedPart(part, side) else SelectedPart()
    }

    override fun getCollisionShape(context: CollisionContext?): VoxelShape {
        return Shapes.empty()
    }

    override fun markForSave() {
        setChanged()
    }

    override fun partChanged() {
        markForSave()
        markForUpdate()
    }

    override fun hasRedstone(): Boolean {
        return level?.hasNeighborSignal(worldPosition) ?: false
    }

    override fun isEmpty(): Boolean {
        return parts.all { it == null }
    }

    override fun cleanup() {
    }

    override fun notifyNeighbors() {
        val lvl = level
        if (lvl != null && !lvl.isClientSide) {
            lvl.updateNeighborsAt(worldPosition, blockState.block)
        }
    }

    override fun notifyNeighborNow(side: Direction?) {
        val lvl = level
        if (lvl != null && !lvl.isClientSide) {
            lvl.updateNeighborsAt(worldPosition, blockState.block)
        }
    }

    override fun isInWorld(): Boolean {
        return level != null && !isRemoved
    }

    fun recolourBlock(side: Direction?, newColor: AEColor?, who: Player?): Boolean {
        if (newColor != null && newColor != currentColor) {
            currentColor = newColor
            mainNode.setGridColor(newColor)
            markForUpdate()
            markForSave()
            return true
        }
        return false
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        mainNode.saveToNBT(tag)
        tag.putInt("AEColor", currentColor.ordinal)

        val partsTag = CompoundTag()
        for (dir in Direction.values()) {
            val part = parts[dir.ordinal]
            if (part != null) {
                val partTag = CompoundTag()
                partTag.putString("id", IPartItem.getId(part.partItem).toString())
                part.writeToNBT(partTag)
                partsTag.put(dir.name, partTag)
            }
        }
        tag.put("AFParts", partsTag)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        mainNode.loadFromNBT(tag)
        if (tag.contains("AEColor")) {
            val idx = tag.getInt("AEColor")
            if (idx in 0 until AEColor.values().size) {
                currentColor = AEColor.values()[idx]
                mainNode.setGridColor(currentColor)
            }
        }

        if (tag.contains("AFParts")) {
            val partsTag = tag.getCompound("AFParts")
            for (dir in Direction.values()) {
                if (partsTag.contains(dir.name)) {
                    val partTag = partsTag.getCompound(dir.name)
                    val id = ResourceLocation.tryParse(partTag.getString("id"))
                    if (id != null) {
                        val partItem = IPartItem.byId(id)
                        if (partItem != null) {
                            var part = parts[dir.ordinal]
                            if (part == null || IPartItem.getId(part.partItem) != id) {
                                part?.removeFromWorld()
                                part = partItem.createPart()
                                if (part != null) {
                                    part.setPartHostInfo(dir, this, this)
                                    parts[dir.ordinal] = part
                                    if (level != null && !level!!.isClientSide && !isRemoved) {
                                        part.addToWorld()
                                    }
                                }
                            }
                            part?.readFromNBT(partTag)
                        }
                    }
                } else {
                    parts[dir.ordinal]?.removeFromWorld()
                    parts[dir.ordinal] = null
                }
            }
        }
    }

    override fun getUpdatePacket(): net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener>? {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag)
        return tag
    }

}