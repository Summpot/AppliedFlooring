package io.github.summpot.appliedflooring.forge

import dev.architectury.platform.forge.EventBuses
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.config.ModConfigEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(AppliedFlooringMod.MOD_ID)
class AppliedFlooringForge {
    init {
        EventBuses.registerModEventBus(AppliedFlooringMod.MOD_ID, MOD_BUS)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AppliedFlooringForgeConfig.SPEC)
        MOD_BUS.addListener(::onConfigLoading)
        MOD_BUS.addListener(::onConfigReloading)
        MOD_BUS.addListener(::onAddPackFinders)
        AppliedFlooringMod.init()
    }

    private fun onAddPackFinders(event: net.minecraftforge.event.AddPackFindersEvent) {
        if (event.packType == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(io.github.summpot.appliedflooring.client.dynamic.AppliedFlooringPackSource)
        }
    }

    private fun onConfigLoading(event: ModConfigEvent.Loading) {
        if (event.config.modId == AppliedFlooringMod.MOD_ID) {
            AppliedFlooringForgeConfig.applyToCommon()
        }
    }

    private fun onConfigReloading(event: ModConfigEvent.Reloading) {
        if (event.config.modId == AppliedFlooringMod.MOD_ID) {
            AppliedFlooringForgeConfig.applyToCommon()
        }
    }
}
