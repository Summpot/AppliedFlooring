package io.github.summpot.appliedflooring.fabric

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry
import io.github.summpot.appliedflooring.AppliedFlooringMod
import net.fabricmc.api.ModInitializer
import net.minecraftforge.fml.config.ModConfig

class AppliedFlooringFabric : ModInitializer {
    override fun onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(
            AppliedFlooringMod.MOD_ID,
            ModConfig.Type.COMMON,
            AppliedFlooringFabricConfig.SPEC
        )
        AppliedFlooringFabricConfig.applyToCommon()
        dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT) {
            Runnable {
                io.github.summpot.appliedflooring.fabric.client.MEFlooringFabricClient.init()
            }
        }
        AppliedFlooringMod.init()
    }
}