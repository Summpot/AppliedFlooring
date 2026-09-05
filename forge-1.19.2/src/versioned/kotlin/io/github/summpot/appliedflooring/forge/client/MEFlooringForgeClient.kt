package io.github.summpot.appliedflooring.forge.client

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.eventbus.api.SubscribeEvent

@OnlyIn(Dist.CLIENT)
object MEFlooringForgeClient {

    fun register(bus: IEventBus) {
        bus.register(this)
    }

    @SubscribeEvent
    fun onBakingCompleted(event: ModelEvent.BakingCompleted) {
        MEFlooringForgeClientShared.wrapModels(event.models)
    }
}
