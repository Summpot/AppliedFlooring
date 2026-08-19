package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item

object ModItems {
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(AppliedFlooringMod.MOD_ID, Registries.ITEM)

    val ME_FLOORING: RegistrySupplier<Item> = ITEMS.register("me_flooring") {
        BlockItem(ModBlocks.ME_FLOORING.get(), Item.Properties())
    }

    val DENSE_ME_FLOORING: RegistrySupplier<Item> = ITEMS.register("dense_me_flooring") {
        BlockItem(ModBlocks.DENSE_ME_FLOORING.get(), Item.Properties())
    }

    val WIRELESS_CHARGER_FLOORING: RegistrySupplier<Item> = ITEMS.register("wireless_charger_flooring") {
        BlockItem(ModBlocks.WIRELESS_CHARGER_FLOORING.get(), Item.Properties())
    }

    val TERMINAL_FLOORING: RegistrySupplier<Item> = ITEMS.register("terminal_flooring") {
        BlockItem(ModBlocks.TERMINAL_FLOORING.get(), Item.Properties())
    }

    val COLORED_ME_FLOORING: Map<appeng.api.util.AEColor, RegistrySupplier<Item>> = ModBlocks.COLORED_ME_FLOORING
        .mapValues { (color, blockSupplier) ->
            val name = "${color.registryPrefix}_me_flooring"
            ITEMS.register(name) {
                BlockItem(blockSupplier.get(), Item.Properties())
            }
        }

    fun register() {
        ITEMS.register()
    }
}
