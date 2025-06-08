package net.conczin.immersive_furniture.fabric.cobalt.registration;

import net.conczin.immersive_furniture.cobalt.registration.Registration;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class RegistrationImpl extends Registration.Impl {
    @Override
    public <T> Supplier<T> register(Registry<? super T> registry, ResourceLocation id, Supplier<T> obj) {
        T register = Registry.register(registry, id, obj.get());
        return () -> register;
    }

    @Override
    public <T extends BlockEntity> BlockEntityType.Builder<T> blockEntityTypeBuilder(Registration.CobaltBlockEntitySupplier<T> supplier, Block block) {
        return BlockEntityType.Builder.of(supplier::create, block);
    }
}
