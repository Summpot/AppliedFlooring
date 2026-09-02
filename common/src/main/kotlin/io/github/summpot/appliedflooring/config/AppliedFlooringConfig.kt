package io.github.summpot.appliedflooring.config

/**
 * AppliedFlooring configuration values used by common code.
 *
 * This object is version-agnostic and compiled into every Minecraft target.
 * Platform modules (Fabric via ForgeConfigApiPort, Forge, NeoForge) define and register
 * their respective ModConfig/ForgeConfigSpec and apply loaded values here.
 */
object AppliedFlooringConfig {
    @Volatile var elevatorCooldownTicks: Int = 4
    @Volatile var elevatorEnergyCost: Double = 500.0
    @Volatile var elevatorMaxDistance: Int = 64
    @Volatile var elevatorAllowFallbackFlooring: Boolean = true
    @Volatile var laserIdleBaseEnergy: Double = 8.0
    @Volatile var laserIdleDistanceEnergy: Double = 0.5
    @Volatile var laserMaxDistance: Int = 64
    @Volatile var laserRemotePlaceEnergy: Double = 500.0
    @Volatile var laserBuilderEnergyPerBlock: Double = 20.0
    @Volatile var laserBuilderMaxRadius: Int = 16
    @Volatile var laserBuilderTickInterval: Int = 2
    @Volatile var laserBuilderBlocksPerTick: Int = 1

    @JvmStatic
    fun apply(
        elevatorCooldownTicks: Int,
        elevatorEnergyCost: Double,
        elevatorMaxDistance: Int,
        elevatorAllowFallbackFlooring: Boolean,
        laserIdleBaseEnergy: Double,
        laserIdleDistanceEnergy: Double,
        laserMaxDistance: Int,
        laserRemotePlaceEnergy: Double = 500.0,
        laserBuilderEnergyPerBlock: Double = 20.0,
        laserBuilderMaxRadius: Int = 16,
        laserBuilderTickInterval: Int = 2,
        laserBuilderBlocksPerTick: Int = 1
    ) {
        this.elevatorCooldownTicks = elevatorCooldownTicks.coerceAtLeast(1)
        this.elevatorEnergyCost = elevatorEnergyCost.coerceAtLeast(0.0)
        this.elevatorMaxDistance = elevatorMaxDistance.coerceIn(4, 256)
        this.elevatorAllowFallbackFlooring = elevatorAllowFallbackFlooring
        this.laserIdleBaseEnergy = laserIdleBaseEnergy.coerceAtLeast(0.0)
        this.laserIdleDistanceEnergy = laserIdleDistanceEnergy.coerceAtLeast(0.0)
        this.laserMaxDistance = laserMaxDistance.coerceIn(4, 256)
        this.laserRemotePlaceEnergy = laserRemotePlaceEnergy.coerceAtLeast(0.0)
        this.laserBuilderEnergyPerBlock = laserBuilderEnergyPerBlock.coerceAtLeast(0.0)
        this.laserBuilderMaxRadius = laserBuilderMaxRadius.coerceIn(1, 32)
        this.laserBuilderTickInterval = laserBuilderTickInterval.coerceAtLeast(1)
        this.laserBuilderBlocksPerTick = laserBuilderBlocksPerTick.coerceIn(1, 16)
    }
}
