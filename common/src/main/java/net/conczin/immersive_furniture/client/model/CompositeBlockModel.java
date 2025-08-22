package net.conczin.immersive_furniture.client.model;

import net.conczin.immersive_furniture.data.FurnitureData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class CompositeBlockModel {
    // The tint index stores the mapping to the original element id.
    public final Map<Integer, FurnitureData.Element> indexToElement;

    // For each state and each render type a separate model
    public final Map<Integer, Map<RenderType, BlockModel>> models = new LinkedHashMap<>();

    public CompositeBlockModel(Map<Integer, FurnitureData.Element> indexToElement) {
        this.indexToElement = indexToElement;
    }

    public void addModel(RenderType type, Map<Integer, BlockModel> model) {
        for (Map.Entry<Integer, BlockModel> entry : model.entrySet()) {
            if (!entry.getValue().getElements().isEmpty()) {
                models.computeIfAbsent(
                        entry.getKey(),
                        k -> new LinkedHashMap<>()
                ).put(type, entry.getValue());
            }
        }
    }

    public FurnitureData.Element getElement(int index) {
        return indexToElement.get(index);
    }
}
