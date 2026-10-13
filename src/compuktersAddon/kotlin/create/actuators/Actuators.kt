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

package create.actuators

import compukter.peripheral.Peripheral
import compukter.peripheral.TypedPeripheralProvider

/** A programmable mechanical pump; reversing flow does not reverse its kinetic input. */
public value class Pump internal constructor(private val handle: Int) : Peripheral {
    public companion object : TypedPeripheralProvider<Pump>("create:pump") {
        override fun wrap(handle: Int): Pump = Pump(handle)
    }

    public fun reversed(): Boolean = ActuatorBindings.pumpReversed(handle)

    public fun setReversed(reversed: Boolean): Boolean = ActuatorBindings.setPumpReversed(handle, reversed)
}

/** A normally disengaged programmable clutch. Closing the final handle disengages it. */
public value class Clutch internal constructor(private val handle: Int) : Peripheral {
    public companion object : TypedPeripheralProvider<Clutch>("create:clutch") {
        override fun wrap(handle: Int): Clutch = Clutch(handle)
    }

    public fun connected(): Boolean = ActuatorBindings.clutchConnected(handle)

    public fun setConnected(connected: Boolean): Boolean = ActuatorBindings.setClutchConnected(handle, connected)
}

private object ActuatorBindings {
    external fun pumpReversed(handle: Int): Boolean

    external fun setPumpReversed(handle: Int, reversed: Boolean): Boolean

    external fun clutchConnected(handle: Int): Boolean

    external fun setClutchConnected(handle: Int, connected: Boolean): Boolean
}
