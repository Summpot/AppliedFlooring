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

            // 1. Static part models
            val staticModels = part.staticModels
            val models = staticModels.models
            if (models.isNotEmpty()) {
                poseStack.pushPose()
                poseStack.translate(0.5, 0.5, 0.5)
                when (side) {
                    Direction.DOWN -> poseStack.mulPose(Vector3f.XP.rotationDegrees(180f))
                    Direction.UP -> {}
                    Direction.NORTH -> poseStack.mulPose(Vector3f.XP.rotationDegrees(-90f))
                    Direction.SOUTH -> poseStack.mulPose(Vector3f.XP.rotationDegrees(90f))
                    Direction.WEST -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(90f))
                    Direction.EAST -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90f))
                }
                poseStack.translate(-0.5, -0.5, -0.5)

                for (modelLoc in models) {
                    val mrl = ModelResourceLocation(modelLoc, "")
                    val bakedModel = modelManager.getModel(mrl)
                    if (bakedModel != null && bakedModel != modelManager.missingModel) {
                        blockRenderer.modelRenderer.renderModel(
                            poseStack.last(),
                            bufferSource.getBuffer(RenderType.cutout()),
                            be.blockState,
                            bakedModel,
                            1.0f, 1.0f, 1.0f,
                            combinedLight,
                            combinedOverlay
                        )
                    }
                }
                poseStack.popPose()
            }

            // 2. Dynamic part render
            if (part.requireDynamicRender()) {
                part.renderDynamic(partialTicks, poseStack, bufferSource, combinedLight, combinedOverlay)
            }
        }
    }
}