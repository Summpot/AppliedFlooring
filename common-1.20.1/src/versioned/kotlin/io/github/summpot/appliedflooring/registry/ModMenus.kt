package io.github.summpot.appliedflooring.registry

import dev.architectury.registry.menu.MenuRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.menu.MELaserConnectorMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType

object ModMenus {
    val MENUS: DeferredRegister<MenuType<*>> = DeferredRegister.create(
        AppliedFlooringMod.MOD_ID,
        Registries.MENU
    )

    val ME_LASER_CONNECTOR_MENU: RegistrySupplier<MenuType<MELaserConnectorMenu>> = MENUS.register("me_laser_connector") {
        MenuRegistry.ofExtended { containerId, inv, buf ->
            MELaserConnectorMenu(containerId, inv, buf)
        }
    }

    fun register() {
        MENUS.register()
    }
}
