# 0.0.1

Initial release

# TODO

* Scrap shapes, finish global effects
* Finish material selector
* Fix material texture fetcher
* Add inventory and settings page
* Add finish page
* Add particle emitter
    * Instance the particle and emulate it, it should be possible fine
* Add furniture export
* Baked textures support

# Export

* Export as PNG with data in metadata
* Data is exported as nbt for simplicity

# Sharing

* Furniture is stored as identifiers username:name (username local is mapped respectively)
* In the list, local furniture can be uploaded (will overwrite existing)
    * When creating furniture, auto share doesn't make sense imo because people may "just play around"

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

## Library adjustments

* Improve tags endpoint to return a dict with usage count, cropped to 1000 tags.
* Deprecate v1/list from the api list if possible.
* Why dafuq is meta not part of the search? It feels like the whole point of meta is to be part of it?
    * Since it breaks MCA, v2 is required (or a include meta flag)
    * Then also extract it because wtf why is it a string like the rest?
    * Probably add a header field "meta=none|decoded|raw" to the search" and call the possibity of not using json
      compatible strings a feature lol (E.g. java might actually profit from that)
        * Same with upload, if a string is present it gets parsed.
    * Then, v2 just contains "meta=none" by default because often it is not needed anyway
    * And content endpoint gets a v2 with meta=decoded default, with v1 passing meta=raw
    * And think about additional stuff to add to v2
    * Finish the post processing hook and add a dry field, also return logs and performance stats.
    * Add processors to MCA, we need to ramp up security:
        * Image stripper: Load the png, strip it from all metadata, and save it again, rejecting any "non png, non
          64x64, non 32bpp" images
        * Metadata validator: Check if the metadata is valid json, and all fields are of valid type, and not too long.