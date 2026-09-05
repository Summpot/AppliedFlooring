package io.github.summpot.appliedflooring.forge.client

import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.BakedModelWrapper
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.client.model.data.ModelProperty

object MEFlooringForgeModelProperties {
    val CTM_DATA: ModelProperty<IntArray> = ModelProperty()
}

class MEFlooringForgeBakedModel(
    original: BakedModel,
    private val block: MEFlooringBlock
) : BakedModelWrapper<BakedModel>(original) {

    override fun getModelData(
        level: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        modelData: ModelData
    ): ModelData {
        val connections = MEFlooringCTMHelper.computeConnections(level, pos, state)
        return modelData.derive().with(MEFlooringForgeModelProperties.CTM_DATA, connections).build()
    }

    override fun getQuads(
        state: BlockState?,
        side: Direction?,
        rand: RandomSource,
        extraData: ModelData,
        renderType: RenderType?
    ): List<BakedQuad> {
        if (side == null || state == null) {
            return originalModel.getQuads(state, side, rand, extraData, renderType)
        }
        val connectionData = extraData.get(MEFlooringForgeModelProperties.CTM_DATA)
        if (connectionData == null) {
            return originalModel.getQuads(state, side, rand, extraData, renderType)
        }
        return MEFlooringCTMHelper.getQuads(state, side, connectionData, block)
    }
}
