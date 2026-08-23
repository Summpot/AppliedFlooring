package io.github.summpot.appliedflooring.client

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import io.github.summpot.appliedflooring.client.render.MEElevatorRenderer
import io.github.summpot.appliedflooring.client.render.MEFlooringBlockEntityRenderer
import io.github.summpot.appliedflooring.client.render.MELaserConnectorRenderer
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

object MEFlooringClient {
    fun init() {
        BlockEntityRendererRegistry.register(ModBlockEntities.ME_FLOORING_BE.get(), BlockEntityRendererProvider { context -> MEFlooringBlockEntityRenderer(context) })
        BlockEntityRendererRegistry.register(ModBlockEntities.ME_LASER_CONNECTOR_BE.get(), BlockEntityRendererProvider { context -> MELaserConnectorRenderer(context) })
        BlockEntityRendererRegistry.register(ModBlockEntities.ME_ELEVATOR_BE.get(), BlockEntityRendererProvider { context -> MEElevatorRenderer(context) })
    }
}