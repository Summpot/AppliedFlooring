package io.github.summpot.appliedflooring.client

import dev.architectury.event.EventResult
import dev.architectury.event.events.client.ClientGuiEvent
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.event.events.client.ClientRawInputEvent
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry
import dev.architectury.registry.menu.MenuRegistry
import io.github.summpot.appliedflooring.client.gui.ElevatorFloorHud
import io.github.summpot.appliedflooring.client.gui.MELaserConnectorScreen
import io.github.summpot.appliedflooring.client.render.MEElevatorRenderer
import io.github.summpot.appliedflooring.client.render.MEFlooringBlockEntityRenderer
import io.github.summpot.appliedflooring.client.render.MELaserConnectorRenderer
import io.github.summpot.appliedflooring.registry.ModBlockEntities
import io.github.summpot.appliedflooring.registry.ModMenus
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

object MEFlooringClient {
    fun init() {
        ModMenus.ME_LASER_CONNECTOR_MENU.listen { menuType ->
            MenuRegistry.registerScreenFactory(menuType) { menu, inv, title ->
                MELaserConnectorScreen(menu, inv, title)
            }
        }

        ClientTickEvent.CLIENT_POST.register {
            ElevatorFloorHud.tick()
        }
        ClientGuiEvent.RENDER_HUD.register { graphics, _ ->
            ElevatorFloorHud.render(graphics)
        }
        ClientRawInputEvent.MOUSE_SCROLLED.register { _, _, amountY ->
            if (ElevatorFloorHud.onScroll(amountY)) {
                EventResult.interruptFalse()
            } else {
                EventResult.pass()
            }
        }

        ClientLifecycleEvent.CLIENT_SETUP.register {
            BlockEntityRendererRegistry.register(
                ModBlockEntities.ME_FLOORING_BE.get(),
                BlockEntityRendererProvider { context -> MEFlooringBlockEntityRenderer(context) }
            )
            BlockEntityRendererRegistry.register(
                ModBlockEntities.ME_LASER_CONNECTOR_BE.get(),
                BlockEntityRendererProvider { context -> MELaserConnectorRenderer(context) }
            )
            BlockEntityRendererRegistry.register(
                ModBlockEntities.ME_ELEVATOR_BE.get(),
                BlockEntityRendererProvider { context -> MEElevatorRenderer(context) }
            )
        }
    }
}