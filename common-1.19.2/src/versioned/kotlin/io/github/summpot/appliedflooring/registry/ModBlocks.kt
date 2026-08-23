package io.github.summpot.appliedflooring.registry

import appeng.api.util.AEColor
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.block.MEElevatorBlock
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.block.MELaserConnectorBlock
import net.minecraft.core.Registry
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

object ModBlocks {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(AppliedFlooringMod.MOD_ID, Registry.BLOCK_REGISTRY)

    private fun defaultProps(): BlockBehaviour.Properties {
        return BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .lightLevel { state -> if (state.getValue(MEFlooringBlock.POWERED)) 7 else 0 }
    }

    val ME_FLOORING: RegistrySupplier<Block> = BLOCKS.register("me_flooring") {
        MEFlooringBlock(defaultProps(), AEColor.TRANSPARENT) as Block
    }

    val COLORED_ME_FLOORING: Map<AEColor, RegistrySupplier<Block>> = AEColor.values()
        .filter { it != AEColor.TRANSPARENT }
        .associateWith { color ->
            val name = "${color.registryPrefix}_me_flooring"
            BLOCKS.register(name) {
                MEFlooringBlock(defaultProps(), color) as Block
            }
        }

    val ME_LASER_CONNECTOR: RegistrySupplier<Block> = BLOCKS.register("me_laser_connector") {
        MELaserConnectorBlock(defaultProps(), AEColor.TRANSPARENT) as Block
    }

    val COLORED_ME_LASER_CONNECTOR: Map<AEColor, RegistrySupplier<Block>> = AEColor.values()
        .filter { it != AEColor.TRANSPARENT }
        .associateWith { color ->
            val name = "${color.registryPrefix}_me_laser_connector"
            BLOCKS.register(name) {
                MELaserConnectorBlock(defaultProps(), color) as Block
            }
        }

    val ME_ELEVATOR: RegistrySupplier<Block> = BLOCKS.register("me_elevator") {
        MEElevatorBlock(defaultProps(), AEColor.TRANSPARENT) as Block
    }

    val COLORED_ME_ELEVATOR: Map<AEColor, RegistrySupplier<Block>> = AEColor.values()
        .filter { it != AEColor.TRANSPARENT }
        .associateWith { color ->
            val name = "${color.registryPrefix}_me_elevator"
            BLOCKS.register(name) {
                MEElevatorBlock(defaultProps(), color) as Block
            }
        }

    fun register() {
        BLOCKS.register()
    }
}
