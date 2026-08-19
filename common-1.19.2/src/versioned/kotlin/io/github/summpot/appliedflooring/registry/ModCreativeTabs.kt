package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.CreativeTabRegistry
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

object ModCreativeTabs {
    val TAB: CreativeModeTab = CreativeTabRegistry.create(
        ResourceLocation(AppliedFlooringMod.MOD_ID, "main")
    ) {
        ItemStack(ModBlocks.ME_FLOORING.get())
    }

    fun register() {
        // Tab created statically on load
    }
}
