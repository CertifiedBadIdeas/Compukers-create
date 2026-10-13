/*
 * The Compukters Developers
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 * Licensed under the Apache License, Version 2.0.
 */
package ru.lazyhat.compukters.integration.create.gametest;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import ru.lazyhat.compukters.minecraft.peripheral.PeripheralDeviceIdentity;
import ru.lazyhat.compukters.minecraft.peripheral.PeripheralNetworkMember;
import ru.lazyhat.compukters.minecraft.peripheral.PeripheralNetworkStorage;

/** Seeds the persisted per-segment representation produced before controller-based boiler identities. */
final class LegacyBoilerBindings {
    static UUID add(ServerLevel level, BlockPos computer, BlockPos segment, String provider, boolean separateNetwork) {
        var entity = level.getBlockEntity(segment);
        var instance = UUID.randomUUID();
        ((IBlockEntityExtension) entity).getPersistentData().putString("compukters_peripheral_instance", instance.toString());
        entity.setChanged();
        var storage = PeripheralNetworkStorage.Companion.get(level);
        var directory = storage.getDirectory();
        var member = new PeripheralNetworkMember(instance,
                new PeripheralDeviceIdentity(provider, level.dimension().toString(), segment, "boiler"), "boiler");
        UUID network;
        if (separateNetwork) {
            network = directory.createFor(member).getId();
        } else {
            var computerId = UUID.fromString(((IBlockEntityExtension) level.getBlockEntity(computer))
                    .getPersistentData().getString("compukters_peripheral_instance"));
            network = directory.networkOf(computerId).getId();
            directory.bind(network, member);
        }
        storage.setDirty();
        return network;
    }

    static boolean empty(ServerLevel level, UUID network) {
        return PeripheralNetworkStorage.Companion.get(level).getDirectory().get(network).getMembers().isEmpty();
    }
}
