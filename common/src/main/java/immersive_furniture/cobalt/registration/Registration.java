package immersive_furniture.cobalt.registration;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class Registration {
    private static Impl INSTANCE;

    public static <T extends Entity> void register(EntityType<T> type, EntityRendererProvider<T> constructor) {
        INSTANCE.registerEntityRenderer(type, constructor);
    }

    public static <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<T> obj) {
        return INSTANCE.register(registry, id, obj);
    }

    public static <T extends BlockEntity> BlockEntityType.Builder<T> blockEntityTypeBuilder(CoboltBlockEntitySupplier<T> supplier, Block block) {
        return INSTANCE.blockEntityTypeBuilder(supplier, block);
    }

    public abstract static class Impl {
        protected Impl() {
            INSTANCE = this;
        }

        public abstract <T extends Entity> void registerEntityRenderer(EntityType<T> type, EntityRendererProvider<T> constructor);


        public abstract <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<T> obj);

        public abstract <T extends BlockEntity> BlockEntityType.Builder<T> blockEntityTypeBuilder(CoboltBlockEntitySupplier<T> supplier, Block block);
    }

    public interface CoboltBlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos blockPos, BlockState blockState);
    }
}
