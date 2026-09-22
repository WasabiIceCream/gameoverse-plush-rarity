package net.gameoverse.plushrarity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Plushables injects one of its ~150 plush items into almost every major structure loot table
 * (every village house, mineshafts, both bastion tables, buried treasure, desert pyramid, end
 * city, jungle temple, nether fortress, pillager outpost, shipwreck, stronghold, underwater
 * ruins, woodland mansion, igloo, ancient city, simple dungeons) via a hardcoded, unconfigurable
 * loot pool that rolls a uniform 0-1 times per chest - averaging roughly a 50% chance per
 * qualifying chest, with no config option to tune it (confirmed by decompiling
 * {@code com.khazoda.plushables.registry.LootTables} - {@code plushables.properties} only has two
 * unrelated storage-system toggles).
 * <p>
 * Rather than patching Plushables' own bytecode (the rolls range is pushed via single-byte
 * {@code fconst_0}/{@code fconst_1} opcodes with no safe length-preserving way to reach a small
 * fraction - see this project's own README for the full investigation), this hooks the real,
 * public {@code LootTableEvents.MODIFY_DROPS} event - the same one Plushables itself doesn't use,
 * since it modifies pools before the roll, not results after. Operating post-roll instead means
 * this needs no knowledge of which specific loot tables Plushables targets: it just probabilistically
 * strips plush items out of whatever final drop list any loot table produces, wherever they came
 * from, leaving every other drop in that same roll completely untouched.
 */
public class PlushRarity implements ModInitializer {
   private static final String PLUSHABLES_NAMESPACE = "plushables";

   /** Chance a plush item that would have dropped is actually kept - deliberately very low. */
   private static final double KEEP_CHANCE = 0.05;

   @Override
   public void onInitialize() {
      LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
         drops.removeIf(stack -> isPlushable(stack) && context.getRandom().nextDouble() >= KEEP_CHANCE);
      });
   }

   private static boolean isPlushable(ItemStack stack) {
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id != null && PLUSHABLES_NAMESPACE.equals(id.getNamespace());
   }
}
