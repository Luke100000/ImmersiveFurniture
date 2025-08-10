# 0.0.8

* Optimized and threaded shape generation to avoid spikes at super complex models
* Beds now allow respawning
* New worlds can now be copied without data loss
* Destroying furniture now kicks off passengers
* Fixed position when sleeping

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
* Only play the closest sound + particles and those stacked?
    * Needs a flag somewhere, should not be defaulted
* Non latin search not searching
* Redstone signal and turn light on and off
    * Inventory content, or on-right-click (strength slider?)
* Redstone input shall trigger right click
* Multi craft intransparent and hard to use (E.g. "hold shift, hold space")
* On flat elements (sprites mostly) the direction is picked oddly
* Better dismounting
* Filter likes button and config