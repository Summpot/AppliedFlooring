package io.github.summpot.appliedflooring.forge.client

import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper
import io.github.summpot.appliedflooring.registry.ModBlocks
import net.minecraft.client.renderer.block.BlockModelShaper
import net.minecraft.client.resources.model.BakedModel

object MEFlooringForgeClientShared {

    @Suppress("UNCHECKED_CAST")
    fun wrapModels(models: MutableMap<*, BakedModel>) {
        MEFlooringCTMHelper.initSprites()

        val allFlooringBlocks = ArrayList<MEFlooringBlock>()
        (ModBlocks.ME_FLOORING.get() as? MEFlooringBlock)?.let { allFlooringBlocks.add(it) }
        allFlooringBlocks.addAll(ModBlocks.COLORED_ME_FLOORING.values.mapNotNull { it.get() as? MEFlooringBlock })
        (ModBlocks.ME_ELEVATOR.get() as? MEFlooringBlock)?.let { allFlooringBlocks.add(it) }
        allFlooringBlocks.addAll(ModBlocks.COLORED_ME_ELEVATOR.values.mapNotNull { it.get() as? MEFlooringBlock })
        (ModBlocks.ME_LASER_CONNECTOR.get() as? MEFlooringBlock)?.let { allFlooringBlocks.add(it) }
        allFlooringBlocks.addAll(ModBlocks.COLORED_ME_LASER_CONNECTOR.values.mapNotNull { it.get() as? MEFlooringBlock })

        val map = models as MutableMap<Any, BakedModel>

        for (block in allFlooringBlocks) {
            for (state in block.stateDefinition.possibleStates) {
                val mrl = BlockModelShaper.stateToModelLocation(state)
                val existing = map[mrl]
                if (existing != null) {
                    map[mrl] = MEFlooringForgeBakedModel(existing, block)
                }
            }
        }
    }
}
