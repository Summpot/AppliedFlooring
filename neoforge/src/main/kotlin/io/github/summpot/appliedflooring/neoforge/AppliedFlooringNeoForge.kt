package io.github.summpot.appliedflooring.neoforge

import appeng.api.AECapabilities
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

@Mod(AppliedFlooringMod.MOD_ID)
class AppliedFlooringNeoForge(bus: IEventBus, container: ModContainer) {
    init {
        container.registerConfig(ModConfig.Type.COMMON, AppliedFlooringNeoForgeConfig.SPEC)
        bus.addListener(this::onConfigLoading)
        bus.addListener(this::onConfigReloading)
        AppliedFlooringMod.init()
        bus.addListener(::registerCapabilities)
        dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT) {
            Runnable {
                io.github.summpot.appliedflooring.neoforge.client.MEFlooringNeoForgeClient.register(bus)
            }
        }
    }

    private fun onConfigLoading(event: ModConfigEvent.Loading) {
        if (event.config.modId == AppliedFlooringMod.MOD_ID) {
            AppliedFlooringNeoForgeConfig.applyToCommon()
        }
    }

    private fun onConfigReloading(event: ModConfigEvent.Reloading) {
        if (event.config.modId == AppliedFlooringMod.MOD_ID) {
            AppliedFlooringNeoForgeConfig.applyToCommon()
        }
    }

    private fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_FLOORING_BE.get()) { be, _ -> be }
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_LASER_CONNECTOR_BE.get()) { be, _ -> be }
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_ELEVATOR_BE.get()) { be, _ -> be }
    }
}