package net.conczin.immersive_furniture.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class RotatedMaterial {
    private final ResourceLocation atlasLocation;
    private final ResourceLocation texture;
    private final int rotation;

    public RotatedMaterial(String location) {
        this(InventoryMenu.BLOCK_ATLAS, new ResourceLocation(location), 0);
    }

    public RotatedMaterial(ResourceLocation atlasLocation, ResourceLocation texture, int rotation) {
        this.atlasLocation = atlasLocation;
        this.texture = texture;
        this.rotation = rotation;
    }

    public TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getTextureAtlas(atlasLocation).apply(texture);
    }

    public int getRotation() {
        return rotation;
    }
}
