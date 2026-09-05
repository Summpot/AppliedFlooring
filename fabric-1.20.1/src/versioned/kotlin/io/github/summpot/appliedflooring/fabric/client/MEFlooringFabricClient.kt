package io.github.summpot.appliedflooring.fabric.client

import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.client.model.MEFlooringCTMHelper
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation

object MEFlooringFabricClient {
    fun init() {
        ModelLoadingPlugin.register { pluginContext ->
            pluginContext.modifyModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE) { model, context ->
                MEFlooringCTMHelper.initSprites()
                val id = context.id()
                if (id is ModelResourceLocation && id.namespace == AppliedFlooringMod.MOD_ID) {
                    val block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("${id.namespace}:${id.path}"))
                    if (block is MEFlooringBlock && model != null) {
                        return@register MEFlooringFabricBakedModel(model, block)
                    }
                }
                model
            }
        }
    }
}
