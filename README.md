# Invasion: Overgrowth

**A spiritual successor to *Hostile Worlds: Invasion*, rebuilt from scratch for NeoForge 1.21.1.**

A blood-red meteor, an endless horde that hunts you down, and an overgrown world on the other side of the portal.

**Requires:** Minecraft 1.21.1 + NeoForge 21.1.x. Drop the jar into your `mods` folder (server and clients for multiplayer).

## The invasion
1. On a **random day between day 20 and day 60** (picked once per world) a giant tumbling block of bloody cobblestone streaks overhead with a fire trail, crashes 45–75 blocks from a player and leaves a **bloody cobblestone portal**.
2. **Moss spreads** out from the portal frame. The frame and portal **can't be broken in survival** (immune to explosions, pistons, water, the Wither and the Ender Dragon).
3. An **endless horde** pours out: sword **Invaders** and pickaxe **Invader Miners**. They never burn, never despawn, and **lock on to the nearest survival player anywhere in the Overworld**: they march toward you across the map, tunnel, staircase and pillar through anything softer than obsidian, and block their way up out of water. Respects `mobGriefing`.
4. Run as far as you like: while the invasion is active, small groups **claw their way out of the ground near players who are far from the portal**.
5. Enter the portal (a green starfield) to reach **the Overgrowth** and its mossy stepped-pyramid **temple**, which always stands in the Overgrown Wilds.
6. Defeat the **Temple Guardian**: a fast mossy-stone giant that sprays fire, fires redstone volleys, leap-slams and water-dashes. It drops the **Particle Gauntlet**, the horde stops and every invader crumbles.

**One invasion per world.** `/invasion invasion start` only starts another one when run in creative mode.

## Particle Gauntlet
- **Three tanks**: hold use while looking at a **torch/fire/lava/campfire** (fire), a **redstone torch/wire/block/ore** (redstone) or **water** (water) to charge.
- **Hold use** to fire the selected element: fire stream (burns), redstone bolts (heavy hit + knockback), water jet (douses fire, shoves mobs, jets you backward).
- **Sneak + use**: elemental shockwave. **Punching** with a charged gauntlet adds an elemental hit.
- Press **R** to switch element. Tank bars show next to the hotbar.

## The Overgrowth
Small, varied biomes, so you don't have to travel far to see them all:
- **Overgrown Wilds**: azalea forests, moss everywhere, mossy stone rocks and rubble. Home of the temple.
- **Giant Tree Lands**: Giant Oak, Giant Birch, Giant Azalea and Mixed Giant Woods: forests of towering giants, with rare **Ent villages** (an Elder Tree, Ent-sized hollow-stump houses, standing stones, gardens).
- **The Flesh** and **Dark Flesh**: ground made of flesh, spotted-flesh boulders, hair, blood pools, the Flesh Thing's nest.
- **Blood Grove**: Blood Oaks and giant Blood Oaks, falling leaves and leaf litter, blood geysers and blood lakes.
- **Bloodstone Wastes**: red fog, bloody cobblestone scars and boulders, blood lakes and blood geysers.
- **Overgrown Caves**: lush caves under everything.

Surface water in the red biomes is real **blood**: fill a bucket.

## Creatures
- **Ent** (oak / birch / azalea): tall tree-giants. Peaceful until hit, they fight invaders. Rarely found in vanilla forests too. Rare drop: a **Giant Sapling**.
- **Fleshling**: slime-sized flesh crawlers of the flesh biomes.
- **Flesh Thing** (boss) lives in a **Flesh nest**: bite, telegraphed stomp, flesh-glob volley, charge, then an exhausted window (double damage). Calls Fleshlings at 2/3 and 1/3 health. Drops a **Fleshling Egg** that hatches into a pet Fleshling that follows you, fights for you and can't die.

## Blood, Geysers & Blood Resistance
- **Blood** is thick and slow. It saps players (hunger + slowness) and heals invaders.
- Stepping on a **Blood Geyser** launches you ~16 blocks up and burns you. Sneak to stand on it; mine it with a pickaxe to take it home.
- **Blood Resistance** (boots, treasure only): no burn, slow falling after a launch (geyser **elevators**!), and immunity to blood. Found in Ent village and dungeon chests.

## Gauntlet Spawner
A mossy pedestal with a glowing top, found in every Overgrown Dungeon (mine it with a pickaxe).
- **Display:** use any item on it and the item floats and turns above the glow. Use it empty-handed to take the item back.
- **Rematch:** place the **Particle Gauntlet** on a Gauntlet Spawner inside the Overgrowth temple and the **Temple Guardian rises again**.
  You keep the gauntlet (take it back off the pedestal to fight). A rematch Guardian drops diamonds, emeralds, golden apples and
  experience instead of a second gauntlet.

## Overgrown Dungeons
Underground in every Overgrowth biome: a big mossy monster hall with invader spawners and a blood-geyser altar, cobwebbed corridors and side rooms with
loot chests (diamonds, emeralds, enchanted gear and books, golden apples, netherite scrap, giant saplings, Blood Resistance).

## Blood Oak
Full wood set: log, wood, stripped log/wood, planks, stairs, slab, fence, fence gate, door, trapdoor, button,
pressure plate, leaves, leaf litter, sapling, boat and chest boat.

## Commands (cheats / op)
`/invasion invasion start [player]` · `/invasion invasion stop` · `/invasion invasion status` · `/invasion temple` (`/hw ...` works too).

## Config
`config/invasion-common.toml`: earliest and latest meteor day, auto-invasion, max horde size, spawn interval, pursuit groups on/off and interval,
whether invaders dig, moss radius.

## Building
```
./gradlew build
```
The jar is written to `build/libs/`. Requires Java 21 (Gradle downloads it automatically).

## Credits
See [CREDITS.md](CREDITS.md). All code, models and textures are original. A spiritual successor to *Hostile Worlds: Invasion* by Corosus;
no code or assets from the original mod are included.
