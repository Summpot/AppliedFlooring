package io.github.summpot.appliedflooring.menu

import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.registry.ModMenus
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.item.ItemStack

class MELaserConnectorMenu(
    containerId: Int,
    val playerInventory: Inventory,
    val blockPos: BlockPos,
    val blockEntity: MELaserConnectorBlockEntity?
) : AbstractContainerMenu(ModMenus.ME_LASER_CONNECTOR_MENU.get(), containerId) {

    var isPowered: Boolean = false

    private val powerDataSlot = object : DataSlot() {
        override fun get(): Int = if (blockEntity?.isPowered() == true) 1 else 0
        override fun set(value: Int) {
            isPowered = value != 0
            blockEntity?.clientPowered = isPowered
        }
    }

    init {
        addDataSlot(powerDataSlot)
    }

    constructor(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf) : this(
        containerId,
        playerInventory,
        buf.readBlockPos().let { pos ->
            pos to (playerInventory.player.level().getBlockEntity(pos) as? MELaserConnectorBlockEntity)
        }
    ) {
        if (buf.readableBytes() > 0) {
            val initPowered = buf.readBoolean()
            this.isPowered = initPowered
            blockEntity?.clientPowered = initPowered
        }
    }

    private constructor(containerId: Int, playerInventory: Inventory, pair: Pair<BlockPos, MELaserConnectorBlockEntity?>) : this(
        containerId,
        playerInventory,
        pair.first,
        pair.second
    )

    override fun stillValid(player: Player): Boolean {
        return blockEntity != null && !blockEntity.isRemoved &&
                player.distanceToSqr(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5) <= 64.0
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }
}
