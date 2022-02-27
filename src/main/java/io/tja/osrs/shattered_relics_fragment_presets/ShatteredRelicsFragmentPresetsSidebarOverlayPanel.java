package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.ComponentConstants;
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
    private final LineComponent importPresetButtonComponent;
    private final LineComponent exportPresetButtonComponent;
    private final LineComponent pageChangeButtonComponent;

    private final int SIDEBAR_WIDTH = 120;
    private final int SIDEBAR_RIGHT_MARGIN = 12;
    private final int SIDEBAR_TOP_MARGIN = 4;

    private boolean isFixedWidth;

    private final Map<Preset, LineComponent> presetButtonComponents = new HashMap<>();

    @Inject
    private ShatteredRelicsFragmentPresetsSidebarOverlayPanel(Client client,
            ShatteredRelicsFragmentPresetsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        titleComponent = TitleComponent.builder().text("Presets").build();
        spacer = LineComponent.builder().build();
        newPresetButtonComponent = LineComponent.builder().left("+ Save as preset").build();
        deletePresetButtonComponent = LineComponent.builder().left("- Delete this preset").build();
        importPresetButtonComponent = LineComponent.builder().left("Import <- clipboard").build();
        exportPresetButtonComponent = LineComponent.builder().left("Export -> clipboard").build();
        pageChangeButtonComponent = LineComponent.builder().build();

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.MED);
    }

    public void setIsFixedViewport(boolean isFixedWidth) {
        this.isFixedWidth = isFixedWidth;
        if (isFixedWidth) {
            Widget rootInterfaceWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT_ROOT_INTERFACE_CONTAINER);
            panelComponent.setPreferredSize(new Dimension(rootInterfaceWidget.getOriginalWidth(), 0));
            panelComponent.setBackgroundColor(new Color(70, 61, 50, 255));
        } else {
            panelComponent.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
            panelComponent.setBackgroundColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
        }
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
        if (isFixedWidth) {
            Widget rootInterfaceWidget = client.getWidget(WidgetInfo.FIXED_VIEWPORT_ROOT_INTERFACE_CONTAINER);
            panelComponent.setPreferredLocation(new Point(
                    rootInterfaceWidget.getOriginalX(),
                    rootInterfaceWidget.getOriginalY()));
        } else {
            panelComponent.setPreferredLocation(new Point(
                    plugin.fragmentWindowBounds.x - SIDEBAR_WIDTH - SIDEBAR_RIGHT_MARGIN + plugin.getOffsetX(),
                    plugin.fragmentWindowBounds.y + SIDEBAR_TOP_MARGIN + plugin.getOffsetY()));
        }

        plugin.newPresetButtonBounds = newPresetButtonComponent.getBounds();
        plugin.deletePresetButtonBounds = deletePresetButtonComponent.getBounds();
        if (plugin.shouldShowExtraButtons()) {
            plugin.importPresetButtonBounds = importPresetButtonComponent.getBounds();
            plugin.exportPresetButtonBounds = exportPresetButtonComponent.getBounds();
        } else {
            plugin.importPresetButtonBounds = null;
            plugin.exportPresetButtonBounds = null;
        }

        if (shouldRenderPreviousButton()) {
            Rectangle fullBounds = pageChangeButtonComponent.getBounds();
            plugin.previousPageButtonBounds = new Rectangle(fullBounds.x, fullBounds.y, fullBounds.width / 2, fullBounds.height);
        } else {
            plugin.previousPageButtonBounds = null;
        }
        if (shouldRenderNextButton()) {
            Rectangle fullBounds = pageChangeButtonComponent.getBounds();
            plugin.nextPageButtonBounds = new Rectangle(fullBounds.x + fullBounds.width / 2, fullBounds.y, fullBounds.width / 2, fullBounds.height);
        } else {
            plugin.nextPageButtonBounds = null;
        }


        return super.render(graphics);
    }

    private void renderPresetSidebar(Graphics2D graphics) {
        titleComponent.setText(String.format("Presets (%d/%d)", plugin.selectedPage + 1, plugin.numberOfPages()));
        panelComponent.getChildren().add(titleComponent);

        if (shouldRenderNextButton() || shouldRenderPreviousButton()) {
            panelComponent.getChildren().add(pageChangeButtonComponent);
        }
        if (shouldRenderPreviousButton()) {
            pageChangeButtonComponent.setLeft("<- Previous");
        } else {
            pageChangeButtonComponent.setLeft(null);
        }
        if (shouldRenderNextButton()) {
            pageChangeButtonComponent.setRight("Next ->");
        } else {
            pageChangeButtonComponent.setRight(null);
        }

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
        if (plugin.shouldShowExtraButtons()) {
            panelComponent.getChildren().add(importPresetButtonComponent);
            panelComponent.getChildren().add(exportPresetButtonComponent);
        }

        panelComponent.getChildren().add(spacer);

        for (Preset p : plugin.currentPageOfPresets()) {
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

    private boolean shouldRenderPreviousButton() {
        return plugin.selectedPage > 0;
    }

    private boolean shouldRenderNextButton() {
        return plugin.selectedPage < plugin.numberOfPages() - 1;
    }
}
