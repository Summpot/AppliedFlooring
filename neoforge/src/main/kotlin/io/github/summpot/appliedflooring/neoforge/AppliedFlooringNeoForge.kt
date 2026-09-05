package io.github.summpot.appliedflooring.neoforge

import appeng.api.AECapabilities
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent

@Mod(AppliedFlooringMod.MOD_ID)
class AppliedFlooringNeoForge(bus: IEventBus, container: ModContainer) {
    init {
        container.registerConfig(ModConfig.Type.COMMON, AppliedFlooringNeoForgeConfig.SPEC)
        bus.addListener(this::onConfigLoading)
        bus.addListener(this::onConfigReloading)
        AppliedFlooringMod.init()
        bus.addListener(::registerCapabilities)
        bus.addListener(::onAddPackFinders)
    }

    private fun onAddPackFinders(event: net.neoforged.neoforge.event.AddPackFindersEvent) {
        if (event.packType == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {
            event.addRepositorySource { consumer ->
                val pack = net.minecraft.server.packs.repository.Pack(
                    net.minecraft.server.packs.PackLocationInfo(
                        "appliedflooring:virtual_textures",
                        net.minecraft.network.chat.Component.literal("Applied Flooring Dynamic Textures"),
                        net.minecraft.server.packs.repository.PackSource.BUILT_IN,
                        java.util.Optional.empty()
                    ),
                    object : net.minecraft.server.packs.repository.Pack.ResourcesSupplier {
                        override fun openPrimary(info: net.minecraft.server.packs.PackLocationInfo): net.minecraft.server.packs.PackResources {
                            return io.github.summpot.appliedflooring.client.dynamic.AppliedFlooringVirtualPack1211()
                        }
                        override fun openFull(
                            info: net.minecraft.server.packs.PackLocationInfo,
                            metadata: net.minecraft.server.packs.repository.Pack.Metadata
                        ): net.minecraft.server.packs.PackResources {
                            return io.github.summpot.appliedflooring.client.dynamic.AppliedFlooringVirtualPack1211()
                        }
                    },
                    net.minecraft.server.packs.repository.Pack.Metadata(
                        net.minecraft.network.chat.Component.literal("Applied Flooring Dynamic Textures"),
                        net.minecraft.server.packs.repository.PackCompatibility.COMPATIBLE,
                        net.minecraft.world.flag.FeatureFlagSet.of(),
                        emptyList(),
                        true // isHidden = true
                    ),
                    net.minecraft.server.packs.PackSelectionConfig(
                        true, // required = true
                        net.minecraft.server.packs.repository.Pack.Position.BOTTOM,
                        true // fixedPosition = true
                    )
                )
                consumer.accept(pack)
            }
        }
    }

    private fun onConfigLoading(event: ModConfigEvent.Loading) {
        if (event.config.modId == AppliedFlooringMod.MOD_ID) {
            AppliedFlooringNeoForgeConfig.applyToCommon()
        }
    }

    private fun onConfigReloading(event: ModConfigEvent.Reloading) {
        if (event.config.modId == AppliedFlooringMod.MOD_ID) {
            AppliedFlooringNeoForgeConfig.applyToCommon()
        }
    }

    private fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_FLOORING_BE.get()) { be, _ -> be }
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_LASER_CONNECTOR_BE.get()) { be, _ -> be }
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, ModBlockEntities.ME_ELEVATOR_BE.get()) { be, _ -> be }
    }
}