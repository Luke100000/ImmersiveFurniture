# 0.0.1

Initial release

# TODO

Adding more renderers is apparently impossible.
However, it seems quite common to animated the texture atlas.
So, lets add a huge placeholder texture (512x) and update it.


```
net.minecraft.client.renderer.texture.TextureAtlas#upload
net.minecraft.client.renderer.texture.TextureAtlas#cycleAnimationFrames
net.minecraft.client.renderer.block.BlockRenderDispatcher#renderBatched(net.minecraft.world.level.block.state.BlockState, net.minecraft.core.BlockPos, net.minecraft.world.level.BlockAndTintGetter, com.mojang.blaze3d.vertex.PoseStack, com.mojang.blaze3d.vertex.VertexConsumer, boolean, net.minecraft.util.RandomSource, net.minecraftforge.client.model.data.ModelData, net.minecraft.client.renderer.RenderType)
```

* Server maintains a furniture registry
* Server syncs with the client
* Server spawns a block entity for furniture with special functionality
* The Client decides on whether to bake or render (only baking for non-entities)

However, it would be much easier to skip the registry, storing the data directly in the nbt.
For a simple 8 cuboid model with settings thats around a 1kb of data tho. Place 1000 fences and you have 1mb of data.

Alternatively, later on a BakedFurniture can be introduced without entity, relying on said registry to resolve to the data. No need to decide on that yet.