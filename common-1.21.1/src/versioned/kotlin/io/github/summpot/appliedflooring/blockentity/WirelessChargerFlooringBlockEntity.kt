package io.github.summpot.appliedflooring.blockentity

import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.implementations.items.IAEItemPowerStorage
import appeng.api.networking.energy.IEnergyService
import appeng.api.util.AEColor
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

class WirelessChargerFlooringBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlockEntity(type, pos, state, isDenseCable = false, currentColor = color) {

    private var tickCounter = 0

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
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
}
