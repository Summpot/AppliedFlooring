package io.github.summpot.appliedflooring.util

import net.minecraft.world.entity.Entity

fun Entity.isOnGroundCompat(): Boolean = this.onGround()
