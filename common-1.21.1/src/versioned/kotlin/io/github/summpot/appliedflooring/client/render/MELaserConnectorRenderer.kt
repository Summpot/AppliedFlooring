package io.github.summpot.appliedflooring.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.Direction
import org.joml.Matrix4f

class MELaserConnectorRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<MELaserConnectorBlockEntity> {

    override fun render(
        be: MELaserConnectorBlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        renderAttachedParts(be, partialTicks, poseStack, bufferSource, combinedLight, combinedOverlay)

        val targetUp = be.connectedTargetUp
        if (targetUp != null && be.isPowered()) {
            val height = (targetUp.y - be.blockPos.y).toFloat()
            if (height > 0.0f) {
                renderLaserBeam(be, partialTicks, poseStack, bufferSource, height)
            }
        }
    }

    private fun renderLaserBeam(
        be: MELaserConnectorBlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        height: Float
    ) {
        val rgb = getLaserColor(be.currentColor)
        val r = rgb.first
        val g = rgb.second
        val b = rgb.third

        val gameTime = be.level?.gameTime ?: 0L
        val animAngle = ((gameTime % 360) + partialTicks) * 3.0f

        poseStack.pushPose()
        poseStack.translate(0.5, 0.0, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(animAngle))
        poseStack.translate(-0.5, 0.0, -0.5)

        val buffer = bufferSource.getBuffer(RenderType.lightning())
        val mat = poseStack.last().pose()

        // 1. Inner intense beam
        renderBeamBox(mat, buffer, 0.06f, 1.0f, height, r, g, b, 0.9f)

        // 2. Outer glow halo
        renderBeamBox(mat, buffer, 0.14f, 1.0f, height, r, g, b, 0.35f)

        poseStack.popPose()
    }

    private fun renderBeamBox(
        mat: Matrix4f,
        buffer: VertexConsumer,
        radius: Float,
        minY: Float,
        maxY: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val minX = 0.5f - radius
        val maxX = 0.5f + radius
        val minZ = 0.5f - radius
        val maxZ = 0.5f + radius

        // North
        addVertex(mat, buffer, minX, minY, minZ, r, g, b, a)
        addVertex(mat, buffer, minX, maxY, minZ, r, g, b, a)
        addVertex(mat, buffer, maxX, maxY, minZ, r, g, b, a)
        addVertex(mat, buffer, maxX, minY, minZ, r, g, b, a)

        // South
        addVertex(mat, buffer, maxX, minY, maxZ, r, g, b, a)
        addVertex(mat, buffer, maxX, maxY, maxZ, r, g, b, a)
        addVertex(mat, buffer, minX, maxY, maxZ, r, g, b, a)
        addVertex(mat, buffer, minX, minY, maxZ, r, g, b, a)

        // West
        addVertex(mat, buffer, minX, minY, maxZ, r, g, b, a)
        addVertex(mat, buffer, minX, maxY, maxZ, r, g, b, a)
        addVertex(mat, buffer, minX, maxY, minZ, r, g, b, a)
        addVertex(mat, buffer, minX, minY, minZ, r, g, b, a)

        // East
        addVertex(mat, buffer, maxX, minY, minZ, r, g, b, a)
        addVertex(mat, buffer, maxX, maxY, minZ, r, g, b, a)
        addVertex(mat, buffer, maxX, maxY, maxZ, r, g, b, a)
        addVertex(mat, buffer, maxX, minY, maxZ, r, g, b, a)
    }

    private fun addVertex(
        mat: Matrix4f,
        buffer: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        buffer.addVertex(mat, x, y, z).setColor(r, g, b, a)
    }

    private fun getLaserColor(color: appeng.api.util.AEColor): Triple<Float, Float, Float> {
        return when (color) {
            appeng.api.util.AEColor.WHITE -> Triple(1.0f, 1.0f, 1.0f)
            appeng.api.util.AEColor.ORANGE -> Triple(1.0f, 0.55f, 0.1f)
            appeng.api.util.AEColor.MAGENTA -> Triple(0.9f, 0.25f, 0.9f)
            appeng.api.util.AEColor.LIGHT_BLUE -> Triple(0.4f, 0.75f, 1.0f)
            appeng.api.util.AEColor.YELLOW -> Triple(1.0f, 0.95f, 0.2f)
            appeng.api.util.AEColor.LIME -> Triple(0.45f, 1.0f, 0.2f)
            appeng.api.util.AEColor.PINK -> Triple(1.0f, 0.55f, 0.75f)
            appeng.api.util.AEColor.GRAY -> Triple(0.4f, 0.45f, 0.5f)
            appeng.api.util.AEColor.LIGHT_GRAY -> Triple(0.7f, 0.75f, 0.8f)
            appeng.api.util.AEColor.CYAN -> Triple(0.1f, 0.85f, 0.95f)
            appeng.api.util.AEColor.PURPLE -> Triple(0.65f, 0.25f, 0.95f)
            appeng.api.util.AEColor.BLUE -> Triple(0.2f, 0.35f, 1.0f)
            appeng.api.util.AEColor.BROWN -> Triple(0.6f, 0.35f, 0.15f)
            appeng.api.util.AEColor.GREEN -> Triple(0.25f, 0.75f, 0.15f)
            appeng.api.util.AEColor.RED -> Triple(1.0f, 0.2f, 0.2f)
            appeng.api.util.AEColor.BLACK -> Triple(0.2f, 0.2f, 0.25f)
            appeng.api.util.AEColor.TRANSPARENT -> Triple(0.3f, 0.85f, 1.0f)
        }
    }

    private fun renderAttachedParts(
        be: MELaserConnectorBlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val modelManager = Minecraft.getInstance().modelManager

        for (side in Direction.values()) {
            val part = be.getPart(side) ?: continue
            val sideLight = be.level?.let { lvl ->
                net.minecraft.client.renderer.LevelRenderer.getLightColor(lvl, be.blockPos.relative(side))
            } ?: combinedLight

            val staticModels = part.staticModels
            val models = staticModels.models
            if (models.isNotEmpty()) {
                poseStack.pushPose()
                poseStack.translate(0.5, 0.5, 0.5)
                when (side) {
                    Direction.DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(270f))
                    Direction.UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90f))
                    Direction.NORTH -> {}
                    Direction.SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                    Direction.WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90f))
                    Direction.EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270f))
                }
                poseStack.translate(-0.5, -0.5, -0.5)
                poseStack.translate(0.0, 0.0, -0.0001)

                for (modelLoc in models) {
                    val mrl = ModelResourceLocation(modelLoc, "standalone")
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

            if (part.requireDynamicRender()) {
                part.renderDynamic(partialTicks, poseStack, bufferSource, sideLight, combinedOverlay)
            }
        }
    }

    private fun renderBakedModelWithShade(
        poseStack: PoseStack,
        buffer: VertexConsumer,
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
                buffer.putBulkData(pose, quad, shade, shade, shade, 1.0f, light, overlay)
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
