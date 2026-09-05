package io.github.summpot.appliedflooring.neoforge.client

import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper
import io.github.summpot.appliedflooring.registry.ModBlocks
import net.minecraft.client.renderer.block.BlockModelShaper
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.ModelEvent

object MEFlooringNeoForgeClient {

    fun register(bus: IEventBus) {
        bus.addListener(::onModifyBakingResult)
    }

    private fun onModifyBakingResult(event: ModelEvent.ModifyBakingResult) {
        MEFlooringCTMHelper.initSprites()
        val models = event.models

        val allFlooringBlocks = ArrayList<MEFlooringBlock>()
        (ModBlocks.ME_FLOORING.get() as? MEFlooringBlock)?.let { allFlooringBlocks.add(it) }
        allFlooringBlocks.addAll(ModBlocks.COLORED_ME_FLOORING.values.mapNotNull { it.get() as? MEFlooringBlock })
        (ModBlocks.ME_ELEVATOR.get() as? MEFlooringBlock)?.let { allFlooringBlocks.add(it) }
        allFlooringBlocks.addAll(ModBlocks.COLORED_ME_ELEVATOR.values.mapNotNull { it.get() as? MEFlooringBlock })
        (ModBlocks.ME_LASER_CONNECTOR.get() as? MEFlooringBlock)?.let { allFlooringBlocks.add(it) }
        allFlooringBlocks.addAll(ModBlocks.COLORED_ME_LASER_CONNECTOR.values.mapNotNull { it.get() as? MEFlooringBlock })

        for (block in allFlooringBlocks) {
            for (state in block.stateDefinition.possibleStates) {
                val mrl = BlockModelShaper.stateToModelLocation(state)
                val existing = models[mrl]
                if (existing != null) {
                    models[mrl] = MEFlooringNeoForgeBakedModel(existing, block)
                }
            }
        }
    }
}
