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

import com.simibubi.create.content.kinetics.transmission.ClutchBlock
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class ProgrammableClutchBlock(
    properties: Properties,
) : ClutchBlock(properties) {
    init {
        registerDefaultState(defaultBlockState().setValue(POWERED, true))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? =
        super.getStateForPlacement(context)?.setValue(POWERED, true)

    // POWERED describes disengagement here, not the surrounding redstone signal.
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        other: Block,
        neighbor: BlockPos,
        moving: Boolean,
    ) {}

    override fun getBlockEntityType(): BlockEntityType<out SplitShaftBlockEntity> = ProgrammableActuators.CLUTCH_ENTITY.get()
}
