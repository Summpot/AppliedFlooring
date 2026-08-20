package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity
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

    fun register() {
        BLOCK_ENTITIES.register()
    }
}
