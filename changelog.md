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

Alternatively, later on a BakedFurniture can be introduced without entity, relying on said registry to resolve to the
data. No need to decide on that yet.

# GUI

Fixed tags: storage, seating, lighting, decoration, tool, nature, food, ...

* Library
    * Tabs for Local, Shared, Favorites, Global
    * Search
    * Tags
    * A 2x4 grid of furniture
* Preview
    * Resources, abilities, and preview
    * Craft
    * Modify
    * Favorite
    * Delete (Will differ between local and shared)
* Model
    * A list of elements with names, hide, delete, move up, move down
        * No hierarchy, objects shall not become too complex
        * Elements can be cuboid, particle emitter, item, seat
    * Details
        * Position, rotation, scale
        * Particle type, count, speed, size
        * Slot id
    * Import
        * Import from a block model json or texture
* Materials
    * A material search with "paste on face/object"
    * Material picker
* Effects
    * Global effects, with an excluded list (age, moss, rust, dirt, burned, ...)
    * Each effect has one or more sliders or ticks
* Settings
    * Inventory
        * A list of predefined inventory shapes
    * Sounds
        * Ambient sounds, material sounds, ...
* Overview
    * Name (will complain if it exists locally)
    * Tags
    * Save (back to preview)
    * Share (Will upload, button not available for remixes)

# Materials

* Fetch all block/cube textures with up down etc
* Provide custom ones the same way, even tho they will remain unused
* Reconstruct the names by iterating blocks and constructing a lookup, with fallbacks for the rest

## Wrap modes

A 3D position is mapped to a 2D texture coordinate and sprite.

* Expand: The texture is repeated using a 3x3 grid, works for logs and most other block
    * Default for 2+ face models
* Repeat: The texture is repeated, works for most tillable blocks with minor edge seams
    * Default for 1-face models

An additional offset controls either the inner part, or the whole part for repeat mode.
An additional margin controls the margin for expand mode, default 4px.

## Volumes

Generate (sometimes lazy) volumes to be used by effects.

* Collision
* AO (Circular sample scan)
* Rain (Floodfill with flow, simulating water damage)
* Edge (Similar like AO, but with smaller kernel, detecting edges and corners)
* Noise (Multi octave and seeded)

## Workflow

* Model
    * Transformation tools and rotation settings
    * Per-element settings including particle effects and co
    * Material type override (auto, iron, wood, ...)
    * Copy material, paste material
    * Element type
* Materials
    * A block model lookup with a search
    * Favorite to always show on top
* Material shapes
    * Nails, bordered, ...
    * Uses a luminance + albedo overlay texture
* Material effects as sliders
    * Age, moss, rust, dirt, burned, ...
* Particle emitter settings
    * Particle selection
    * Speed and jitter
    * Size, velocity, ...
* Inventory settings
* Sounds
* Global settings

Left the settings, right the element list