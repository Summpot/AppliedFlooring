package io.github.summpot.appliedflooring

import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import io.github.summpot.appliedflooring.client.MEFlooringClient
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import io.github.summpot.appliedflooring.registry.ModBlocks
import io.github.summpot.appliedflooring.registry.ModCreativeTabs
import io.github.summpot.appliedflooring.registry.ModItems
import org.slf4j.LoggerFactory

object AppliedFlooringMod {
    const val MOD_ID = "appliedflooring"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun init() {
        LOGGER.info("Initializing Applied Flooring Mod...")
        ModBlocks.register()
        ModItems.register()
        ModBlockEntities.register()
        ModCreativeTabs.register()

        EnvExecutor.runInEnv(Env.CLIENT) {
            Runnable {
                dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register {
                    MEFlooringClient.init()
                }
            }
        }

        LOGGER.info("Applied Flooring Mod initialized successfully.")
    }
}
