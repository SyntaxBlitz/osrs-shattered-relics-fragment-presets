package io.tja.osrs.shattered_relics_fragment_presets;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("io.tja.osrs.shattered_relics_fragment_presets.v1")
public interface ShatteredRelicsFragmentPresetsConfig extends Config {
    public final String CONFIG_GROUP = "io.tja.osrs.shattered_relics_fragment_presets.v1";

    public final String ALL_PRESETS = "all_presets";

    @ConfigItem(
            keyName = "resizable_offset_x",
            name = "X Offset in resizable mode",
            description = "When in resizable mode, this offset will be manually applied to the X position of the " +
                    "preset pane",
            position = 0
    )
    @Range(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    default int resizableOffsetX() {
        return 0;
    }

    @ConfigItem(
            keyName = "resizable_offset_y",
            name = "Y Offset in resizable mode",
            description = "When in resizable mode, this offset will be manually applied to the Y position of the " +
                    "preset pane",
            position = 1
    )
    @Range(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    default int resizableOffsetY() {
        return 0;
    }

    @ConfigItem(
            keyName = "shift_click_equip_unequip",
            name = "Shift-click equip/unequip",
            description = "Equip and unequip fragments by shift-clicking them",
            position = 2
    )
    default boolean shiftClickToggleFragment() {
        return true;
    }

    enum DragMode {
        ENABLED,
        IMPROVED,
        DISABLED
    }
    @ConfigItem(
            keyName = "drag_mode",
            name = "Drag and Drop",
            description = "How fragment dragging and dropping should function - 'Improved' and 'Disabled' make it " +
                    "easier to shift-click fragments quickly",
            position = 3
    )
    default DragMode dragMode() { return DragMode.ENABLED; }

    @ConfigItem(
            keyName = "show_extra_buttons",
            name = "Show import/export buttons",
            description = "Show extra buttons for importing to/exporting from system clipboard",
            position = 4
    )
    default boolean showExtraButtons() {
        return false;
    }

    @ConfigItem(
            keyName = "page_size",
            name = "Preset page size",
            description = "The number of presets to display per page",
            position = 5
    )
    @Range(min = 1, max = Integer.MAX_VALUE)
    default int pageSize() {
        return 10;
    }
}
