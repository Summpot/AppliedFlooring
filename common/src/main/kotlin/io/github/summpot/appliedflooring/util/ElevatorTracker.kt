package io.github.summpot.appliedflooring.util

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ElevatorTracker {
    private val cooldowns = ConcurrentHashMap<UUID, Int>()
    private val wasOnGround = ConcurrentHashMap<UUID, Boolean>()

    /**
     * Horizontal speed (blocks/tick) above which a jump is treated as travel, not "go up".
     * Vanilla walk is ~0.216, sprint is ~0.281.
     */
    const val UPWARD_MAX_HORIZONTAL_SPEED = 0.24

    fun getCooldown(uuid: UUID): Int = cooldowns.getOrDefault(uuid, 0)

    fun setCooldown(uuid: UUID, ticks: Int) {
        cooldowns[uuid] = ticks
    }

    fun tickCooldown(uuid: UUID) {
        val current = cooldowns[uuid] ?: 0
        if (current > 0) {
            cooldowns[uuid] = current - 1
        }
    }

    fun wasOnGround(uuid: UUID): Boolean = wasOnGround.getOrDefault(uuid, false)

    fun setOnGround(uuid: UUID, onGround: Boolean) {
        wasOnGround[uuid] = onGround
    }

    fun isFastHorizontal(deltaX: Double, deltaZ: Double, sprinting: Boolean): Boolean {
        if (sprinting) return true
        val limit = UPWARD_MAX_HORIZONTAL_SPEED
        return deltaX * deltaX + deltaZ * deltaZ > limit * limit
    }

    fun removePlayer(uuid: UUID) {
        cooldowns.remove(uuid)
        wasOnGround.remove(uuid)
    }
}
