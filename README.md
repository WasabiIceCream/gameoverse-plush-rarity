# gameoverse-plush-rarity

Makes Plushables items actually rare, without touching Plushables itself.
Wholly original work, MIT licensed.

## The problem

Plushables injects one of its ~150 plush items into almost every major
structure loot table - every village house, mineshafts, both bastion
tables, buried treasure, desert pyramid, end city, jungle temple, nether
fortress, pillager outpost, shipwreck, stronghold, underwater ruins,
woodland mansion, igloo, ancient city, and simple dungeons - via a
hardcoded, unconfigurable loot pool. Confirmed by decompiling
`com.khazoda.plushables.registry.LootTables`: `plushables.properties`
only exposes two unrelated storage-system toggles, nothing about drop
rate. The pool's own roll count is `UniformGenerator.between(0F, 1F)`,
shared across the whole list of qualifying tables, and if it rolls at
all it always picks exactly one item, uniformly, from every single plush
item in the mod. Measured empirically on this server's real loot tables
(100 rolls of `minecraft:chests/simple_dungeon` with this mod's own
filter disabled): **21% of chest rolls produced a plush item** - far
from the "pretty rare" the user expected.

## Why not just patch Plushables' jar directly?

This project has an established convention (see
`spyglass-astronomy-crash-fix`) for hand-patching a mod's bytecode
directly when the fix is small: only make **length-preserving** edits -
swap opcodes in place, never insert new instructions or constant-pool
entries, since that requires recalculating branch offsets by hand and
risks a malformed class file.

That convention ruled out a direct patch here. The rolls value is pushed
via `fconst_0`/`fconst_1` - dedicated single-byte opcodes for the
literals 0.0 and 1.0. There's no `ldc` with a float constant to swap to
something small like 0.05 without changing the instruction's length, and
the only values reachable via a safe same-length opcode swap are 0, 1,
or 2 - meaning a safe patch could only take drops from "common" to
"never," not to "actually rare."

## The real fix

Hooks Fabric API's real, public `LootTableEvents.MODIFY_DROPS` event -
notably a *different* hook than the one Plushables itself uses
(`LootTableEvents.MODIFY`, which edits loot pools before the roll,
while `MODIFY_DROPS` runs on the actual generated `List<ItemStack>`
after the roll). Operating post-roll means this mod needs no knowledge
of which specific loot tables Plushables targets - it just checks every
item in whatever final drop list any loot table produces, and
probabilistically removes any item whose registry namespace is
`plushables`, leaving every other drop in that same roll untouched.
Keep chance is a single constant (`KEEP_CHANCE = 0.05`), easy to retune.

## Verified in-game, not just by code review

Using this project's own established "temp rate-bump" verification
technique: built and deployed three versions in sequence against the
real local dedicated server, rolling `minecraft:chests/simple_dungeon`
100+ times each via `/loot spawn`:

1. `KEEP_CHANCE = 1.0` (filter effectively disabled) - confirmed the
   21% baseline rate above, i.e. that plush items really do show up
   commonly without this mod.
2. `KEEP_CHANCE = 0.0` (always remove) - **0 plushables items in 458
   total drops**, despite the same table producing them regularly one
   restart earlier. Proves the hook actually intercepts and removes
   them, not just a coincidental quiet sample.
3. `KEEP_CHANCE = 0.05` - the real, deployed value.

## Status

Working, verified in-game via the before/after test above. Local-only
so far, not yet pushed to production.
