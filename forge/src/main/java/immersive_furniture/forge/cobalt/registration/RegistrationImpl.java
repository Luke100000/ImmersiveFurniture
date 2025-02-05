package immersive_furniture.forge.cobalt.registration;

import immersive_furniture.cobalt.registration.Registration;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.*;
import java.util.function.Supplier;


public class RegistrationImpl extends Registration.Impl {
    private final Map<String, RegistryRepo> repos = new HashMap<>();

    private RegistryRepo getRepo(String namespace) {
        return repos.computeIfAbsent(namespace, RegistryRepo::new);
    }

    @Override
    public <T extends Entity> void registerEntityRenderer(EntityType<T> type, EntityRendererProvider<T> constructor) {
        EntityRenderers.register(type, constructor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<T> obj) {
        DeferredRegister reg = getRepo(id.getNamespace()).get(registry);
        return reg.register(id.getPath(), obj);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType.Builder<T> blockEntityTypeBuilder(Registration.CoboltBlockEntitySupplier<T> supplier, Block block) {
        return BlockEntityType.Builder.of(supplier::create, block);
    }

    static class RegistryRepo {
        private final Set<ResourceLocation> skipped = new HashSet<>();
        private final Map<ResourceLocation, DeferredRegister<?>> registries = new HashMap<>();

        private final String namespace;

        public RegistryRepo(String namespace) {
            this.namespace = namespace;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> DeferredRegister get(Registry<? super T> registry) {
            ResourceLocation id = registry.key().location();
            if (!registries.containsKey(id) && !skipped.contains(id)) {
                //noinspection UnstableApiUsage
                ForgeRegistry reg = RegistryManager.ACTIVE.getRegistry(id);
                if (reg == null) {
                    skipped.add(id);
                    return null;
                }

                DeferredRegister def = DeferredRegister.create(Objects.requireNonNull(reg, "Registry=" + id), namespace);

                def.register(FMLJavaModLoadingContext.get().getModEventBus());

                registries.put(id, def);
            }

            return registries.get(id);
        }
    }

    public static class DataLoaderRegister {
        private final List<PreparableReloadListener> dataLoaders = new ArrayList<>();

        public List<PreparableReloadListener> getLoaders() {
            return dataLoaders;
        }
    }
}
