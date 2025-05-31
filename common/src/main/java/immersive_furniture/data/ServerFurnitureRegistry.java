package immersive_furniture.data;

import immersive_furniture.cobalt.network.NetworkHandler;
import immersive_furniture.config.Config;
import immersive_furniture.network.s2c.FurnitureRegistryMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class ServerFurnitureRegistry {
    public static FurnitureRegistrySavedData getData(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FurnitureRegistrySavedData::new, FurnitureRegistrySavedData::new, "immersive_furniture");
    }

    public static void increase(ServerLevel level, FurnitureData data) {
        String hash = data.getHash();
        FurnitureRegistrySavedData registry = getData(level);
        registry.usageCount.put(hash, registry.usageCount.getOrDefault(hash, 0) + 1);
        registry.setDirty();
    }

    public static int registerIdentifier(ServerLevel level, FurnitureData data) {
        String hash = data.getHash();
        FurnitureRegistrySavedData saveData = getData(level);

        // Do not register if the hash is not used enough
        int count = saveData.usageCount.getOrDefault(hash, 0);
        if (count < Config.getInstance().lowMemoryModeThreshold) {
            return 0;
        }

        if (saveData.registry.hashToIdentifier.containsKey(hash)) {
            // Already registered
            return saveData.registry.hashToIdentifier.get(hash);
        }

        int identifier = saveData.registry.hashToIdentifier.size() + 1;
        if (identifier < 1024) {
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
        } else {
            // The Registry is full
            return 0;
        }
    }

    public static void syncWithPlayer(ServerPlayer player) {
        int chunkSize = 128;
        FurnitureRegistrySavedData savedData = getData(player.serverLevel());
        for (int i = 1; i <= savedData.registry.identifierToHash.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, savedData.registry.identifierToHash.size());
            Map<Integer, String> subMap = new HashMap<>();
            for (int j = i; j <= end; j++) {
                if (savedData.registry.identifierToHash.containsKey(j)) {
                    subMap.put(j, savedData.registry.identifierToHash.get(j));
                }
            }
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
