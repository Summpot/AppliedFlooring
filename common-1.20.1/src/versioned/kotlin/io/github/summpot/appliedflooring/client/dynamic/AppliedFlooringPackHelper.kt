package io.github.summpot.appliedflooring.client.dynamic

import net.minecraft.SharedConstants
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.repository.RepositorySource
import net.minecraft.world.flag.FeatureFlagSet
import java.util.function.Consumer

object AppliedFlooringPackSource : RepositorySource {
    override fun loadPacks(consumer: Consumer<Pack>) {
        val title = Component.literal("Applied Flooring Dynamic Textures")
        val packVersion = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES)
        val info = Pack.Info(
            title,
            packVersion,
            FeatureFlagSet.of()
        )
        val pack = Pack.create(
            "appliedflooring:virtual_textures",
            title,
            true, // required
            { AppliedFlooringVirtualPack1201() },
            info,
            PackType.CLIENT_RESOURCES,
            Pack.Position.BOTTOM,
            true, // fixedPosition
            PackSource.BUILT_IN
        )
        if (pack != null) {
            consumer.accept(pack)
        }
    }
}
