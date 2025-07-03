package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiRenderTypeBlockModel {
    public final Map<Integer, FurnitureData.Element> indexToElement;
    public final Map<RenderType, BlockModel> models = new LinkedHashMap<>();

    public MultiRenderTypeBlockModel(Map<Integer, FurnitureData.Element> indexToElement) {
        this.indexToElement = indexToElement;
    }

    public void addModel(RenderType type, BlockModel model) {
        if (!model.getElements().isEmpty()) {
            models.put(type, model);
        }
    }

    public FurnitureData.Element getElement(int index) {
        return indexToElement.get(index);
    }
}
