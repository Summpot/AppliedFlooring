package io.github.summpot.appliedflooring.util

import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.block.MEElevatorBlock
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.blockentity.MEElevatorBlockEntity
import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

object ElevatorTeleportHelper {

    fun tryTeleport(player: ServerPlayer, up: Boolean): Boolean {
        val level = player.commandSenderWorld as? ServerLevel ?: return false
        val origin = player.blockPosition().below()
        val originState = level.getBlockState(origin)
        val originBlock = originState.block as? MEElevatorBlock ?: return false

        val originBe = level.getBlockEntity(origin) as? MEElevatorBlockEntity ?: return false
        if (!originBe.isPowered()) return false

        val grid = originBe.mainNode.grid ?: return false
        val energyService = grid.energyService ?: return false

        val energyCost = AppliedFlooringConfig.elevatorEnergyCost
        val simulated = energyService.extractAEPower(energyCost, Actionable.SIMULATE, PowerMultiplier.ONE)
        if (simulated < energyCost) return false

        val dir = if (up) 1 else -1
        val maxDist = AppliedFlooringConfig.elevatorMaxDistance

        var destinationPos: BlockPos? = null

        // Pass 1: Search for matching MEElevatorBlock in target direction
        for (dist in 2..maxDist) {
            val candidate = origin.above(dir * dist)
            val candState = level.getBlockState(candidate)
            if (candState.block is MEElevatorBlock) {
                val candBlock = candState.block as MEElevatorBlock
                if (candBlock.color == originBlock.color || originBlock.color == AEColor.TRANSPARENT || candBlock.color == AEColor.TRANSPARENT) {
                    val candBe = level.getBlockEntity(candidate) as? MEElevatorBlockEntity
                    if (candBe != null && candBe.isPowered() && candBe.mainNode.grid == grid) {
                        if (isClearSpace(level, candidate)) {
                            destinationPos = candidate
                            break
                        }
                    }
                }
            }
        }

        // Pass 2: Fallback to regular MEFlooringBlock if enabled and no elevator found in target direction
        if (destinationPos == null && AppliedFlooringConfig.elevatorAllowFallbackFlooring) {
            for (dist in 2..maxDist) {
                val candidate = origin.above(dir * dist)
                val candState = level.getBlockState(candidate)
                if (candState.block is MEFlooringBlock && candState.block !is MEElevatorBlock) {
                    if (isClearSpace(level, candidate)) {
                        destinationPos = candidate
                        break
                    }
                }
            }
        }

        if (destinationPos == null) return false

        // Extract power from origin elevator's ME grid
        energyService.extractAEPower(energyCost, Actionable.MODULATE, PowerMultiplier.ONE)

        // AE2 Spatial IO styled effects at origin
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x + 0.5, origin.y + 1.2, origin.z + 0.5, 25, 0.25, 0.5, 0.25, 0.05)
        level.sendParticles(ParticleTypes.END_ROD, origin.x + 0.5, origin.y + 1.1, origin.z + 0.5, 15, 0.2, 0.5, 0.2, 0.08)
        level.sendParticles(ParticleTypes.GLOW, origin.x + 0.5, origin.y + 1.2, origin.z + 0.5, 10, 0.3, 0.4, 0.3, 0.05)
        level.playSound(null, origin, SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 0.7f, 1.6f)
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.8f)

        // Teleport player
        player.teleportTo(level, destinationPos.x + 0.5, destinationPos.y + 1.0, destinationPos.z + 0.5, player.yRot, player.xRot)
        player.deltaMovement = Vec3.ZERO
        player.fallDistance = 0.0f

        // AE2 Spatial IO styled effects at destination
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, destinationPos.x + 0.5, destinationPos.y + 1.2, destinationPos.z + 0.5, 20, 0.25, 0.5, 0.25, 0.05)
        level.sendParticles(ParticleTypes.END_ROD, destinationPos.x + 0.5, destinationPos.y + 1.1, destinationPos.z + 0.5, 12, 0.2, 0.5, 0.2, 0.05)
        level.sendParticles(ParticleTypes.INSTANT_EFFECT, destinationPos.x + 0.5, destinationPos.y + 1.2, destinationPos.z + 0.5, 8, 0.3, 0.4, 0.3, 0.05)
        level.playSound(null, destinationPos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.7f, 1.5f)
        level.playSound(null, destinationPos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.8f)

        ElevatorTracker.setCooldown(player.uuid, AppliedFlooringConfig.elevatorCooldownTicks)
        return true
    }

    private fun isClearSpace(level: Level, pos: BlockPos): Boolean {
        val space1 = level.getBlockState(pos.above())
        val space2 = level.getBlockState(pos.above(2))
        val clear1 = space1.isAir || space1.material.isReplaceable || !space1.material.isSolid
        val clear2 = space2.isAir || space2.material.isReplaceable || !space2.material.isSolid
        return clear1 && clear2
    }
}
