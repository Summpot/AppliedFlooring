package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item

object ModItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(AppliedFlooringMod.MOD_ID, Registry.ITEM_REGISTRY)

    val ME_FLOORING: RegistrySupplier<Item> = ITEMS.register("me_flooring") {
        BlockItem(ModBlocks.ME_FLOORING.get(), Item.Properties().tab(ModCreativeTabs.TAB))
    }

    val COLORED_ME_FLOORING: Map<appeng.api.util.AEColor, RegistrySupplier<Item>> = ModBlocks.COLORED_ME_FLOORING
        .mapValues { (color, blockSupplier) ->
            val name = "${color.registryPrefix}_me_flooring"
            ITEMS.register(name) {
                BlockItem(blockSupplier.get(), Item.Properties().tab(ModCreativeTabs.TAB))
            }
        }

    fun register() {
        ITEMS.register()
    }
}
