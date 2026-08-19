package io.github.summpot.appliedflooring

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
        LOGGER.info("Applied Flooring Mod initialized successfully.")
    }
}
