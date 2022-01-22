package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.TextComponent;

import java.awt.*;

@Slf4j
public class ShatteredRelicsFragmentPresetsOverlay extends Overlay {

    private final Client client;
    private final ShatteredRelicsFragmentPresetsPlugin plugin;

    @Inject
    private ShatteredRelicsFragmentPresetsOverlay(Client client, ShatteredRelicsFragmentPresetsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (plugin.devBounds != null) {
            graphics.setColor(new Color(255, 0, 0, 150));

            graphics.fillRect(plugin.devBounds.x, plugin.devBounds.y, plugin.devBounds.width, plugin.devBounds.height);
        }

        if (!plugin.showingFragments) {
            return null;
        }

        renderFragmentOverlay(graphics);

        return null;
    }

    private void renderFragmentOverlay(Graphics2D graphics) {
        // todo: don't render if filters are shown

        // OverlayUtil.renderPolygon(graphics, , );
        // graphics.setColor(new Color(255, 0, 0, 50));
        // graphics.fillRect(plugin.fragmentListBounds.x, plugin.fragmentListBounds.y,
        // plugin.fragmentListBounds.width, plugin.fragmentListBounds.height);

        for (FragmentData d : plugin.fragmentData) {
            Rectangle intersection = plugin.fragmentListBounds.intersection(d.widgetBounds);

            if (d.isEquipped) {
                graphics.setColor(new Color(0, 255, 0, 20));
            } else {
                graphics.setColor(new Color(255, 255, 0, 50));
            }
            graphics.fillRect(intersection.x, intersection.y, intersection.width, intersection.height);

            if (d.isEquipped) {
                graphics.setColor(new Color(0, 255, 0, 50));
            } else {
                graphics.setColor(new Color(255, 255, 0, 150));
            }

            graphics.fillRect(plugin.fragmentScrollbarInnerBounds.x,
                    (int) (plugin.fragmentScrollbarInnerBounds.y
                            + plugin.fragmentScrollbarInnerBounds.height * d.scrollPercentage),
                    plugin.fragmentScrollbarInnerBounds.width, 2);
        }

        for (Rectangle r : plugin.presetEquippedFragmentBounds) {
            graphics.setColor(new Color(0, 255, 0, 100));
            graphics.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}