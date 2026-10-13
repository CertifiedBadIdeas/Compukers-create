/*
 * The Compukters Developers
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 * Licensed under the Apache License, Version 2.0.
 */

package ru.lazyhat.compukters.integration.create.gametest

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.fluids.pump.PumpBlock
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import ru.lazyhat.compukters.impl.registry.CompuktersRegistry
import ru.lazyhat.compukters.integration.create.actuators.ProgrammableActuators
import ru.lazyhat.compukters.integration.create.actuators.ProgrammableClutchBlockEntity
import ru.lazyhat.compukters.integration.create.actuators.ProgrammablePumpBlock

@PrefixGameTestTemplate(false)
object CreateActuatorGameTests {
    @JvmStatic
    @GameTest(batch = "create_actuators", template = "bastion/mobs/empty", templateNamespace = "minecraft", timeoutTicks = 1_000_000)
    fun guestControlAndDisconnect(helper: GameTestHelper) {
        val computer = BlockPos(1, 2, 1)
        val clutch = BlockPos(4, 2, 3)
        val pump = BlockPos(4, 2, 5)
        val output = clutch.east()
        helper.setBlock(computer, CompuktersRegistry.COMPUTER.get())
        helper.setBlock(
            clutch,
            ProgrammableActuators.CLUTCH
                .get()
                .defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X),
        )
        helper.setBlock(output, AllBlocks.SHAFT.defaultState.setValue(BlockStateProperties.AXIS, Direction.Axis.X))
        helper.setBlock(clutch.west(), AllBlocks.CREATIVE_MOTOR.defaultState.setValue(DirectionalKineticBlock.FACING, Direction.EAST))
        helper.setBlock(
            pump,
            ProgrammableActuators.PUMP
                .get()
                .defaultBlockState()
                .setValue(PumpBlock.FACING, Direction.EAST),
        )
        val guest = GuestComputerScenario(helper, computer)
        val sequence = helper.startSequence()
        sequence.thenExecuteAfter(10) {
            helper.assertTrue(
                !(helper.getBlockEntity(clutch) as ProgrammableClutchBlockEntity).connected(),
                "new clutch must be disengaged",
            )
            helper.assertTrue((helper.getBlockEntity(output) as KineticBlockEntity).speed == 0f, "default clutch transmitted rotation")
            PeripheralNetworkGameTestFixtures.bind(helper, *listOf(computer, clutch, pump).map(helper::absolutePos).toTypedArray())
            CreatePeripheralGameTests.nameDevice(helper, clutch, "load")
            CreatePeripheralGameTests.nameDevice(helper, pump, "fuel")
        }
        guest.prepare(sequence, SOURCE)
        guest.awaitMarker(sequence, "actuators-connected")
        sequence
            .thenWaitUntil {
                helper.assertTrue(
                    (helper.getBlockEntity(output) as KineticBlockEntity).speed != 0f,
                    "program did not engage kinetic output",
                )
            }.thenExecute {
                helper.assertTrue(
                    helper.getBlockState(pump).getValue(PumpBlock.FACING) == Direction.WEST,
                    "pump flow direction did not reverse",
                )
                helper.setBlock(clutch.above(), Blocks.REDSTONE_BLOCK)
            }.thenExecuteAfter(5) {
                helper.assertTrue(
                    (helper.getBlockEntity(clutch) as ProgrammableClutchBlockEntity).connected(),
                    "redstone changed programmed clutch",
                )
                PeripheralNetworkGameTestFixtures.unbind(helper, helper.absolutePos(computer))
            }.thenWaitUntil {
                helper.assertTrue(
                    !(helper.getBlockEntity(clutch) as ProgrammableClutchBlockEntity).connected(),
                    "lost connection did not release clutch without a guest call",
                )
                helper.assertTrue((helper.getBlockEntity(output) as KineticBlockEntity).speed == 0f, "disengaged clutch still drove output")
            }
        guest.resume(sequence)
        guest.awaitMarker(sequence, "actuators-stale")
        sequence.thenExecute {
            PeripheralNetworkGameTestFixtures.bind(helper, *listOf(clutch, pump, computer).map(helper::absolutePos).toTypedArray())
        }
        guest.resume(sequence)
        guest.awaitMarker(sequence, "actuators-reacquired")
        sequence.thenWaitUntil {
            helper.assertTrue((helper.getBlockEntity(output) as KineticBlockEntity).speed != 0f, "reacquired clutch did not drive output")
        }
        guest.resume(sequence)
        // Ending the program releases the lease while preserving the pump state.
        sequence
            .thenWaitUntil {
                helper.assertTrue(
                    !(helper.getBlockEntity(clutch) as ProgrammableClutchBlockEntity).connected(),
                    "program exit did not release clutch",
                )
                helper.assertTrue((helper.getBlockEntity(output) as KineticBlockEntity).speed == 0f, "program exit left output running")
                helper.assertTrue(
                    helper.getBlockState(pump).getValue(ProgrammablePumpBlock.REVERSED),
                    "pump direction was reset with program",
                )
            }.thenSucceed()
    }

    @JvmStatic
    @GameTest(batch = "create_actuators", template = "bastion/mobs/empty", templateNamespace = "minecraft", timeoutTicks = 1_000_000)
    fun stoppedProgramDisengagesClutch(helper: GameTestHelper) {
        val computer = BlockPos(1, 2, 1)
        val clutch = BlockPos(3, 2, 1)
        helper.setBlock(computer, CompuktersRegistry.COMPUTER.get())
        helper.setBlock(clutch, ProgrammableActuators.CLUTCH.get())
        val guest = GuestComputerScenario(helper, computer)
        val sequence = helper.startSequence()
        sequence.thenExecuteAfter(5) {
            PeripheralNetworkGameTestFixtures.bind(helper, helper.absolutePos(computer), helper.absolutePos(clutch))
            CreatePeripheralGameTests.nameDevice(helper, clutch, "load")
        }
        guest.prepare(
            sequence,
            """
            import create.actuators.Clutch
            import compukter.terminal.Terminal
            fun main() {
                check(Clutch.named("load").setConnected(true))
                Terminal.write("clutch-running\n")
                readln()
            }
            """.trimIndent(),
        )
        guest.awaitMarker(sequence, "clutch-running")
        sequence.thenExecute {
            helper.assertTrue((helper.getBlockEntity(clutch) as ProgrammableClutchBlockEntity).connected(), "guest did not engage clutch")
        }
        guest.stop(sequence)
        sequence
            .thenWaitUntil {
                helper.assertTrue(
                    !(helper.getBlockEntity(clutch) as ProgrammableClutchBlockEntity).connected(),
                    "foreground cancellation left clutch engaged",
                )
            }.thenSucceed()
    }

    @JvmStatic
    @GameTest(batch = "create_actuators", template = "bastion/mobs/empty", templateNamespace = "minecraft")
    fun leaseReleaseAndReplacement(helper: GameTestHelper) {
        val pos = BlockPos(2, 2, 2)
        val state = ProgrammableActuators.CLUTCH.get().defaultBlockState()
        helper.setBlock(pos, state)
        helper
            .startSequence()
            .thenExecuteAfter(5) {
                val entity = helper.getBlockEntity(pos) as ProgrammableClutchBlockEntity
                val first = Any()
                val second = Any()
                entity.acquire(first)
                entity.acquire(second)
                helper.assertTrue(entity.setConnected(first, true), "owner failed to engage clutch")
                entity.release(first)
                helper.assertTrue(entity.connected(), "first release interrupted another retained lease")
                entity.release(second)
                helper.assertTrue(!entity.connected(), "last release must disengage clutch")
                entity.acquire(first)
                entity.setConnected(first, true)
                entity.onLoad()
                helper.assertTrue(!entity.connected(), "reload must discard transient leases")
                entity.acquire(second)
                entity.setConnected(second, true)
                entity.onChunkUnloaded()
                helper.assertTrue(!entity.connected(), "chunk unload did not disengage clutch")
                entity.acquire(second)
                entity.setConnected(second, true)
                helper.setBlock(pos, Blocks.AIR)
                helper.setBlock(pos, state)
                val replacement = helper.getBlockEntity(pos) as ProgrammableClutchBlockEntity
                val replacementToken = Any()
                replacement.acquire(replacementToken)
                replacement.setConnected(replacementToken, true)
                entity.release(second)
                helper.assertTrue(replacement.connected(), "old endpoint modified replacement entity")
                replacement.release(replacementToken)
            }.thenSucceed()
    }

    @JvmStatic
    @GameTest(batch = "create_actuators", template = "bastion/mobs/empty", templateNamespace = "minecraft", timeoutTicks = 500)
    fun pumpReversesActualFluidAtSameSpeed(helper: GameTestHelper) {
        val pump = BlockPos(4, 2, 4)
        val west = pump.west(2)
        val east = pump.east(2)
        helper.setBlock(west, AllBlocks.FLUID_TANK.get())
        helper.setBlock(east, AllBlocks.FLUID_TANK.get())
        helper.setBlock(pump.west(), AllBlocks.FLUID_PIPE.get())
        helper.setBlock(pump.east(), AllBlocks.FLUID_PIPE.get())
        helper.setBlock(
            pump,
            ProgrammableActuators.PUMP
                .get()
                .defaultBlockState()
                .setValue(PumpBlock.FACING, Direction.EAST),
        )

        fun tank(position: BlockPos): IFluidHandler =
            checkNotNull(helper.level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(position), Direction.UP))
        var eastBeforeReverse = 0
        helper
            .startSequence()
            .thenExecuteAfter(10) {
                tank(west).fill(FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE)
                val entity = helper.getBlockEntity(pump) as KineticBlockEntity
                entity.setSpeed(64f)
                entity.onSpeedChanged(0f)
            }.thenWaitUntil {
                helper.assertTrue(tank(east).getFluidInTank(0).amount >= 1000, "pump did not transfer water towards facing")
            }.thenExecute {
                eastBeforeReverse = tank(east).getFluidInTank(0).amount
                val state = helper.getBlockState(pump)
                helper.setBlock(pump, ProgrammablePumpBlock.withReversed(state, true))
                helper.assertTrue((helper.getBlockEntity(pump) as KineticBlockEntity).speed == 64f, "reversing flow changed rotation speed")
            }.thenWaitUntil {
                helper.assertTrue(
                    tank(east).getFluidInTank(0).amount < eastBeforeReverse,
                    "reversed pump did not withdraw water from destination",
                )
            }.thenSucceed()
    }

    private val SOURCE =
        """
        import create.actuators.Pump
        import create.actuators.Clutch
        import compukter.terminal.Terminal
        fun main() {
            val pump = Pump.named("fuel")
            val clutch = Clutch.named("load")
            check(Pump.first() == pump)
            check(Clutch.all().size == 1)
            check(!clutch.connected())
            check(pump.setReversed(true))
            check(pump.setReversed(true))
            check(pump.reversed())
            check(clutch.setConnected(true))
            Terminal.write("actuators-connected\n")
            readln()
            var stale = false
            try { clutch.connected() } catch (e: compukter.io.IOException) { stale = true }
            check(stale)
            Terminal.write("actuators-stale\n")
            readln()
            check(Clutch.named("load").setConnected(true))
            check(Pump.named("fuel").reversed())
            Terminal.write("actuators-reacquired\n")
            readln()
        }
        """.trimIndent()
}
