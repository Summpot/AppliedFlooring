package io.github.summpot.appliedflooring.util

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ElevatorTracker {
    private val cooldowns = ConcurrentHashMap<UUID, Int>()
    private val wasOnGround = ConcurrentHashMap<UUID, Boolean>()

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

    fun removePlayer(uuid: UUID) {
        cooldowns.remove(uuid)
        wasOnGround.remove(uuid)
    }
}
