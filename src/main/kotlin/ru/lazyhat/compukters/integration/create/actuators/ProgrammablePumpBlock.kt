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

import com.simibubi.create.content.fluids.pump.PumpBlock
import com.simibubi.create.content.fluids.pump.PumpBlockEntity
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty

class ProgrammablePumpBlock(
    properties: Properties,
) : PumpBlock(properties) {
    init {
        registerDefaultState(defaultBlockState().setValue(REVERSED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(REVERSED)
    }

    override fun getRotatedBlockState(
        originalState: BlockState,
        targetedFace: Direction,
    ): BlockState = super.getRotatedBlockState(originalState, targetedFace).cycle(REVERSED)

    override fun getBlockEntityType(): BlockEntityType<out PumpBlockEntity> = ProgrammableActuators.PUMP_ENTITY.get()

    companion object {
        val REVERSED: BooleanProperty = BooleanProperty.create("reversed")

        fun withReversed(
            state: BlockState,
            reversed: Boolean,
        ): BlockState =
            if (state.getValue(REVERSED) == reversed) {
                state
            } else {
                state.setValue(REVERSED, reversed).setValue(FACING, state.getValue(FACING).opposite)
            }
    }
}
