/*
 * The Compukters Developers
 *
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.lazyhat.compukters.integration.create.actuators

import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.fluids.pump.PumpRenderer
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import ru.lazyhat.compukters.integration.create.CompuktersCreateMod

@EventBusSubscriber(modid = CompuktersCreateMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object CreateActuatorClient {
    @JvmStatic
    @SubscribeEvent
    fun renderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(ProgrammableActuators.PUMP_ENTITY.get(), ::PumpRenderer)
        event.registerBlockEntityRenderer(ProgrammableActuators.CLUTCH_ENTITY.get(), ::SplitShaftRenderer)
    }

    @JvmStatic
    @SubscribeEvent
    fun setup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            SimpleBlockEntityVisualizer
                .builder(ProgrammableActuators.PUMP_ENTITY.get())
                .factory(SingleAxisRotatingVisual.ofZ(AllPartialModels.MECHANICAL_PUMP_COG))
                .apply()
            SimpleBlockEntityVisualizer
                .builder(ProgrammableActuators.CLUTCH_ENTITY.get())
                .factory { context, entity, tick -> SplitShaftVisual(context, entity, tick) }
                .neverSkipVanillaRender()
                .apply()
        }
    }
}
