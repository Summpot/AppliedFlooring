package io.github.summpot.appliedflooring.registry

import appeng.api.util.AEColor
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material

object ModBlocks {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(AppliedFlooringMod.MOD_ID, Registry.BLOCK_REGISTRY)

    private fun defaultProps(): BlockBehaviour.Properties {
        return BlockBehaviour.Properties.of(Material.STONE)
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .lightLevel { state -> if (state.getValue(MEFlooringBlock.POWERED)) 7 else 0 }
    }

    val ME_FLOORING: RegistrySupplier<Block> = BLOCKS.register("me_flooring") {
        MEFlooringBlock(defaultProps(), AEColor.TRANSPARENT)
    }

    val COLORED_ME_FLOORING: Map<AEColor, RegistrySupplier<Block>> = AEColor.values()
        .filter { it != AEColor.TRANSPARENT }
        .associateWith { color ->
            val name = "${color.registryPrefix}_me_flooring"
            BLOCKS.register(name) {
                MEFlooringBlock(defaultProps(), color)
            }
        }

    fun register() {
        BLOCKS.register()
    }
}
