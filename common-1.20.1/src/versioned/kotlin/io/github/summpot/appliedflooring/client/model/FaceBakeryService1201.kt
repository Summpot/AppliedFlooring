package io.github.summpot.appliedflooring.client.model

import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.BlockElementFace
import net.minecraft.client.renderer.block.model.BlockFaceUV
import net.minecraft.client.renderer.block.model.FaceBakery
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.BlockModelRotation
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3f

class FaceBakeryService1201 : IFaceBakeryService {
    private val bakery = FaceBakery()
    private val dummyId = ResourceLocation("appliedflooring", "ctm_quad")

    override fun bakeQuad(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        u1: Float, v1: Float,
        u2: Float, v2: Float,
        sprite: TextureAtlasSprite,
        facing: Direction,
        tintIndex: Int
    ): BakedQuad {
        val from = Vector3f(minX, minY, minZ)
        val to = Vector3f(maxX, maxY, maxZ)
        val faceUV = BlockFaceUV(floatArrayOf(u1, v1, u2, v2), 0)
        val elementFace = BlockElementFace(facing, tintIndex, "", faceUV)
        return bakery.bakeQuad(
            from, to, elementFace, sprite, facing,
            BlockModelRotation.X0_Y0, null, true, dummyId
        )
    }
}
