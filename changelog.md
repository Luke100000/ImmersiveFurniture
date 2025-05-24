# 0.0.1

Initial release

# Roadmap 1.0.0

* High-performance block renderer
* Blockbench/JSON block and item model import

# Renderer

There are two rendering pipelines:

* Over the BlockRenderDispatcher in FurnitureBlockEntityRenderer
    * This uses the scratch or entity buffer
    * It injects into item shaper and item renderer (possible breaking points!)
    * That one is fully dynamic
* Over the BlockShaper and chunk renderer system
    * That one needs the static furniture texture and syncing
    * The state also needs the fixed lookup id, and the server needs to sync the nbt

For items, one can also use the default rendering over the item renderer, which uses the block texture as well.

* Safer (will use the default atlas and wont break in modded scenarios)
* Less flexible (semi-fixed atlas again, clearing is possible but sketchy)
* Think about this once block renderer is done, since that's basically the same

# TODO

* Scrap shapes, finish global effects
* Finish material selector
* Fix rotations
    * Fix AO
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
* Confirm dont escape in editor
