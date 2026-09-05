package io.github.summpot.appliedflooring.client.dynamic

import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.pack.PackMetadataSection
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.repository.RepositorySource
import java.util.function.Consumer

object AppliedFlooringPackSource : RepositorySource {
    override fun loadPacks(consumer: Consumer<Pack>, constructor: Pack.PackConstructor) {
        val title = Component.literal("Applied Flooring Dynamic Textures")
        val pack = Pack(
            "appliedflooring:virtual_textures",
            true, // required
            { AppliedFlooringVirtualPack1192() },
            title,
            title,
            PackCompatibility.COMPATIBLE,
            Pack.Position.BOTTOM,
            true, // fixedPosition
            PackSource.BUILT_IN
        )
        consumer.accept(pack)
    }
}
