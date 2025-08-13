# 0.0.8

* Furniture now has an active state
    * Reacts to redstone signals
    * Can be toggled with right-click (if enabled in the editor)
    * Can be toggled to also affect light
* Comparator now reads inventory fill state
* Optimized and threaded shape generation to avoid spikes at super complex models
* Beds now allow respawning
* Fixed position when sleeping
* New worlds can now be copied without data loss
* Destroying furniture now kicks off passengers
* You can now hold space to keep the screen open when crafting furniture
* Crafting sounds

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

# Bugs

* Blockbench/JSON block and item model import
* Datapack support
* On flat elements (sprites mostly) the direction is picked oddly
* Better dismounting
* Give elements a mask based on active state
    * This automatically allows for doors and stuff
    * This requires two sets of collisions and models
    * Needs a preview button

## Backend

* Non latin search not searching
* Filter likes button and config