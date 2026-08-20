package io.github.summpot.appliedflooring

import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import io.github.summpot.appliedflooring.client.MEFlooringClient
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import io.github.summpot.appliedflooring.registry.ModBlocks
import io.github.summpot.appliedflooring.registry.ModCreativeTabs
import io.github.summpot.appliedflooring.registry.ModItems
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

        EnvExecutor.runInEnv(Env.CLIENT) {
            Runnable {
                dev.architectury.event.events.client.ClientLifecycleEvent.CLIENT_SETUP.register {
                    MEFlooringClient.init()
                }
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

        LOGGER.info("Applied Flooring Mod initialized successfully.")
    }
}
