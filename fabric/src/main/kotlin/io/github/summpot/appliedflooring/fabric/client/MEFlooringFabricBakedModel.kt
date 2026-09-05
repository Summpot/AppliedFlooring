package io.github.summpot.appliedflooring.fabric.client

import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Supplier

class MEFlooringFabricBakedModel(
    private val original: BakedModel,
    private val block: MEFlooringBlock
) : BakedModel by original, FabricBakedModel {

    override fun isVanillaAdapter(): Boolean = false

    override fun emitBlockQuads(
        blockView: BlockAndTintGetter,
        state: BlockState,
        pos: BlockPos,
        randomSupplier: Supplier<RandomSource>,
        context: RenderContext
    ) {
        val connections = MEFlooringCTMHelper.computeConnections(blockView, pos, state)
        val emitter = context.emitter
        for (side in Direction.values()) {
            val quads = MEFlooringCTMHelper.getQuads(state, side, connections, block)
            for (quad in quads) {
                emitter.fromVanilla(quad.vertices, 0, false)
                emitter.cullFace(side)
                emitter.colorIndex(quad.tintIndex)
                emitter.emit()
            }
        }
    }

    override fun emitItemQuads(
        stack: ItemStack,
        randomSupplier: Supplier<RandomSource>,
        context: RenderContext
    ) {
        val emitter = context.emitter
        for (side in Direction.values()) {
            for (quad in original.getQuads(null, side, randomSupplier.get())) {
                emitter.fromVanilla(quad.vertices, 0, false)
                emitter.cullFace(side)
                emitter.colorIndex(quad.tintIndex)
                emitter.emit()
            }
        }
        for (quad in original.getQuads(null, null, randomSupplier.get())) {
            emitter.fromVanilla(quad.vertices, 0, false)
            emitter.cullFace(null)
            emitter.colorIndex(quad.tintIndex)
            emitter.emit()
        }
    }
}
