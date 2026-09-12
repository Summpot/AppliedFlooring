package io.github.summpot.appliedflooring.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f

class MELaserConnectorRenderer(val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<MELaserConnectorBlockEntity> {

    companion object {
        private val LASER_CORE_TEXTURE = ResourceLocation.tryParse("appliedflooring:textures/misc/laser.png")!!
        private val LASER_BEAM_TEXTURE = ResourceLocation.tryParse("appliedflooring:textures/misc/laser2.png")!!
        private val LASER_GLOW_TEXTURE = ResourceLocation.tryParse("appliedflooring:textures/misc/laser_glow.png")!!
        private val INFINITE_AABB = AABB(
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY
        )
    }

    override fun shouldRenderOffScreen(be: MELaserConnectorBlockEntity): Boolean = true

    override fun getViewDistance(): Int = 256

    override fun shouldRender(be: MELaserConnectorBlockEntity, cameraPos: Vec3): Boolean {
        val targetUp = be.connectedTargetUp
        if (targetUp != null) {
            val center = Vec3.atCenterOf(be.blockPos)
            val dx = center.x - cameraPos.x
            val dz = center.z - cameraPos.z
            val horizDistSq = dx * dx + dz * dz
            val maxDist = getViewDistance().toDouble()
            if (horizDistSq > maxDist * maxDist) return false
            val minY = be.blockPos.y.toDouble()
            val maxY = targetUp.y.toDouble() + 1.0
            return cameraPos.y >= minY - maxDist && cameraPos.y <= maxY + maxDist
        }
        return super.shouldRender(be, cameraPos)
    }

    fun getRenderBoundingBox(be: BlockEntity): AABB {
        return INFINITE_AABB
    }

    fun getRenderBoundingBox(be: MELaserConnectorBlockEntity): AABB {
        return INFINITE_AABB
    }

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
            if (height > 1.0f) {
                renderLaserBeam(be, partialTicks, poseStack, bufferSource, height)
            }
        }

        if (be.builderStatus == io.github.summpot.appliedflooring.blockentity.BuilderStatus.BUILDING) {
            renderBuildingEffects(be, partialTicks, poseStack, bufferSource)
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
        val animTime = (gameTime % 72000L).toFloat() + partialTicks

        val minY = 1.0f
        val maxY = height
        val length = maxY - minY
        if (length <= 0.0f) return

        val pose = poseStack.last().pose()
        val normal = poseStack.last().normal()

        // Beacon-style square beam: inner core, plasma, outer glow, square end caps.
        val rot = 0.0f
        val diskR = r * 0.5f + 0.5f
        val diskG = g * 0.5f + 0.5f
        val diskB = b * 0.5f + 0.5f
        val capHalf = 0.22f + Mth.sin(animTime * 0.12f) * 0.015f
        val capBuffer = bufferSource.getBuffer(RenderType.beaconBeam(LASER_GLOW_TEXTURE, true))
        renderHorizontalSquare(pose, normal, capBuffer, 1.002f, capHalf, rot, diskR, diskG, diskB, 0.85f)
        renderHorizontalSquare(pose, normal, capBuffer, maxY - 0.002f, capHalf, -rot, diskR, diskG, diskB, 0.85f)

        val glowBuffer = bufferSource.getBuffer(RenderType.beaconBeam(LASER_GLOW_TEXTURE, true))
        val glowHalf = 0.20f + Mth.sin(animTime * 0.06f) * 0.015f
        val vGlow1 = -animTime * 0.02f
        val vGlow2 = vGlow1 + length * 0.5f
        renderSquarePrism(pose, normal, glowBuffer, minY, maxY, glowHalf, -rot * 0.5f, r, g, b, 0.28f, vGlow1, vGlow2)

        val beamBuffer = bufferSource.getBuffer(RenderType.beaconBeam(LASER_BEAM_TEXTURE, true))
        val beamHalf = 0.11f + Mth.sin(animTime * 0.1f) * 0.008f
        val vBeam1 = animTime * 0.04f
        val vBeam2 = vBeam1 + length
        renderSquarePrism(pose, normal, beamBuffer, minY, maxY, beamHalf, rot, r, g, b, 0.78f, vBeam1, vBeam2)

        val coreBuffer = bufferSource.getBuffer(RenderType.beaconBeam(LASER_CORE_TEXTURE, true))
        val coreHalf = 0.045f + Mth.sin(animTime * 0.15f) * 0.004f
        val vCore1 = -animTime * 0.08f
        val vCore2 = vCore1 + length * 1.5f
        val coreR = r * 0.4f + 0.6f
        val coreG = g * 0.4f + 0.6f
        val coreB = b * 0.4f + 0.6f
        renderSquarePrism(pose, normal, coreBuffer, minY, maxY, coreHalf, rot * 1.25f, coreR, coreG, coreB, 0.95f, vCore1, vCore2)
    }

    private fun rotatedSquareCorners(half: Float, rotDeg: Float): Array<Pair<Float, Float>> {
        val rad = Math.toRadians(rotDeg.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val locals = arrayOf(
            floatArrayOf(-half, -half),
            floatArrayOf(half, -half),
            floatArrayOf(half, half),
            floatArrayOf(-half, half)
        )
        return Array(4) { i ->
            val lx = locals[i][0]
            val lz = locals[i][1]
            val x = lx * cos - lz * sin
            val z = lx * sin + lz * cos
            (0.5f + x) to (0.5f + z)
        }
    }

    private fun renderSquarePrism(
        mat: Matrix4f,
        normalMat: Matrix3f,
        buffer: VertexConsumer,
        minY: Float,
        maxY: Float,
        half: Float,
        rotDeg: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        v1: Float,
        v2: Float
    ) {
        val corners = rotatedSquareCorners(half, rotDeg)
        for (i in 0 until 4) {
            val (x1, z1) = corners[i]
            val (x2, z2) = corners[(i + 1) % 4]
            addVertex(mat, normalMat, buffer, x1, minY, z1, r, g, b, a, 0.0f, v1)
            addVertex(mat, normalMat, buffer, x1, maxY, z1, r, g, b, a, 0.0f, v2)
            addVertex(mat, normalMat, buffer, x2, maxY, z2, r, g, b, a, 1.0f, v2)
            addVertex(mat, normalMat, buffer, x2, minY, z2, r, g, b, a, 1.0f, v1)

            addVertex(mat, normalMat, buffer, x2, minY, z2, r, g, b, a, 1.0f, v1)
            addVertex(mat, normalMat, buffer, x2, maxY, z2, r, g, b, a, 1.0f, v2)
            addVertex(mat, normalMat, buffer, x1, maxY, z1, r, g, b, a, 0.0f, v2)
            addVertex(mat, normalMat, buffer, x1, minY, z1, r, g, b, a, 0.0f, v1)
        }
    }

    private fun renderHorizontalSquare(
        mat: Matrix4f,
        normalMat: Matrix3f,
        buffer: VertexConsumer,
        y: Float,
        half: Float,
        rotDeg: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val c = rotatedSquareCorners(half, rotDeg)
        addVertex(mat, normalMat, buffer, c[0].first, y, c[0].second, r, g, b, a, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, c[1].first, y, c[1].second, r, g, b, a, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, c[2].first, y, c[2].second, r, g, b, a, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, c[3].first, y, c[3].second, r, g, b, a, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)

        addVertex(mat, normalMat, buffer, c[3].first, y, c[3].second, r, g, b, a, 0.0f, 1.0f, 0.0f, -1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, c[2].first, y, c[2].second, r, g, b, a, 1.0f, 1.0f, 0.0f, -1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, c[1].first, y, c[1].second, r, g, b, a, 1.0f, 0.0f, 0.0f, -1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, c[0].first, y, c[0].second, r, g, b, a, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f)
    }

    private fun addVertex(
        mat: Matrix4f,
        normalMat: Matrix3f,
        buffer: VertexConsumer,
        x: Float,
        y: Float,
        z: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        u: Float,
        v: Float,
        nx: Float = 0.0f,
        ny: Float = 1.0f,
        nz: Float = 0.0f
    ) {
        buffer.addVertex(mat, x, y, z)
            .setColor(r, g, b, a)
            .setUv(u, v)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(nx, ny, nz)
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
            appeng.api.util.AEColor.BLACK -> Triple(0.25f, 0.25f, 0.3f)
            appeng.api.util.AEColor.TRANSPARENT -> Triple(0.2f, 0.85f, 1.0f)
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

    private fun renderBuildingEffects(
        be: MELaserConnectorBlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val targetY = be.resolveBuildY()
        val relY = (targetY - be.blockPos.y).toFloat()
        val placingPos = be.currentPlacingPos

        val rgb = getLaserColor(be.floorColor)
        val r = rgb.first
        val g = rgb.second
        val b = rgb.third

        // 1. Render beam pulse to currently placing block
        if (placingPos != null) {
            val dx = (placingPos.x - be.blockPos.x).toFloat()
            val dz = (placingPos.z - be.blockPos.z).toFloat()

            val startX = 0.5f
            val startY = relY + 0.5f
            val startZ = 0.5f

            val endX = dx + 0.5f
            val endY = relY + 0.5f
            val endZ = dz + 0.5f

            val buffer = bufferSource.getBuffer(RenderType.beaconBeam(LASER_CORE_TEXTURE, true))
            val pose = poseStack.last().pose()
            val normal = poseStack.last().normal()

            val rad = 0.04f
            addVertex(pose, normal, buffer, startX, startY, startZ, r, g, b, 0.9f, 0.0f, 0.0f)
            addVertex(pose, normal, buffer, startX, startY + rad, startZ, r, g, b, 0.9f, 0.0f, 1.0f)
            addVertex(pose, normal, buffer, endX, endY + rad, endZ, r, g, b, 0.9f, 1.0f, 1.0f)
            addVertex(pose, normal, buffer, endX, endY, endZ, r, g, b, 0.9f, 1.0f, 0.0f)
        }

        // 2. Render holographic perimeter boundary at target floor layer
        val rx = be.radiusX.toFloat()
        val rz = be.radiusZ.toFloat()
        val minX = -rx
        val maxX = rx + 1.0f
        val minZ = -rz
        val maxZ = rz + 1.0f
        val yBox = relY + 0.02f

        val glowBuffer = bufferSource.getBuffer(RenderType.beaconBeam(LASER_GLOW_TEXTURE, true))
        val pose = poseStack.last().pose()
        val normal = poseStack.last().normal()
        val lineW = 0.06f

        // North edge
        renderHoloEdge(pose, normal, glowBuffer, minX, maxX, minZ, minZ + lineW, yBox, r, g, b, 0.5f)
        // South edge
        renderHoloEdge(pose, normal, glowBuffer, minX, maxX, maxZ - lineW, maxZ, yBox, r, g, b, 0.5f)
        // West edge
        renderHoloEdge(pose, normal, glowBuffer, minX, minX + lineW, minZ, maxZ, yBox, r, g, b, 0.5f)
        // East edge
        renderHoloEdge(pose, normal, glowBuffer, maxX - lineW, maxX, minZ, maxZ, yBox, r, g, b, 0.5f)
    }

    private fun renderHoloEdge(
        mat: Matrix4f,
        normalMat: Matrix3f,
        buffer: VertexConsumer,
        x1: Float,
        x2: Float,
        z1: Float,
        z2: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        addVertex(mat, normalMat, buffer, x1, y, z1, r, g, b, a, 0.0f, 0.0f)
        addVertex(mat, normalMat, buffer, x2, y, z1, r, g, b, a, 1.0f, 0.0f)
        addVertex(mat, normalMat, buffer, x2, y, z2, r, g, b, a, 1.0f, 1.0f)
        addVertex(mat, normalMat, buffer, x1, y, z2, r, g, b, a, 0.0f, 1.0f)
    }
}
