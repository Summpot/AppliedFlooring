package io.github.summpot.appliedflooring.neoforge

import appeng.api.AECapabilities
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

@Mod(AppliedFlooringMod.MOD_ID)
class AppliedFlooringNeoForge(bus: IEventBus) {
    init {
        AppliedFlooringMod.init()
        bus.addListener(::registerCapabilities)
    }

    private fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_FLOORING_BE.get()) { be, _ -> be }
    }
}