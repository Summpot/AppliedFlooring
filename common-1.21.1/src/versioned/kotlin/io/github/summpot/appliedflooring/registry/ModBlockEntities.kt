package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.blockentity.MEElevatorBlockEntity
import io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

object ModBlockEntities {
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(
        AppliedFlooringMod.MOD_ID,
        Registries.BLOCK_ENTITY_TYPE
    )

    val ME_FLOORING_BE: RegistrySupplier<BlockEntityType<MEFlooringBlockEntity>> = BLOCK_ENTITIES.register("me_flooring") {
        val validBlocks = mutableListOf<Block>(ModBlocks.ME_FLOORING.get())
        validBlocks.addAll(ModBlocks.COLORED_ME_FLOORING.values.map { it.get() })
        BlockEntityType.Builder.of(
            BlockEntityType.BlockEntitySupplier { pos: BlockPos, state: BlockState ->
                MEFlooringBlockEntity(ME_FLOORING_BE.get(), pos, state)
            },
            *validBlocks.toTypedArray()
        ).build(null)
    }

    val ME_LASER_CONNECTOR_BE: RegistrySupplier<BlockEntityType<MELaserConnectorBlockEntity>> = BLOCK_ENTITIES.register("me_laser_connector") {
        val validBlocks = mutableListOf<Block>(ModBlocks.ME_LASER_CONNECTOR.get())
        validBlocks.addAll(ModBlocks.COLORED_ME_LASER_CONNECTOR.values.map { it.get() })
        BlockEntityType.Builder.of(
            BlockEntityType.BlockEntitySupplier { pos: BlockPos, state: BlockState ->
                MELaserConnectorBlockEntity(ME_LASER_CONNECTOR_BE.get(), pos, state)
            },
            *validBlocks.toTypedArray()
        ).build(null)
    }

    val ME_ELEVATOR_BE: RegistrySupplier<BlockEntityType<MEElevatorBlockEntity>> = BLOCK_ENTITIES.register("me_elevator") {
        val validBlocks = mutableListOf<Block>(ModBlocks.ME_ELEVATOR.get())
        validBlocks.addAll(ModBlocks.COLORED_ME_ELEVATOR.values.map { it.get() })
        BlockEntityType.Builder.of(
            BlockEntityType.BlockEntitySupplier { pos: BlockPos, state: BlockState ->
                MEElevatorBlockEntity(ME_ELEVATOR_BE.get(), pos, state)
            },
            *validBlocks.toTypedArray()
        ).build(null)
    }

    fun register() {
        BLOCK_ENTITIES.register()
    }
}
