package io.github.summpot.appliedflooring.fabric

import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.fabricmc.api.ModInitializer

class AppliedFlooringFabric : ModInitializer {
    override fun onInitialize() {
        AppliedFlooringMod.init()
    }
}
