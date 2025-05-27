# 0.0.1

Initial release

# Roadmap 1.0.0

* High-performance block renderer
* Blockbench/JSON block and item model import


# Today

* If the atlas is full, fall back to realtime rendering
* Outline but not implement hashed and identifier based data fetching
* Thread the editor
* Print usages in F3

# Syncer

* Use the default cache `baked/worldUUID/id`
* That, however, will fail for the second player the first few chunks
* What if
    * Remember failed chunks, rerender them once the data is loaded?

# Data

There are three ways to get furniture data:

* As block entity data (full data)
* As hash in entity data with lookup (lite data, on the client this results in delayed rendering)
* As identifier in block state (ultra lite, limited to n variants, results in delayed rendering)
  * need to find a powerful heuristic on when to switch, there is no way to clean up once a block has been registered. 

# Renderer

There are three rendering pipelines:

* Dynamic rendering
    * Via block entity animated or the item injection
        * Used for scratch, entity which requires an entity anyway, and as a last resort
    * Via baked block and item renderers
        * Used for block and item models but only up to 1024 variants
    * Via injection into the chunk renderer as a block entity
        * Basically the first approach but with the block entity renderer
        * That should work since ClientboundLevelChunkWithLightPacket contains the block entity data
        * Fast rendering but still high memory footprint

# TODO

* Fix material texture fetcher
* Add particle emitter
    * Instance the particle and emulate it, it should be possible fine
    * With ambient sound
* Added entity interaction (sit, lay, etc.)
* Add cost calculation and "Nails and Timber" resource
* Retire Cobalt
* Dataloader support and networking
* Inventory
* Icon
