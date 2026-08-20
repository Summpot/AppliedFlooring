package io.github.summpot.appliedflooring.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector3f
import io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.Direction

class MEFlooringBlockEntityRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<MEFlooringBlockEntity> {

    override fun render(
        be: MEFlooringBlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val modelManager = Minecraft.getInstance().modelManager
        val blockRenderer = Minecraft.getInstance().blockRenderer

        for (side in Direction.values()) {
            val part = be.getPart(side) ?: continue
            val sideLight = be.level?.let { lvl ->
                net.minecraft.client.renderer.LevelRenderer.getLightColor(lvl, be.blockPos.relative(side))
            } ?: combinedLight

            // 1. Static part models
            val staticModels = part.staticModels
            val models = staticModels.models
            if (models.isNotEmpty()) {
                poseStack.pushPose()
                poseStack.translate(0.5, 0.5, 0.5)
                when (side) {
                    Direction.DOWN -> poseStack.mulPose(Vector3f.XP.rotationDegrees(270f))
                    Direction.UP -> poseStack.mulPose(Vector3f.XP.rotationDegrees(90f))
                    Direction.NORTH -> {}
                    Direction.SOUTH -> poseStack.mulPose(Vector3f.YP.rotationDegrees(180f))
                    Direction.WEST -> poseStack.mulPose(Vector3f.YP.rotationDegrees(90f))
                    Direction.EAST -> poseStack.mulPose(Vector3f.YP.rotationDegrees(270f))
                }
                poseStack.translate(-0.5, -0.5, -0.5)
                poseStack.translate(0.0, 0.0, -0.0001)

                for (modelLoc in models) {
                    val mrl = ModelResourceLocation(modelLoc, "")
                    val bakedModel = modelManager.getModel(mrl)
                    if (bakedModel != null && bakedModel != modelManager.missingModel) {
                        val buffer = bufferSource.getBuffer(RenderType.cutout())
                        renderBakedModelWithShade(
                            poseStack,
                            buffer,
                            bakedModel,
                            side,
                            sideLight,
                            combinedOverlay
                        )
                    }
                }
                poseStack.popPose()
            }

            // 2. Dynamic part render
            if (part.requireDynamicRender()) {
                part.renderDynamic(partialTicks, poseStack, bufferSource, sideLight, combinedOverlay)
            }
        }
    }

    private fun renderBakedModelWithShade(
        poseStack: PoseStack,
        buffer: com.mojang.blaze3d.vertex.VertexConsumer,
        model: net.minecraft.client.resources.model.BakedModel,
        side: Direction,
        light: Int,
        overlay: Int
    ) {
        val pose = poseStack.last()
        val random = net.minecraft.util.RandomSource.create()
        val directions = Direction.values()

        fun renderQuads(quads: List<net.minecraft.client.renderer.block.model.BakedQuad>) {
            for (quad in quads) {
                val worldDir = transformDirection(quad.direction, side)
                val shade = if (quad.isShade) getDirectionalShade(worldDir) else 1.0f
                buffer.putBulkData(pose, quad, shade, shade, shade, light, overlay)
            }
        }

        for (dir in directions) {
            renderQuads(model.getQuads(null, dir, random))
        }
        renderQuads(model.getQuads(null, null, random))
    }

    private fun getDirectionalShade(dir: Direction): Float {
        return when (dir) {
            Direction.DOWN -> 0.5f
            Direction.UP -> 1.0f
            Direction.NORTH, Direction.SOUTH -> 0.8f
            Direction.WEST, Direction.EAST -> 0.6f
        }
    }

    private fun transformDirection(localDir: Direction?, side: Direction): Direction {
        if (localDir == null) return side
        return when (side) {
            Direction.NORTH -> localDir
            Direction.SOUTH -> when (localDir) {
                Direction.NORTH -> Direction.SOUTH
                Direction.SOUTH -> Direction.NORTH
                Direction.WEST -> Direction.EAST
                Direction.EAST -> Direction.WEST
                else -> localDir
            }
            Direction.WEST -> when (localDir) {
                Direction.NORTH -> Direction.WEST
                Direction.SOUTH -> Direction.EAST
                Direction.WEST -> Direction.SOUTH
                Direction.EAST -> Direction.NORTH
                else -> localDir
            }
            Direction.EAST -> when (localDir) {
                Direction.NORTH -> Direction.EAST
                Direction.SOUTH -> Direction.WEST
                Direction.WEST -> Direction.NORTH
                Direction.EAST -> Direction.SOUTH
                else -> localDir
            }
            Direction.UP -> when (localDir) {
                Direction.NORTH -> Direction.UP
                Direction.SOUTH -> Direction.DOWN
                Direction.UP -> Direction.SOUTH
                Direction.DOWN -> Direction.NORTH
                else -> localDir
            }
            Direction.DOWN -> when (localDir) {
                Direction.NORTH -> Direction.DOWN
                Direction.SOUTH -> Direction.UP
                Direction.UP -> Direction.NORTH
                Direction.DOWN -> Direction.SOUTH
                else -> localDir
            }
        }
    }
}