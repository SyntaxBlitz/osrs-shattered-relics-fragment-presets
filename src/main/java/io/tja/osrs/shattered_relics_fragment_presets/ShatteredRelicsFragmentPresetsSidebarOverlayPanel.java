package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class ShatteredRelicsFragmentPresetsSidebarOverlayPanel extends OverlayPanel {

    private final Client client;
    private final ShatteredRelicsFragmentPresetsPlugin plugin;

    private final TitleComponent titleComponent;
    private final LineComponent spacer;
    private final LineComponent newPresetButtonComponent;

    private final int SIDEBAR_WIDTH = 120;
    private final int SIDEBAR_RIGHT_MARGIN = 12;
    private final int SIDEBAR_TOP_MARGIN = 4;

    private final Map<Preset, LineComponent> presetButtonComponents = new HashMap<>();

    @Inject
    private ShatteredRelicsFragmentPresetsSidebarOverlayPanel(Client client,
            ShatteredRelicsFragmentPresetsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;

        panelComponent.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        titleComponent = TitleComponent.builder().text("Presets").build();
        spacer = LineComponent.builder().build();
        newPresetButtonComponent = LineComponent.builder().left("+ New Preset").build();

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
                plugin.fragmentWindowBounds.x - SIDEBAR_WIDTH - SIDEBAR_RIGHT_MARGIN,
                plugin.fragmentWindowBounds.y + SIDEBAR_TOP_MARGIN
            )
        );

        plugin.newPresetButtonBounds = newPresetButtonComponent.getBounds();

        return super.render(graphics);
    }

    private void renderPresetSidebar(Graphics2D graphics) {
        // final FontMetrics metrics = graphics.getFontMetrics();
        // final int textDescent = metrics.getDescent();
        // final int textHeight = metrics.getHeight();

        // newPresetButtonComponent.setPreferredSize(new Dimension(0, textHeight + 100));
        
        panelComponent.getChildren().add(titleComponent);
        panelComponent.getChildren().add(spacer);
        panelComponent.getChildren().add(newPresetButtonComponent);
        panelComponent.getChildren().add(spacer);

        for (Preset p : presetButtonComponents.keySet()) {
            LineComponent c = presetButtonComponents.get(p);
            if (p == plugin.activePreset) {
                c.setLeftColor(new Color(0, 255, 0, 255));
            } else {
                c.setLeftColor(new Color(255, 255, 255, 255));
            }
            panelComponent.getChildren().add(c);
            p.renderedBounds = c.getBounds();
        }

//        int sidebarWidth = 100;
//        int sidebarRightMargin = 12;
//        int sidebarPadding = 12;
//        int sidebarTopMargin = 4;
//        int sidebarHeight = 200; // todo calculate
//        Rectangle sidebarBox = new Rectangle(plugin.fragmentWindowBounds.x - sidebarWidth - sidebarRightMargin,
//                plugin.fragmentWindowBounds.y + sidebarTopMargin,
//                sidebarWidth,
//                sidebarHeight);
//
//        graphics.setColor(new Color(0, 0, 0, 200));
//        graphics.fillRect(sidebarBox.x, sidebarBox.y, sidebarBox.width, sidebarBox.height);
//
//        // render "new preset" button
//        final FontMetrics metrics = graphics.getFontMetrics();
//        final int textDescent = metrics.getDescent();
//        final int textHeight = metrics.getHeight();
//
//        net.runelite.client.ui.overlay.components.TextComponent textComponent = new TextComponent();
//        textComponent.setText("+ New preset");
//        textComponent.setPosition(new Point(sidebarBox.x + sidebarPadding, sidebarBox.y + sidebarPadding + textHeight));
//        textComponent.render(graphics);
    }
}
