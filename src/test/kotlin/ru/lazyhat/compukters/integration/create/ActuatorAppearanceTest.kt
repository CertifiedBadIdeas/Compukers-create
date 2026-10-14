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

package ru.lazyhat.compukters.integration.create

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ActuatorAppearanceTest {
    private fun resource(path: String): JsonObject =
        checkNotNull(javaClass.getResourceAsStream("/assets/compukters_create/$path"))
            .bufferedReader()
            .use { JsonParser.parseReader(it).asJsonObject }

    @Test
    fun `reversing flow preserves body and panel orientation on every axis`() {
        val parts = resource("blockstates/programmable_pump.json").getAsJsonArray("multipart").map { it.asJsonObject }
        val opposites = mapOf("up" to "down", "down" to "up", "north" to "south", "south" to "north", "east" to "west", "west" to "east")
        for ((facing, opposite) in opposites) {
            fun applications(
                direction: String,
                reversed: Boolean,
            ): List<JsonObject> =
                parts
                    .filter {
                        val condition = it.getAsJsonObject("when")
                        condition["facing"].asString == direction && condition["reversed"].asBoolean == reversed
                    }.map { it.getAsJsonObject("apply") }
            val normal = applications(facing, false)
            val reversed = applications(opposite, true)
            assertEquals(2, normal.size)
            assertEquals(2, reversed.size)
            assertEquals(normal[0], reversed[0], facing)
            val normalPanel = normal[1].deepCopy().apply { remove("model") }
            val reversedPanel = reversed[1].deepCopy().apply { remove("model") }
            assertEquals(normalPanel, reversedPanel, facing)
            assertNotEquals(normal[1]["model"], reversed[1]["model"])
        }
    }

    @Test
    fun `flow reversal changes only the image on the display`() {
        fun elements(name: String) = resource("models/block/$name.json").getAsJsonArray("elements").map { it.asJsonObject }
        val normal = elements("pump_controller")
        val reversed = elements("pump_controller_reversed")
        assertEquals(normal.size, reversed.size)
        normal.zip(reversed).forEach { (forward, backward) ->
            if (forward["name"].asString.startsWith("Flow arrow")) {
                val center = if (forward.getAsJsonArray("from")[1].asDouble < 3.0) 1.5 else 14.5
                assertEquals(2 * center, forward.getAsJsonArray("from")[1].asDouble + backward.getAsJsonArray("to")[1].asDouble)
                assertEquals(2 * center, forward.getAsJsonArray("to")[1].asDouble + backward.getAsJsonArray("from")[1].asDouble)
                assertEquals(forward.getAsJsonObject("faces"), backward.getAsJsonObject("faces"))
            } else {
                assertEquals(forward, backward)
            }
        }
    }

    @Test
    fun `display indicators have no thickness or side faces`() {
        for (model in listOf(
            "block/pump_controller",
            "block/pump_controller_reversed",
            "block/clutch_controller_open",
            "block/clutch_controller_closed",
            "item/programmable_pump",
            "item/programmable_clutch",
        )) {
            val indicators =
                resource("models/$model.json").getAsJsonArray("elements").map { it.asJsonObject }.filter {
                    it["name"]?.asString?.let { name ->
                        name.startsWith("Flow arrow") ||
                            name in listOf("Input contact", "Output contact", "Open bridge", "Closed bridge")
                    } ==
                        true
                }
            assertTrue(indicators.isNotEmpty(), model)
            for (indicator in indicators) {
                assertEquals(indicator.getAsJsonArray("from")[2], indicator.getAsJsonArray("to")[2], model)
                assertEquals(setOf("south"), indicator.getAsJsonObject("faces").keySet(), model)
            }
        }
    }

    @Test
    fun `pump arrows are centered on compact end displays without a socket`() {
        for (model in listOf("block/pump_controller", "block/pump_controller_reversed", "item/programmable_pump")) {
            val elements = resource("models/$model.json").getAsJsonArray("elements").map { it.asJsonObject }
            assertTrue(elements.none { it["name"]?.asString?.startsWith("Socket") == true })
            val screens = elements.filter { it["name"]?.asString == "Display screen" }
            assertEquals(2, screens.size)
            for (screen in screens) {
                val low = screen.getAsJsonArray("from")
                val high = screen.getAsJsonArray("to")
                val center = (low[1].asDouble + high[1].asDouble) / 2
                assertTrue(center == 1.5 || center == 14.5)
                assertTrue(high[2].asDouble < 14.1)
                assertEquals(2.0, high[1].asDouble - low[1].asDouble)
                val arrow =
                    elements.filter {
                        it["name"]?.asString?.startsWith("Flow arrow") == true &&
                            it.getAsJsonArray("from")[1].asDouble >= low[1].asDouble &&
                            it.getAsJsonArray("to")[1].asDouble <= high[1].asDouble
                    }
                assertTrue(arrow.isNotEmpty())

                fun minimum(axis: Int) = arrow.minOf { it.getAsJsonArray("from")[axis].asDouble }

                fun maximum(axis: Int) = arrow.maxOf { it.getAsJsonArray("to")[axis].asDouble }
                assertEquals(8.0, (minimum(0) + maximum(0)) / 2)
                assertEquals(center, (minimum(1) + maximum(1)) / 2)
                for ((index, first) in arrow.withIndex()) {
                    for (second in arrow.drop(index + 1)) {
                        fun overlap(axis: Int): Double =
                            minOf(first.getAsJsonArray("to")[axis].asDouble, second.getAsJsonArray("to")[axis].asDouble) -
                                maxOf(first.getAsJsonArray("from")[axis].asDouble, second.getAsJsonArray("from")[axis].asDouble)
                        assertTrue(overlap(0) <= 0.0 || overlap(1) <= 0.0, "Arrow strokes overlap on $model")
                    }
                }
            }
        }
    }

    @Test
    fun `clutch switches only its controller indicator`() {
        val parts = resource("blockstates/programmable_clutch.json").getAsJsonArray("multipart").map { it.asJsonObject }
        for (axis in listOf("x", "y", "z")) {
            fun body(powered: Boolean) =
                parts
                    .single {
                        val condition = it.getAsJsonObject("when")
                        condition["axis"].asString == axis && condition["powered"].asBoolean == powered &&
                            it.getAsJsonObject("apply")["model"].asString.endsWith("/clutch_body")
                    }.getAsJsonObject("apply")
            assertEquals(body(false), body(true), axis)
        }
    }
}
