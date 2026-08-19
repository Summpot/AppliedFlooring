package io.github.summpot.appliedflooring.forge

import dev.architectury.platform.forge.EventBuses
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(AppliedFlooringMod.MOD_ID)
class AppliedFlooringForge {
    init {
        EventBuses.registerModEventBus(AppliedFlooringMod.MOD_ID, MOD_BUS)
        AppliedFlooringMod.init()
    }
}
