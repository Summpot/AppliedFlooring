package io.github.summpot.appliedflooring.client.model

import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction

interface IFaceBakeryService {
    fun bakeQuad(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        u1: Float, v1: Float,
        u2: Float, v2: Float,
        sprite: TextureAtlasSprite,
        facing: Direction,
        tintIndex: Int
    ): BakedQuad
}
