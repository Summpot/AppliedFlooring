package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

object ModCreativeTabs {
    val TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(
        AppliedFlooringMod.MOD_ID,
        Registries.CREATIVE_MODE_TAB
    )

    val MAIN_TAB: RegistrySupplier<CreativeModeTab> = TABS.register("appliedflooring_tab") {
        CreativeTabRegistry.create { builder ->
            builder.title(Component.translatable("itemGroup.appliedflooring"))
                .icon { ItemStack(ModItems.ME_FLOORING.get()) }
                .displayItems { _, output ->
                    output.accept(ModItems.ME_FLOORING.get())
                    for (itemSupplier in ModItems.COLORED_ME_FLOORING.values) {
                        output.accept(itemSupplier.get())
                    }
                }
        }
    }

    fun register() {
        TABS.register()
    }
}
