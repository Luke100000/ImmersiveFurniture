# 0.0.1

Initial release

# Roadmap 1.0.0

* Blockbench/JSON block and item model import
* Custom material packs
* Datapack support
* Multi-block proxy
    * A special block that only defines collisions as states and forwards interactions

# Todo

* Create different blocks
    * One for entity data (I feel like it should be separate)
    * One for furniture
    * One for light sources (but 16x more rare)
* Make registry non-linear (find smallest value between x and y)
    * Use upper 2 bytes for block type, lower 2 bytes for identifier
* Thread the editor
* Add particle emitter
    * Instance the particle and emulate it, it should be possible fine
    * With ambient sound
* Added entity interaction (sit, lay, etc.)
* Inventory
* Icon
