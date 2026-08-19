package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.blockentity.DenseMEFlooringBlockEntity
import io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity
import io.github.summpot.appliedflooring.blockentity.TerminalFlooringBlockEntity
import io.github.summpot.appliedflooring.blockentity.WirelessChargerFlooringBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

object ModBlockEntities {
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(
        AppliedFlooringMod.MOD_ID,
        Registry.BLOCK_ENTITY_TYPE_REGISTRY
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

    val DENSE_ME_FLOORING_BE: RegistrySupplier<BlockEntityType<DenseMEFlooringBlockEntity>> = BLOCK_ENTITIES.register("dense_me_flooring") {
        BlockEntityType.Builder.of(
            BlockEntityType.BlockEntitySupplier { pos: BlockPos, state: BlockState ->
                DenseMEFlooringBlockEntity(DENSE_ME_FLOORING_BE.get(), pos, state)
            },
            ModBlocks.DENSE_ME_FLOORING.get()
        ).build(null)
    }

    val WIRELESS_CHARGER_FLOORING_BE: RegistrySupplier<BlockEntityType<WirelessChargerFlooringBlockEntity>> = BLOCK_ENTITIES.register("wireless_charger_flooring") {
        BlockEntityType.Builder.of(
            BlockEntityType.BlockEntitySupplier { pos: BlockPos, state: BlockState ->
                WirelessChargerFlooringBlockEntity(WIRELESS_CHARGER_FLOORING_BE.get(), pos, state)
            },
            ModBlocks.WIRELESS_CHARGER_FLOORING.get()
        ).build(null)
    }

    val TERMINAL_FLOORING_BE: RegistrySupplier<BlockEntityType<TerminalFlooringBlockEntity>> = BLOCK_ENTITIES.register("terminal_flooring") {
        BlockEntityType.Builder.of(
            BlockEntityType.BlockEntitySupplier { pos: BlockPos, state: BlockState ->
                TerminalFlooringBlockEntity(TERMINAL_FLOORING_BE.get(), pos, state)
            },
            ModBlocks.TERMINAL_FLOORING.get()
        ).build(null)
    }

    fun register() {
        BLOCK_ENTITIES.register()
    }
}
