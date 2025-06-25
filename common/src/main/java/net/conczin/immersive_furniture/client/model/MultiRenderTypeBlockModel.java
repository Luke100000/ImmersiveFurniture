package net.conczin.immersive_furniture.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;

import java.util.HashMap;
import java.util.Map;

public class MultiRenderTypeBlockModel {
    public final Map<RenderType, BlockModel> models = new HashMap<>();

    public void addModel(RenderType type, BlockModel model) {
        if (!model.getElements().isEmpty()) {
            models.put(type, model);
        }
    }
}
