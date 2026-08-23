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
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
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
) : BlockEntity(type, pos, state), IInWorldGridNodeHost, IPartHost, appeng.api.implementations.blockentities.IColorableBlockEntity {

    private val parts: Array<IPart?> = arrayOfNulls(6)
    private var tickCounter = 0

    val mainNode: IManagedGridNode = GridHelper.createManagedNode(this, NodeListener).apply {
        if (isDenseCable) {
            setFlags(GridFlags.PREFERRED, GridFlags.DENSE_CAPACITY)
        } else {
            setFlags(GridFlags.PREFERRED)
        }
        setIdlePowerUsage(0.0)
        setInWorldNode(true)
        setExposedOnSides(EnumSet.allOf(Direction::class.java))
    }

    init {
        mainNode.setGridColor(currentColor)
        mainNode.setTagName("flooring")
    }

    private fun connectPartToGrid(part: IPart) {
        val main = mainNode.node ?: return
        val partNode = part.gridNode ?: return
        val alreadyConnected = main.connections.any { it.a() == partNode || it.b() == partNode }
        if (!alreadyConnected) {
            GridHelper.createConnection(main, partNode)
        }
    }

    object NodeListener : IGridNodeListener<MEFlooringBlockEntity> {
        override fun onInWorldConnectionChanged(nodeOwner: MEFlooringBlockEntity, node: IGridNode) {
            nodeOwner.markForUpdate()
        }

        override fun onSaveChanges(nodeOwner: MEFlooringBlockEntity, node: IGridNode) {
            nodeOwner.markForSave()
        }

        override fun onStateChanged(nodeOwner: MEFlooringBlockEntity, node: IGridNode, state: IGridNodeListener.State) {
            if (state == IGridNodeListener.State.POWER) {
                nodeOwner.markForUpdate()
            }
        }
    }

    open fun onEntitySteppedOn(entity: Entity) {
    }

    fun isPowered(): Boolean {
        if (!mainNode.isReady) return false
        val grid = mainNode.grid ?: return false
        return grid.energyService?.isNetworkPowered ?: false
    }

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        tickCounter++
        if (tickCounter % 10 != 0) return

        val grid = mainNode.grid ?: return
        val energyService = grid.energyService ?: return
        if (!energyService.isNetworkPowered) return

        val checkArea = AABB(pos.above())
        val players = level.getEntitiesOfClass(Player::class.java, checkArea)
        if (players.isEmpty()) return

        for (player in players) {
            chargePlayerItems(player, energyService)
        }
    }

    private fun chargePlayerItems(player: Player, energyService: IEnergyService) {
        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue
            chargeItemStack(stack, energyService)
        }
    }

    private fun chargeItemStack(stack: ItemStack, energyService: IEnergyService) {
        val item = stack.item
        if (item is IAEItemPowerStorage) {
            if (item.getPowerFlow(stack) == appeng.api.config.AccessRestriction.READ) return
            val maxPower = item.getAEMaxPower(stack)
            if (maxPower <= 0.0) return
            val currentPower = item.getAECurrentPower(stack)
            val needed = maxPower - currentPower
            if (needed > 0.0) {
                val rate = maxOf(item.getChargeRate(stack) * 10.0, 10000.0)
                val toExtract = minOf(needed, rate)
                val extracted = energyService.extractAEPower(toExtract, Actionable.MODULATE, PowerMultiplier.ONE)
                if (extracted > 0.0) {
                    val notStored = item.injectAEPower(stack, extracted, Actionable.MODULATE)
                    if (notStored > 0.0) {
                        energyService.injectPower(notStored, Actionable.MODULATE)
                    }
                }
            }
        }
    }

    fun onReady() {
        val lvl = level
        if (lvl != null && !lvl.isClientSide && !isRemoved) {
            if (!mainNode.isReady) {
                mainNode.create(lvl, worldPosition)
            }
            for (dir in Direction.values()) {
                val part = parts[dir.ordinal]
                if (part != null) {
                    part.setPartHostInfo(dir, this, this)
                    part.addToWorld()
                    connectPartToGrid(part)
                }
            }
            markForUpdate()
        }
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
        GridHelper.onFirstTick(this) { it.onReady() }
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
            connectPartToGrid(part)
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
        val lvl = level ?: return
        if (lvl.isClientSide || isRemoved) return
        val isPowered = isPowered()
        val state = blockState
        if (state.hasProperty(io.github.summpot.appliedflooring.block.MEFlooringBlock.POWERED) &&
            state.getValue(io.github.summpot.appliedflooring.block.MEFlooringBlock.POWERED) != isPowered
        ) {
            lvl.setBlock(worldPosition, state.setValue(io.github.summpot.appliedflooring.block.MEFlooringBlock.POWERED, isPowered), net.minecraft.world.level.block.Block.UPDATE_CLIENTS)
        } else {
            lvl.sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS)
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
        val part = parts[side.ordinal] ?: return SelectedPart()
        val (u, v) = when (side.axis) {
            Direction.Axis.Y -> Pair(dx, dz)
            Direction.Axis.Z -> Pair(dx, dy)
            Direction.Axis.X -> Pair(dz, dy)
        }
        val isHittingPart = kotlin.math.abs(u) <= 0.35 && kotlin.math.abs(v) <= 0.35
        return if (isHittingPart) SelectedPart(part, side) else SelectedPart()
    }

    override fun getCollisionShape(context: CollisionContext?): VoxelShape {
        return Shapes.empty()
    }

    override fun markForSave() {
        setChanged()
    }

    fun updateConnections() {
        val sides = EnumSet.allOf(Direction::class.java)
        for (s in Direction.values()) {
            if (parts[s.ordinal] != null || isBlocked(s)) {
                sides.remove(s)
            }
        }
        mainNode.setExposedOnSides(sides)
    }

    override fun partChanged() {
        updateConnections()
        markForSave()
        markForUpdate()
        notifyNeighbors()
    }

    fun isProvidingStrongPower(side: Direction): Int {
        val part = parts[side.ordinal]
        return part?.isProvidingStrongPower ?: 0
    }

    fun isProvidingWeakPower(side: Direction): Int {
        val part = parts[side.ordinal]
        return part?.isProvidingWeakPower ?: 0
    }

    fun canConnectRedstone(opposite: Direction): Boolean {
        val part = parts[opposite.ordinal]
        return part?.canConnectRedstone() ?: false
    }

    fun onNeighborChanged(level: BlockGetter, pos: BlockPos, neighbor: BlockPos) {
        for (part in parts) {
            part?.onNeighborChanged(level, pos, neighbor)
        }
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

    fun disassembleWithWrench(
        player: Player,
        level: Level,
        hitPos: Vec3,
        wrench: ItemStack
    ): net.minecraft.world.InteractionResult {
        if (!level.isClientSide) {
            val sp = selectPartWorld(hitPos)
            if (sp.part != null) {
                // 1. Dismantle clicked Part only
                val drops = mutableListOf<ItemStack>()
                sp.part.addPartDrop(drops, true)
                sp.part.addAdditionalDrops(drops, true)
                removePartFromSide(sp.side)
                for (item in drops) {
                    player.inventory.placeItemBackInInventory(item)
                }
            } else {
                // 2. Dismantle floor tile only, preserving parts in world via CableBus
                val existingParts = mutableListOf<Pair<Direction, IPartItem<*>>>()
                for (dir in Direction.values()) {
                    val p = parts[dir.ordinal]
                    if (p != null) {
                        existingParts.add(Pair(dir, p.partItem))
                    }
                }

                player.inventory.placeItemBackInInventory(ItemStack(blockState.block))
                clearContainer()

                val pos = worldPosition
                val state = blockState
                val block = state.block
                block.playerWillDestroy(level, pos, state, player)

                if (existingParts.isEmpty()) {
                    level.removeBlock(pos, false)
                    block.destroy(level, pos, state)
                } else {
                    level.removeBlock(pos, false)
                    val host = appeng.api.parts.PartHelper.getOrPlacePartHost(level, pos, true, player)
                    if (host != null) {
                        for ((dir, pItem) in existingParts) {
                            host.addPart(pItem, dir, player)
                        }
                    }
                }
            }
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.0f)
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun recolourBlock(side: Direction?, newColor: AEColor?, who: Player?): Boolean {
        if (newColor != null && newColor != currentColor) {
            currentColor = newColor
            mainNode.setGridColor(newColor)
            markForUpdate()
            markForSave()
            return true
        }
        return false
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        mainNode.saveToNBT(tag)
        tag.putInt("AEColor", currentColor.ordinal)

        val partsTag = CompoundTag()
        for (dir in Direction.values()) {
            val part = parts[dir.ordinal]
            if (part != null) {
                val partTag = CompoundTag()
                partTag.putString("id", IPartItem.getId(part.partItem).toString())
                part.writeToNBT(partTag, registries)
                partsTag.put(dir.name, partTag)
            }
        }
        tag.put("AFParts", partsTag)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
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
                                        connectPartToGrid(part)
                                    }
                                }
                            }
                            part?.readFromNBT(partTag, registries)
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

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

}