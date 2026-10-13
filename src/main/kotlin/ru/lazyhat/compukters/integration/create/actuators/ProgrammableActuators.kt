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

import com.simibubi.create.AllBlocks
import com.simibubi.create.api.stress.BlockStressValues
import com.simibubi.create.content.fluids.pump.PumpBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersAddonRegistry
import ru.lazyhat.compukters.integration.create.CompuktersCreateMod
import java.util.function.Supplier

object ProgrammableActuators {
    private val blocks = DeferredRegister.createBlocks(CompuktersCreateMod.MOD_ID)
    private val items = DeferredRegister.createItems(CompuktersCreateMod.MOD_ID)
    private val entities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CompuktersCreateMod.MOD_ID)

    val PUMP: DeferredBlock<ProgrammablePumpBlock> =
        blocks.register(
            "programmable_pump",
            Supplier {
                ProgrammablePumpBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.MECHANICAL_PUMP.get()))
            },
        )
    val CLUTCH: DeferredBlock<ProgrammableClutchBlock> =
        blocks.register(
            "programmable_clutch",
            Supplier {
                ProgrammableClutchBlock(BlockBehaviour.Properties.ofFullCopy(AllBlocks.CLUTCH.get()))
            },
        )
    val PUMP_ITEM: DeferredItem<BlockItem> = items.registerSimpleBlockItem(PUMP)
    val CLUTCH_ITEM: DeferredItem<BlockItem> = items.registerSimpleBlockItem(CLUTCH)
    val PUMP_ENTITY: DeferredHolder<BlockEntityType<*>, BlockEntityType<PumpBlockEntity>> =
        entities.register(
            "programmable_pump",
            Supplier {
                BlockEntityType.Builder.of({ pos, state -> PumpBlockEntity(PUMP_ENTITY.get(), pos, state) }, PUMP.get()).build(null)
            },
        )
    val CLUTCH_ENTITY: DeferredHolder<BlockEntityType<*>, BlockEntityType<ProgrammableClutchBlockEntity>> =
        entities.register(
            "programmable_clutch",
            Supplier {
                BlockEntityType.Builder.of({ pos, state -> ProgrammableClutchBlockEntity(pos, state) }, CLUTCH.get()).build(null)
            },
        )

    fun register(bus: IEventBus) {
        blocks.register(bus)
        items.register(bus)
        entities.register(bus)
        bus.addListener(::setup)
        CompuktersAddonRegistry.registerCreativeItems("create") { listOf(PUMP_ITEM.get(), CLUTCH_ITEM.get()) }
    }

    private fun setup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            BlockStressValues.IMPACTS.register(PUMP.get()) { BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get()) }
            BlockStressValues.IMPACTS.register(CLUTCH.get()) { BlockStressValues.getImpact(AllBlocks.CLUTCH.get()) }
        }
    }
}
