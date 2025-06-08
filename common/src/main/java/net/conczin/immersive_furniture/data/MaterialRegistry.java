package net.conczin.immersive_furniture.data;

import net.conczin.immersive_furniture.client.model.MaterialSource;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class MaterialRegistry {
    public static final MaterialRegistry INSTANCE = new MaterialRegistry();

    public final Map<ResourceLocation, MaterialSource> materials = new HashMap<>();

    public void register(MaterialSource material) {
        materials.put(material.location(), material);
    }
}
