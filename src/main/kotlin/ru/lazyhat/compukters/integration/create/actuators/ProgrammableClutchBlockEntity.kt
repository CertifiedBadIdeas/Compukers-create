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

import com.simibubi.create.content.kinetics.transmission.ClutchBlockEntity
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.Collections
import java.util.IdentityHashMap

class ProgrammableClutchBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : ClutchBlockEntity(ProgrammableActuators.CLUTCH_ENTITY.get(), pos, state) {
    private val leases = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

    fun acquire(token: Any) {
        check(leases.add(token)) { "Clutch lease is already acquired" }
    }

    fun release(token: Any) {
        if (leases.remove(token) && leases.isEmpty()) applyConnected(false)
    }

    fun connected(): Boolean = !blockState.getValue(GearshiftBlock.POWERED)

    fun setConnected(
        token: Any,
        connected: Boolean,
    ): Boolean {
        check(token in leases) { "Clutch control requires an active peripheral lease" }
        applyConnected(connected)
        return connected()
    }

    private fun applyConnected(connected: Boolean) {
        val world = level ?: return
        if (world.isClientSide || isRemoved || !world.hasChunkAt(blockPos) || world.getBlockEntity(blockPos) !== this) return
        val state = world.getBlockState(blockPos)
        val block = state.block as? ProgrammableClutchBlock ?: return
        if (state.getValue(GearshiftBlock.POWERED) == !connected) return
        block.detachKinetics(world, blockPos, true)
        world.setBlock(blockPos, state.setValue(GearshiftBlock.POWERED, !connected), Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE)
        sendData()
    }

    override fun onLoad() {
        if (level?.isClientSide == false) {
            leases.clear()
            applyConnected(false)
        }
        super.onLoad()
    }

    override fun initialize() {
        if (leases.isEmpty()) applyConnected(false)
        super.initialize()
    }

    override fun onChunkUnloaded() {
        leases.clear()
        applyConnected(false)
        super.onChunkUnloaded()
    }

    override fun remove() {
        leases.clear()
        applyConnected(false)
        super.remove()
    }
}
