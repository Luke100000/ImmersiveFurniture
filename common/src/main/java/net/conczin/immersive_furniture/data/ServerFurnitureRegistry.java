package net.conczin.immersive_furniture.data;

import net.conczin.immersive_furniture.cobalt.network.NetworkHandler;
import net.conczin.immersive_furniture.config.Config;
import net.conczin.immersive_furniture.network.s2c.FurnitureRegistryMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class ServerFurnitureRegistry {
    public static FurnitureRegistrySavedData getData(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FurnitureRegistrySavedData::new, FurnitureRegistrySavedData::new, "net/conczin/immersive_furniture");
    }

    public static void increase(ServerLevel level, FurnitureData data) {
        String hash = data.getHash();
        FurnitureRegistrySavedData registry = getData(level);
        registry.usageCount.put(hash, registry.usageCount.getOrDefault(hash, 0) + 1);
        registry.setDirty();
    }

    public static int registerIdentifier(ServerLevel level, FurnitureData data, int from, int to) {
        String hash = data.getHash();
        FurnitureRegistrySavedData saveData = getData(level);

        // Already registered
        if (saveData.registry.hashToIdentifier.containsKey(hash)) {
            return saveData.registry.hashToIdentifier.get(hash);
        }

        // Do not register if the hash is not used enough
        int count = saveData.usageCount.getOrDefault(hash, 0);
        if (count < Config.getInstance().lowMemoryModeThreshold) {
            return -1;
        }

        // Search for an available identifier
        for (int identifier = from; identifier <= to; identifier++) {
            if (!saveData.registry.identifierToHash.containsKey(identifier)) {
                // Register new identifier
                saveData.registry.hashToIdentifier.put(hash, identifier);
                saveData.registry.identifierToHash.put(identifier, hash);
                saveData.setDirty();

                // Sync with players
                FurnitureRegistryMessage message = new FurnitureRegistryMessage(Map.of(identifier, hash));
                for (ServerPlayer player : level.players()) {
                    NetworkHandler.sendToPlayer(message, player);
                }

                return identifier;
            }
        }

        // The Registry is full
        return -1;
    }

    public static void syncWithPlayer(ServerPlayer player) {
        int chunkSize = 128;
        FurnitureRegistrySavedData savedData = getData(player.serverLevel());

        Map<Integer, String> subMap = new HashMap<>();
        for (String hash : savedData.registry.hashToIdentifier.keySet()) {
            int identifier = savedData.registry.hashToIdentifier.get(hash);
            subMap.put(identifier, hash);

            if (subMap.size() >= chunkSize) {
                FurnitureRegistryMessage message = new FurnitureRegistryMessage(subMap);
                NetworkHandler.sendToPlayer(message, player);
                subMap.clear();
            }
        }

        if (!subMap.isEmpty()) {
            FurnitureRegistryMessage message = new FurnitureRegistryMessage(subMap);
            NetworkHandler.sendToPlayer(message, player);
        }
    }

    public static class FurnitureRegistrySavedData extends SavedData {
        final Map<String, Integer> usageCount = new HashMap<>();
        final FurnitureRegistry registry = FurnitureRegistry.INSTANCE;

        public FurnitureRegistrySavedData() {

        }

        public FurnitureRegistrySavedData(CompoundTag nbt) {
            CompoundTag usageCountTag = nbt.getCompound("usageCount");
            usageCountTag.getAllKeys().forEach(key ->
                    usageCount.put(key, usageCountTag.getInt(key)));

            CompoundTag hashToIdentifierTag = nbt.getCompound("hashToIdentifier");
            hashToIdentifierTag.getAllKeys().forEach(key -> {
                registry.hashToIdentifier.put(key, hashToIdentifierTag.getInt(key));
                registry.identifierToHash.put(hashToIdentifierTag.getInt(key), key);
            });
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            CompoundTag usageCountTag = new CompoundTag();
            usageCount.forEach(usageCountTag::putInt);
            nbt.put("usageCount", usageCountTag);

            CompoundTag hashToIdentifierTag = new CompoundTag();
            registry.hashToIdentifier.forEach(hashToIdentifierTag::putInt);
            nbt.put("hashToIdentifier", hashToIdentifierTag);

            return nbt;
        }
    }
}
