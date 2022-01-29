package io.tja.osrs.shattered_relics_fragment_presets;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("io.tja.osrs.shattered_relics_fragment_presets.v1")
public interface ShatteredRelicsFragmentPresetsConfig extends Config
{
	public final String CONFIG_GROUP = "io.tja.osrs.shattered_relics_fragment_presets.v1";

	public final String ALL_PRESETS = "all_presets";

	@ConfigItem(keyName = "resizable_offset_x", name = "X Offset in resizable mode", description = "When in resizable" +
			" mode, this offset will be manually applied to the X position of the preset pane")
	@Range(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
	default int resizableOffsetX() {
		return 0;
	}

	@ConfigItem(keyName = "resizable_offset_y", name = "Y Offset in resizable mode", description = "When in resizable" +
			" mode, this offset will be manually applied to the Y position of the preset pane")
	@Range(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
	default int resizableOffsetY() {
		return 0;
	}
  
  @ConfigItem(
			keyName = "shitClickEquipFragment",
			name = "Shift Click Equip Fragments",
			description = "Allows the user to equip fragments by shift-clicking them. Note: There's currently no way to swap the \"unequip\" option.",
			position = 1
	)
	default boolean shitClickEquipFragment() {
		return true;
  }
}
