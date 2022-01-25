package net.runelite.client.plugins.shattered_relics_fragment_presetsb;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ShatteredRelicsFragmentPresetsSidebarOverlayPanel extends OverlayPanel {

    private final Client client;
    private final ShatteredRelicsFragmentPresetsPlugin plugin;

    private final TitleComponent titleComponent;
    private final LineComponent spacer;
    private final LineComponent newPresetButtonComponent;
    private final LineComponent deletePresetButtonComponent;

    private final int SIDEBAR_WIDTH = 120;
    private final int SIDEBAR_RIGHT_MARGIN = 12;
    private final int SIDEBAR_TOP_MARGIN = 4;
    private final int SIDEBAR_X_ADJUSTMENT = 620;

    private final Map<Preset, LineComponent> presetButtonComponents = new HashMap<>();

    @Inject
    private ShatteredRelicsFragmentPresetsSidebarOverlayPanel(Client client,
            ShatteredRelicsFragmentPresetsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;

        panelComponent.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        titleComponent = TitleComponent.builder().text("Presets").build();
        spacer = LineComponent.builder().build();
        newPresetButtonComponent = LineComponent.builder().left("+ Save as preset").build();
        deletePresetButtonComponent = LineComponent.builder().left("- Delete this preset").build();

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.MED);
    }

    private void checkComponents() {
        // just reset the whole set if anything changes. easier this way
        if (presetButtonComponents.size() != plugin.allPresets.size()) {
            refreshComponents();
            return;
        }

        for (Preset p : plugin.allPresets) {
            if (!presetButtonComponents.containsKey(p)) {
                refreshComponents();
                return;
            }
        }
    }

    private void refreshComponents() {
        presetButtonComponents.clear();
        for (Preset p : plugin.allPresets) {
            LineComponent c = LineComponent.builder().left(p.name).build();
            presetButtonComponents.put(p, c);
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.showingFragments) {
            return null;
        }

        graphics.setFont(FontManager.getRunescapeFont());

        checkComponents();

        renderPresetSidebar(graphics);
        panelComponent.setPreferredLocation(new Point(
                plugin.fragmentWindowBounds.x - SIDEBAR_WIDTH - SIDEBAR_RIGHT_MARGIN + SIDEBAR_X_ADJUSTMENT,
                plugin.fragmentWindowBounds.y + SIDEBAR_TOP_MARGIN));

        plugin.newPresetButtonBounds = newPresetButtonComponent.getBounds();
        plugin.deletePresetButtonBounds = deletePresetButtonComponent.getBounds();

        return super.render(graphics);
    }

    private void renderPresetSidebar(Graphics2D graphics) {
        panelComponent.getChildren().add(titleComponent);
        panelComponent.getChildren().add(spacer);
        panelComponent.getChildren().add(newPresetButtonComponent);
        if (!plugin.allPresets.isEmpty()) {
            if (plugin.activePreset == null) {
                deletePresetButtonComponent.setLeftColor(new Color(192, 192, 192, 255));
            } else {
                deletePresetButtonComponent.setLeftColor(new Color(255, 255, 255, 255));
            }
            panelComponent.getChildren().add(deletePresetButtonComponent);
        }
        panelComponent.getChildren().add(spacer);

        for (Preset p : plugin.allPresets) {
            LineComponent c = presetButtonComponents.get(p);
            if (p == plugin.activePreset) {
                c.setLeftColor(new Color(0, 255, 0, 255));
            } else {
                c.setLeftColor(new Color(255, 255, 255, 255));
            }
            panelComponent.getChildren().add(c);
            p.renderedBounds = c.getBounds();
        }
    }
}
