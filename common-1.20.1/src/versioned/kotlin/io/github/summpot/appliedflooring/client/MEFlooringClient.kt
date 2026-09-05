package io.github.summpot.appliedflooring.client

import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import dev.architectury.registry.menu.MenuRegistry
import io.github.summpot.appliedflooring.client.gui.MELaserConnectorScreen
import io.github.summpot.appliedflooring.client.render.MEElevatorRenderer
import io.github.summpot.appliedflooring.client.render.MEFlooringBlockEntityRenderer
import io.github.summpot.appliedflooring.client.render.MELaserConnectorRenderer
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import io.github.summpot.appliedflooring.registry.ModMenus
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

object MEFlooringClient {
    fun init() {
        io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper.bakeryService = io.github.summpot.appliedflooring.client.model.FaceBakeryService1201()

        ModMenus.ME_LASER_CONNECTOR_MENU.listen { menuType ->
            MenuRegistry.registerScreenFactory(menuType) { menu, inv, title ->
                MELaserConnectorScreen(menu, inv, title)
            }
        }

        ClientLifecycleEvent.CLIENT_SETUP.register {
            BlockEntityRendererRegistry.register(
                ModBlockEntities.ME_FLOORING_BE.get(),
                BlockEntityRendererProvider { context -> MEFlooringBlockEntityRenderer(context) }
            )
            BlockEntityRendererRegistry.register(
                ModBlockEntities.ME_LASER_CONNECTOR_BE.get(),
                BlockEntityRendererProvider { context -> MELaserConnectorRenderer(context) }
            )
            BlockEntityRendererRegistry.register(
                ModBlockEntities.ME_ELEVATOR_BE.get(),
                BlockEntityRendererProvider { context -> MEElevatorRenderer(context) }
            )
        }

        registerColors()
    }

    private fun registerColors() {
        val blockColor = net.minecraft.client.color.block.BlockColor { state, level, pos, tintIndex ->
            if (tintIndex == 0) {
                val be = if (level != null && pos != null) level.getBlockEntity(pos) else null
                val color = if (be is appeng.api.implementations.blockentities.IColorableBlockEntity) {
                    be.color
                } else {
                    (state.block as? io.github.summpot.appliedflooring.block.MEFlooringBlock)?.color ?: appeng.api.util.AEColor.TRANSPARENT
                }
                color.mediumVariant
            } else {
                -1
            }
        }

        val blocks = mutableListOf<java.util.function.Supplier<out net.minecraft.world.level.block.Block>>()
        blocks.add(io.github.summpot.appliedflooring.registry.ModBlocks.ME_FLOORING)
        blocks.addAll(io.github.summpot.appliedflooring.registry.ModBlocks.COLORED_ME_FLOORING.values)
        blocks.add(io.github.summpot.appliedflooring.registry.ModBlocks.ME_ELEVATOR)
        blocks.addAll(io.github.summpot.appliedflooring.registry.ModBlocks.COLORED_ME_ELEVATOR.values)
        blocks.add(io.github.summpot.appliedflooring.registry.ModBlocks.ME_LASER_CONNECTOR)
        blocks.addAll(io.github.summpot.appliedflooring.registry.ModBlocks.COLORED_ME_LASER_CONNECTOR.values)

        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerBlockColors(blockColor, *blocks.toTypedArray())

        val itemColor = net.minecraft.client.color.item.ItemColor { stack, tintIndex ->
            if (tintIndex == 0) {
                val block = net.minecraft.world.level.block.Block.byItem(stack.item)
                val color = (block as? io.github.summpot.appliedflooring.block.MEFlooringBlock)?.color ?: appeng.api.util.AEColor.TRANSPARENT
                color.mediumVariant
            } else {
                -1
            }
        }

        val items = mutableListOf<java.util.function.Supplier<out net.minecraft.world.level.ItemLike>>()
        items.add(io.github.summpot.appliedflooring.registry.ModItems.ME_FLOORING)
        items.addAll(io.github.summpot.appliedflooring.registry.ModItems.COLORED_ME_FLOORING.values)
        items.add(io.github.summpot.appliedflooring.registry.ModItems.ME_ELEVATOR)
        items.addAll(io.github.summpot.appliedflooring.registry.ModItems.COLORED_ME_ELEVATOR.values)
        items.add(io.github.summpot.appliedflooring.registry.ModItems.ME_LASER_CONNECTOR)
        items.addAll(io.github.summpot.appliedflooring.registry.ModItems.COLORED_ME_LASER_CONNECTOR.values)

        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(itemColor, *items.toTypedArray())
    }
}