package io.github.summpot.appliedflooring.registry

import appeng.api.util.AEColor
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.block.DenseMEFlooringBlock
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.block.TerminalFlooringBlock
import io.github.summpot.appliedflooring.block.WirelessChargerFlooringBlock
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
    }

    val ME_FLOORING: RegistrySupplier<Block> = BLOCKS.register("me_flooring") {
        MEFlooringBlock(defaultProps(), AEColor.TRANSPARENT, false)
    }

    val DENSE_ME_FLOORING: RegistrySupplier<Block> = BLOCKS.register("dense_me_flooring") {
        DenseMEFlooringBlock(defaultProps(), AEColor.TRANSPARENT)
    }

    val WIRELESS_CHARGER_FLOORING: RegistrySupplier<Block> = BLOCKS.register("wireless_charger_flooring") {
        WirelessChargerFlooringBlock(defaultProps(), AEColor.TRANSPARENT)
    }

    val TERMINAL_FLOORING: RegistrySupplier<Block> = BLOCKS.register("terminal_flooring") {
        TerminalFlooringBlock(defaultProps(), AEColor.TRANSPARENT)
    }

    val COLORED_ME_FLOORING: Map<AEColor, RegistrySupplier<Block>> = AEColor.values()
        .filter { it != AEColor.TRANSPARENT }
        .associateWith { color ->
            val name = "${color.registryPrefix}_me_flooring"
            BLOCKS.register(name) {
                MEFlooringBlock(defaultProps(), color, false)
            }
        }

    fun register() {
        BLOCKS.register()
    }
}
