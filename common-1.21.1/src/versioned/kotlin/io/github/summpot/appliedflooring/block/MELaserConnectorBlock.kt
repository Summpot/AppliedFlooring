package io.github.summpot.appliedflooring.block

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

class MELaserConnectorBlock(
    properties: BlockBehaviour.Properties,
    color: AEColor = AEColor.TRANSPARENT
) : MEFlooringBlock(properties, color) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return MELaserConnectorBlockEntity(ModBlockEntities.ME_LASER_CONNECTOR_BE.get(), pos, state, currentColor = color)
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker { lvl, pos, st, be ->
            if (be is MELaserConnectorBlockEntity) {
                be.serverTick(lvl, pos, st)
            }
        }
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: net.minecraft.world.entity.player.Player,
        hit: net.minecraft.world.phys.BlockHitResult
    ): net.minecraft.world.InteractionResult {
        val be = level.getBlockEntity(pos)
        if (be is io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity) {
            val selectedPart = be.selectPartWorld(hit.location)
            if (selectedPart.part != null) {
                val activated = selectedPart.part.onUseWithoutItem(player, hit.location)
                if (activated) {
                    return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide)
                }
            }
        }

        if (!level.isClientSide && player is net.minecraft.server.level.ServerPlayer) {
            val connectorBe = be as? MELaserConnectorBlockEntity
            if (connectorBe != null) {
                dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(player, connectorBe)
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun useItemOn(
        heldItem: net.minecraft.world.item.ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: net.minecraft.world.entity.player.Player,
        hand: net.minecraft.world.InteractionHand,
        hit: net.minecraft.world.phys.BlockHitResult
    ): net.minecraft.world.ItemInteractionResult {
        // 0. Wrench interaction
        if (io.github.summpot.appliedflooring.util.FlooringWrenchHelper.isWrench(heldItem) && player.isShiftKeyDown) {
            return super.useItemOn(heldItem, state, level, pos, player, hand, hit)
        }

        // 1. Part item
        if (heldItem.item is appeng.api.parts.IPartItem<*>) {
            return super.useItemOn(heldItem, state, level, pos, player, hand, hit)
        }

        val be = level.getBlockEntity(pos)
        if (be is io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity) {
            val selectedPart = be.selectPartWorld(hit.location)
            if (selectedPart.part != null) {
                return super.useItemOn(heldItem, state, level, pos, player, hand, hit)
            }
        }

        // 2. Open GUI
        if (!level.isClientSide && player is net.minecraft.server.level.ServerPlayer) {
            val connectorBe = be as? MELaserConnectorBlockEntity
            if (connectorBe != null) {
                dev.architectury.registry.menu.MenuRegistry.openExtendedMenu(player, connectorBe)
            }
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
    }
}
