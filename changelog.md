# 0.1.0

* Added furniture states
    * Elements can now be enabled/disabled based on furniture active state
    * This allows, for example, doors, traps, yapping animations, etc.
* Added "Item Sprites" to display an item in the inventory rather than a sprite
* Some special sounds are no longer played globally

# 0.0.9

* Fixed a crash

# 0.0.8

* Furniture now has an active state
    * Reacts to redstone signals
    * Can be toggled with right-click (if enabled in the editor)
    * Can be toggled to also affect light
* Comparator now reads inventory fill state
* Optimized and threaded shape generation to avoid spikes at super complex models
    * Only the selected block of multi-block furniture is now outlined when hovered
* Beds now allow respawning
* Fixed position when sleeping
* Dismount position on sitting furniture is now way more reasonable
* New worlds can now be copied without data loss
* Destroying furniture now kicks off passengers
* You can now hold ctrl to keep the screen open when crafting furniture
* Crafting sounds
* Added better furniture sorting to balance popular and new furniture

# 0.0.7

* Fixed interact not interacting on Forge
* Fixed issues with mipmapping

# 0.0.6

* Added multi-selection
* Fixed poses not being rotatable and the front is marked
* Added ctrl-A (select all) and ctrl-D (duplicate) shortcut
* Fixed sounds playing twice
* Fix cache degradation

# 0.0.5

* Fixed crashes and bugs
* Fixed tiled rotated sprites
* Users can now upload modified furniture
* Missing sprites are no longer rendered (instead of ugly missing textures)
* Fixed used resources and mods not tracked in the tooltip

# 0.0.4

* Fixes and improvements
* Interact sounds and particles are now shared in multiplayer

# 0.0.3

* Added autosave
* Added emission to materials and sprites
* Improved transparent rendering
* Improved ambient occlusion on transparent elements
* Transparent elements no longer cull other elements
* Fixed Artisans table is not dropping itself
* Added some anti-z-fighting (Does not replace proper modeling!)
* Sprites now support tile mode (mostly interesting for liquids and co)
* Fixed Sodium incompatibility pausing animations

# 0.0.2

* Fixed a crash

# 0.0.1

Initial release

# TODO

* Blockbench/JSON block and item model import
* Datapack support
* On flat elements (sprites mostly) the direction is picked oddly

## Backend

* Non latin search not searching
* Filter likes button and config