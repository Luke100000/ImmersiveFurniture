package immersive_furniture.fabric.cobalt.registration;

import immersive_furniture.cobalt.registration.Registration;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class RegistrationImpl extends Registration.Impl {
    @Override
    public <T extends Entity> void registerEntityRenderer(EntityType<T> type, EntityRendererProvider<T> constructor) {
        EntityRendererRegistry.register(type, constructor);
    }

    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<T> obj) {
        T register = Registry.register(registry, id, obj.get());
        return () -> register;
    }

    @Override
    public <T extends BlockEntity> BlockEntityType.Builder<T> blockEntityTypeBuilder(Registration.CoboltBlockEntitySupplier<T> supplier, Block block) {
        return BlockEntityType.Builder.of(supplier::create, block);
    }
}
