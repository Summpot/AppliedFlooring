package io.github.summpot.appliedflooring.neoforge

import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import net.neoforged.neoforge.common.ModConfigSpec

class AppliedFlooringNeoForgeConfig(builder: ModConfigSpec.Builder) {
    private val elevatorCooldownTicks: ModConfigSpec.IntValue
    private val elevatorEnergyCost: ModConfigSpec.DoubleValue
    private val elevatorMaxDistance: ModConfigSpec.IntValue
    private val elevatorAllowFallbackFlooring: ModConfigSpec.BooleanValue
    private val laserIdleBaseEnergy: ModConfigSpec.DoubleValue
    private val laserIdleDistanceEnergy: ModConfigSpec.DoubleValue
    private val laserMaxDistance: ModConfigSpec.IntValue
    private val laserRemotePlaceEnergy: ModConfigSpec.DoubleValue
    private val laserBuilderEnergyPerBlock: ModConfigSpec.DoubleValue
    private val laserBuilderMaxRadius: ModConfigSpec.IntValue
    private val laserBuilderTickInterval: ModConfigSpec.IntValue
    private val laserBuilderBlocksPerTick: ModConfigSpec.IntValue

    init {
        builder.comment("ME Elevator Configuration").push("elevator")
        elevatorCooldownTicks = builder
            .comment("Cooldown in ticks between elevator teleports (default 4 = 0.2s)")
            .defineInRange("cooldown_ticks", 4, 1, 200)
        elevatorEnergyCost = builder
            .comment("Energy cost (AE) per elevator teleport")
            .defineInRange("energy_cost", 500.0, 0.0, 1000000.0)
        elevatorMaxDistance = builder
            .comment("Maximum vertical distance in blocks to search for elevator floors")
            .defineInRange("max_distance", 64, 4, 256)
        elevatorAllowFallbackFlooring = builder
            .comment("Allow teleporting to normal ME flooring blocks when no target elevator exists")
            .define("allow_fallback_flooring", true)
        builder.pop()

        builder.comment("ME Laser Connector Configuration").push("laser")
        laserIdleBaseEnergy = builder
            .comment("Base idle power usage (AE/t) when laser connection is established")
            .defineInRange("idle_base_energy", 8.0, 0.0, 10000.0)
        laserIdleDistanceEnergy = builder
            .comment("Additional idle power usage (AE/t) per block of vertical distance")
            .defineInRange("idle_distance_energy", 0.5, 0.0, 1000.0)
        laserMaxDistance = builder
            .comment("Maximum vertical distance in blocks to connect laser beams")
            .defineInRange("max_distance", 64, 4, 256)
        laserRemotePlaceEnergy = builder
            .comment("Energy cost (AE) to remotely place another layer's laser connector")
            .defineInRange("remote_place_energy", 500.0, 0.0, 1000000.0)
        laserBuilderEnergyPerBlock = builder
            .comment("Energy cost (AE) consumed per floor block placed by auto-builder")
            .defineInRange("builder_energy_per_block", 20.0, 0.0, 100000.0)
        laserBuilderMaxRadius = builder
            .comment("Maximum allowable radius for auto floor construction")
            .defineInRange("builder_max_radius", 16, 1, 32)
        laserBuilderTickInterval = builder
            .comment("Tick interval between floor block placement steps")
            .defineInRange("builder_tick_interval", 2, 1, 100)
        laserBuilderBlocksPerTick = builder
            .comment("Number of blocks placed per step")
            .defineInRange("builder_blocks_per_tick", 1, 1, 16)
        builder.pop()
    }

    fun applyToCommon() {
        AppliedFlooringConfig.apply(
            elevatorCooldownTicks = elevatorCooldownTicks.get(),
            elevatorEnergyCost = elevatorEnergyCost.get(),
            elevatorMaxDistance = elevatorMaxDistance.get(),
            elevatorAllowFallbackFlooring = elevatorAllowFallbackFlooring.get(),
            laserIdleBaseEnergy = laserIdleBaseEnergy.get(),
            laserIdleDistanceEnergy = laserIdleDistanceEnergy.get(),
            laserMaxDistance = laserMaxDistance.get(),
            laserRemotePlaceEnergy = laserRemotePlaceEnergy.get(),
            laserBuilderEnergyPerBlock = laserBuilderEnergyPerBlock.get(),
            laserBuilderMaxRadius = laserBuilderMaxRadius.get(),
            laserBuilderTickInterval = laserBuilderTickInterval.get(),
            laserBuilderBlocksPerTick = laserBuilderBlocksPerTick.get()
        )
    }

    companion object {
        val SPEC: ModConfigSpec
        private val INSTANCE: AppliedFlooringNeoForgeConfig

        init {
            val specPair = ModConfigSpec.Builder().configure(::AppliedFlooringNeoForgeConfig)
            SPEC = specPair.right
            INSTANCE = specPair.left
        }

        fun applyToCommon() {
            INSTANCE.applyToCommon()
        }
    }
}
