package io.github.summpot.appliedflooring.blockentity

import appeng.api.networking.GridFlags
import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IInWorldGridNodeHost
import appeng.api.networking.IManagedGridNode
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
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.EnumSet

open class MEFlooringBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    val isDenseCable: Boolean = false,
    var currentColor: AEColor = AEColor.TRANSPARENT
) : BlockEntity(type, pos, state), IInWorldGridNodeHost, IPartHost {

    val mainNode: IManagedGridNode = GridHelper.createManagedNode(this, NodeListener)
        .setFlags(GridFlags.PREFERRED)
        .setIdlePowerUsage(0.0)
        .setInWorldNode(true)
        .setExposedOnSides(EnumSet.allOf(Direction::class.java))

    init {
        mainNode.setGridColor(currentColor)
        mainNode.setTagName(if (isDenseCable) "dense_flooring" else "flooring")
    }

    object NodeListener : IGridNodeListener<MEFlooringBlockEntity> {
        override fun onInWorldConnectionChanged(nodeOwner: MEFlooringBlockEntity, node: IGridNode) {
            nodeOwner.markForUpdate()
        }

        override fun onSaveChanges(nodeOwner: MEFlooringBlockEntity, node: IGridNode) {
            nodeOwner.markForSave()
        }
    }

    open fun onEntitySteppedOn(entity: Entity) {
        // Base flooring step logic
    }

    override fun getCableConnectionType(dir: Direction?): AECableType {
        return if (isDenseCable) AECableType.DENSE_COVERED else AECableType.COVERED
    }

    override fun isBlocked(side: Direction?): Boolean {
        return false
    }

    override fun getCableConnectionLength(cableType: AECableType?): Float {
        return 0.0f
    }

    override fun getGridNode(dir: Direction?): IGridNode? {
        return mainNode.node
    }

    override fun setRemoved() {
        super.setRemoved()
        mainNode.destroy()
    }

    override fun clearRemoved() {
        super.clearRemoved()
        mainNode.create(level, worldPosition)
    }

    override fun getFacadeContainer(): IFacadeContainer? {
        return null
    }

    override fun getPart(side: Direction?): IPart? {
        return null
    }

    override fun canAddPart(part: ItemStack?, side: Direction?): Boolean {
        return true
    }

    override fun <T : IPart?> addPart(partItem: IPartItem<T>?, side: Direction?, owner: Player?): T? {
        return null
    }

    override fun <T : IPart?> replacePart(
        partItem: IPartItem<T>?,
        side: Direction?,
        owner: Player?,
        hand: InteractionHand?
    ): T? {
        return null
    }

    override fun removePartFromSide(side: Direction?) {
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
    }

    override fun selectPartLocal(pos: Vec3?): SelectedPart {
        return SelectedPart()
    }

    override fun getCollisionShape(context: CollisionContext?): VoxelShape {
        return Shapes.empty()
    }

    override fun removePart(part: IPart?): Boolean {
        return false
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
        return true
    }

    override fun cleanup() {
    }

    override fun notifyNeighbors() {
    }

    override fun notifyNeighborNow(side: Direction?) {
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
    }
}
