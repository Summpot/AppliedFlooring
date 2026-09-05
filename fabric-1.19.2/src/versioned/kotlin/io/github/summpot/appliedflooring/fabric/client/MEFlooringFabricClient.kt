package io.github.summpot.appliedflooring.fabric.client

import com.mojang.datafixers.util.Pair
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelVariantProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.Material
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelState
import net.minecraft.client.resources.model.UnbakedModel
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import java.util.function.Function

object MEFlooringFabricClient {
    fun init() {
        ModelLoadingRegistry.INSTANCE.registerVariantProvider { _ ->
            ModelVariantProvider { modelId, context ->
                if (modelId.namespace == AppliedFlooringMod.MOD_ID && modelId.variant != "inventory") {
                    val block = Registry.BLOCK.get(ResourceLocation.tryParse("${modelId.namespace}:${modelId.path}"))
                    if (block is MEFlooringBlock) {
                        val unbaked = context.loadModel(modelId)
                        return@ModelVariantProvider object : UnbakedModel {
                            override fun getDependencies(): Collection<ResourceLocation> {
                                return unbaked.dependencies
                            }

                            override fun getMaterials(
                                modelGetter: Function<ResourceLocation, UnbakedModel>,
                                missingTextureErrors: Set<Pair<String, String>>
                            ): Collection<Material> {
                                return unbaked.getMaterials(modelGetter, missingTextureErrors)
                            }

                            override fun bake(
                                baker: ModelBakery,
                                spriteGetter: Function<Material, TextureAtlasSprite>,
                                state: ModelState,
                                location: ResourceLocation
                            ): BakedModel? {
                                MEFlooringCTMHelper.initSprites()
                                val baked = unbaked.bake(baker, spriteGetter, state, location) ?: return null
                                return MEFlooringFabricBakedModel(baked, block)
                            }
                        }
                    }
                }
                null
            }
        }
    }
}
