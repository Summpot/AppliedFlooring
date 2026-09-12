package io.github.summpot.appliedflooring

import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import io.github.summpot.appliedflooring.block.MEElevatorBlock
import io.github.summpot.appliedflooring.client.MEFlooringClient
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import io.github.summpot.appliedflooring.registry.ModBlocks
import io.github.summpot.appliedflooring.registry.ModCreativeTabs
import io.github.summpot.appliedflooring.registry.ModItems
import io.github.summpot.appliedflooring.util.ElevatorTeleportHelper
import io.github.summpot.appliedflooring.util.ElevatorTracker
import io.github.summpot.appliedflooring.util.isOnGroundCompat
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory

object AppliedFlooringMod {
    const val MOD_ID = "appliedflooring"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun init() {
        LOGGER.info("Initializing Applied Flooring Mod...")
        ModBlocks.register()
        ModItems.register()
        ModBlockEntities.register()
        ModCreativeTabs.register()
        io.github.summpot.appliedflooring.registry.ModMenus.register()
        io.github.summpot.appliedflooring.network.LaserConnectorNetwork.init()
        io.github.summpot.appliedflooring.network.ElevatorNetwork.init()

        EnvExecutor.runInEnv(Env.CLIENT) {
            Runnable {
                MEFlooringClient.init()
            }
        }

        dev.architectury.event.events.common.InteractionEvent.RIGHT_CLICK_BLOCK.register { player, hand, pos, face ->
            if (player.isSpectator || hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return@register dev.architectury.event.EventResult.pass()
            }
            val stack = player.getItemInHand(hand)
            if (!io.github.summpot.appliedflooring.util.FlooringWrenchHelper.isWrench(stack) || !player.isShiftKeyDown) {
                return@register dev.architectury.event.EventResult.pass()
            }
            val lvl = player.commandSenderWorld
            val be = lvl.getBlockEntity(pos)
            if (be is io.github.summpot.appliedflooring.blockentity.MEFlooringBlockEntity) {
                val eyePos = player.getEyePosition(1.0f)
                val viewVec = player.getViewVector(1.0f)
                val reach = 5.0
                val hitVec = eyePos.add(viewVec.scale(reach))
                val clipResult = lvl.clip(
                    net.minecraft.world.level.ClipContext(
                        eyePos, hitVec,
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                    )
                )
                val res = be.disassembleWithWrench(player, lvl, clipResult.location, stack)
                if (res.consumesAction()) {
                    return@register dev.architectury.event.EventResult.interruptTrue()
                }
            }
            return@register dev.architectury.event.EventResult.pass()
        }

        dev.architectury.event.events.common.TickEvent.PLAYER_POST.register { player ->
            if (player is ServerPlayer) {
                val uuid = player.uuid
                ElevatorTracker.tickCooldown(uuid)
                val cooldown = ElevatorTracker.getCooldown(uuid)

                val onGround = player.isOnGroundCompat()
                val wasOnGround = ElevatorTracker.wasOnGround(uuid)

                if (cooldown == 0) {
                    val lvl = player.commandSenderWorld
                    val below = player.blockPosition().below()
                    val state = lvl.getBlockState(below)
                    if (state.block is MEElevatorBlock) {
                        // Shift is handled client-side as a floor picker; jump still goes up
                        // unless the player is sprinting or moving near sprint speed.
                        val jumpingUp = wasOnGround && !onGround &&
                            player.deltaMovement.y > 0.05 &&
                            player.fallDistance <= 0.1f &&
                            !player.isShiftKeyDown
                        if (jumpingUp && !ElevatorTracker.isFastHorizontal(
                                player.deltaMovement.x,
                                player.deltaMovement.z,
                                player.isSprinting
                            )
                        ) {
                            ElevatorTeleportHelper.tryTeleport(player, true)
                        }
                    }
                }
                ElevatorTracker.setOnGround(uuid, onGround)
            }
        }

        LOGGER.info("Applied Flooring Mod initialized successfully.")
    }
}
