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
                            name == "Clutch state bar"
                    } ==
                        true
                }
            assertTrue(indicators.isNotEmpty(), model)
            for (indicator in indicators) {
                val face = indicator.getAsJsonObject("faces").keySet().single()
                assertTrue(face in setOf("north", "south", "east", "west"), model)
                val normal = if (face == "north" || face == "south") 2 else 0
                assertEquals(indicator.getAsJsonArray("from")[normal], indicator.getAsJsonArray("to")[normal], model)
            }
        }
    }

    @Test
    fun `three pump arrows fit each end display on all four sides`() {
        for (model in listOf("block/pump_controller", "block/pump_controller_reversed", "item/programmable_pump")) {
            val elements = resource("models/$model.json").getAsJsonArray("elements").map { it.asJsonObject }
            assertTrue(elements.none { it["name"]?.asString?.startsWith("Socket") == true })
            val screens = elements.filter { it["name"]?.asString == "Display screen" }
            assertEquals(8, screens.size)
            assertEquals(
                mapOf("north" to 2, "south" to 2, "east" to 2, "west" to 2),
                screens.groupingBy { it.getAsJsonObject("faces").keySet().single() }.eachCount(),
            )
            for (screen in screens) {
                val face = screen.getAsJsonObject("faces").keySet().single()
                val tangent = if (face == "north" || face == "south") 0 else 2
                val normal = if (tangent == 0) 2 else 0
                val outward = if (face == "south" || face == "east") 1 else -1
                val low = screen.getAsJsonArray("from")
                val high = screen.getAsJsonArray("to")
                assertEquals(1.95, low[tangent].asDouble, "Ring must reach the neighboring screen at its corner")
                assertEquals(14.05, high[tangent].asDouble, "Ring must reach the neighboring screen at its corner")
                val center = (low[1].asDouble + high[1].asDouble) / 2
                assertTrue(center == 1.5 || center == 14.5)
                val surface = if (outward == 1) 14.0 else 2.0
                assertTrue((low[normal].asDouble - surface) * outward in 0.0..0.1)
                assertEquals(2.0, high[1].asDouble - low[1].asDouble)
                val arrow =
                    elements.filter {
                        it["name"]?.asString?.startsWith("Flow arrow") == true &&
                            it.getAsJsonObject("faces").has(face) &&
                            it.getAsJsonArray("from")[1].asDouble >= low[1].asDouble &&
                            it.getAsJsonArray("to")[1].asDouble <= high[1].asDouble
                    }
                assertEquals(3, arrow.count { it["name"].asString == "Flow arrow stem" })
                for (stroke in arrow) {
                    assertTrue(stroke.getAsJsonArray("from")[tangent].asDouble > low[tangent].asDouble)
                    assertTrue(stroke.getAsJsonArray("to")[tangent].asDouble < high[tangent].asDouble)
                    assertTrue((stroke.getAsJsonArray("from")[normal].asDouble - low[normal].asDouble) * outward > 0)
                }

                fun minimum(axis: Int) = arrow.minOf { it.getAsJsonArray("from")[axis].asDouble }

                fun maximum(axis: Int) = arrow.maxOf { it.getAsJsonArray("to")[axis].asDouble }
                assertEquals(8.0, (minimum(tangent) + maximum(tangent)) / 2)
                assertEquals(center, (minimum(1) + maximum(1)) / 2)
                for ((index, first) in arrow.withIndex()) {
                    for (second in arrow.drop(index + 1)) {
                        fun overlap(axis: Int): Double =
                            minOf(first.getAsJsonArray("to")[axis].asDouble, second.getAsJsonArray("to")[axis].asDouble) -
                                maxOf(first.getAsJsonArray("from")[axis].asDouble, second.getAsJsonArray("from")[axis].asDouble)
                        assertTrue(overlap(tangent) <= 0.0 || overlap(1) <= 0.0, "Arrow strokes overlap on $model")
                    }
                }
            }
        }
    }

    @Test
    fun `clutch bars show engagement relative to the shaft on all four sides`() {
        for ((state, color) in mapOf("closed" to "lime_concrete", "open" to "red_concrete")) {
            val model = resource("models/block/clutch_controller_$state.json")
            assertEquals("minecraft:block/$color", model.getAsJsonObject("textures")["mark"].asString)
            val elements = model.getAsJsonArray("elements").map { it.asJsonObject }
            val bars = elements.filter { it["name"].asString == "Clutch state bar" }
            assertEquals(setOf("north", "south", "east", "west"), bars.map { it.getAsJsonObject("faces").keySet().single() }.toSet())
            assertEquals(4, bars.size)
            for (bar in bars) {
                val face = bar.getAsJsonObject("faces").keySet().single()
                val tangent = if (face == "north" || face == "south") 0 else 2
                val low = bar.getAsJsonArray("from")
                val high = bar.getAsJsonArray("to")
                assertEquals(if (state == "closed") 4.5 else 1.0, high[1].asDouble - low[1].asDouble)
                assertEquals(if (state == "closed") 1.0 else 4.5, high[tangent].asDouble - low[tangent].asDouble)
                assertEquals("#mark", bar.getAsJsonObject("faces").getAsJsonObject(face)["texture"].asString)
                val along = if (state == "closed") 1 else tangent
                val across = if (state == "closed") tangent else 1
                val caps =
                    elements
                        .filter { it["name"].asString == "Clutch bar end cap" && it.getAsJsonObject("faces").has(face) }
                        .sortedBy { it.getAsJsonArray("from")[along].asDouble }
                assertEquals(2, caps.size)
                assertEquals(low[along], caps.first().getAsJsonArray("to")[along])
                assertEquals(high[along], caps.last().getAsJsonArray("from")[along])
                for (cap in caps) {
                    assertEquals("#frame", cap.getAsJsonObject("faces").getAsJsonObject(face)["texture"].asString)
                    assertTrue(cap.getAsJsonArray("from")[across].asDouble < low[across].asDouble)
                    assertTrue(cap.getAsJsonArray("to")[across].asDouble > high[across].asDouble)
                }
            }
        }
        val item = resource("models/item/programmable_clutch.json")
        val open = resource("models/block/clutch_controller_open.json")
        assertEquals(open.getAsJsonObject("textures")["mark"], item.getAsJsonObject("textures")["mark"])
        assertEquals(
            open.getAsJsonArray("elements").toList(),
            item.getAsJsonArray("elements").filter { it.asJsonObject["name"]?.asString?.startsWith("Clutch ") == true },
        )
    }

    @Test
    fun `clutch indicators fill the casing surface without protrusions or overlapping faces`() {
        val body = resource("models/block/clutch_body.json").getAsJsonArray("elements").map { it.asJsonObject }
        for (state in listOf("open", "closed")) {
            val panel = resource("models/block/clutch_controller_$state.json").getAsJsonArray("elements").map { it.asJsonObject }
            for (element in panel) {
                assertEquals(1, element.getAsJsonObject("faces").size())
                for (key in listOf("from", "to")) {
                    assertTrue(element.getAsJsonArray(key).all { it.asDouble in 0.0..16.0 })
                }
            }
            for (face in listOf("north", "south", "east", "west")) {
                val normal = if (face == "north" || face == "south") 2 else 0
                val tangent = if (normal == 2) 0 else 2
                val surface = if (face == "south" || face == "east") 16.0 else 0.0
                val quads =
                    (body + panel).filter {
                        it.getAsJsonObject("faces").has(face) &&
                            it.getAsJsonArray("from")[normal].asDouble == surface &&
                            it.getAsJsonArray("to")[normal].asDouble == surface
                    }
                assertEquals(
                    256.0,
                    quads.sumOf {
                        val low = it.getAsJsonArray("from")
                        val high = it.getAsJsonArray("to")
                        (high[tangent].asDouble - low[tangent].asDouble) * (high[1].asDouble - low[1].asDouble)
                    },
                )
                for ((index, first) in quads.withIndex()) {
                    for (second in quads.drop(index + 1)) {
                        fun overlap(axis: Int) =
                            minOf(first.getAsJsonArray("to")[axis].asDouble, second.getAsJsonArray("to")[axis].asDouble) -
                                maxOf(first.getAsJsonArray("from")[axis].asDouble, second.getAsJsonArray("from")[axis].asDouble)
                        assertTrue(overlap(tangent) <= 0 || overlap(1) <= 0, "Coplanar faces overlap on $state $face")
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
            for (powered in listOf(false, true)) {
                val controller =
                    parts
                        .single {
                            val condition = it.getAsJsonObject("when")
                            condition["axis"].asString == axis && condition["powered"].asBoolean == powered &&
                                it.getAsJsonObject("apply")["model"].asString.contains("clutch_controller_")
                        }.getAsJsonObject("apply")
                val state = if (powered) "open" else "closed"
                assertEquals("compukters_create:block/clutch_controller_$state", controller["model"].asString)
                assertEquals(body(powered)["x"], controller["x"])
                assertEquals(body(powered)["y"], controller["y"])
            }
        }
    }
}
