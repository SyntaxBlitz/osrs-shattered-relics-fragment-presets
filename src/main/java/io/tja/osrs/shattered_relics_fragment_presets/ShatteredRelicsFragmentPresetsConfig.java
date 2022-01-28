package io.tja.osrs.shattered_relics_fragment_presets;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("io.tja.osrs.shattered_relics_fragment_presets.v1")
public interface ShatteredRelicsFragmentPresetsConfig extends Config
{
	public final String CONFIG_GROUP = "io.tja.osrs.shattered_relics_fragment_presets.v1";

	public final String ALL_PRESETS = "all_presets";

	@ConfigItem(
			keyName = "shitClickEquipFragment",
			name = "Shift Click Equip Fragments",
			description = "Allows the user to equip fragments by shift-clicking them. Note: There's currently no way to swap the \"unequip\" option.",
			position = 1
	)
	default boolean shitClickEquipFragment()
	{
		return true;
	}
}
