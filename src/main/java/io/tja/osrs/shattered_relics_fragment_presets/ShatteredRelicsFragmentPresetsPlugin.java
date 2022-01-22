package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;

import jdk.internal.joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(name = "Shattered Relics Fragment Presets")
public class ShatteredRelicsFragmentPresetsPlugin extends Plugin implements MouseListener {
	@Inject
	private Client client;

	@Inject
	private ShatteredRelicsFragmentPresetsConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ShatteredRelicsFragmentPresetsOverlay fragmentOverlay;

	@Inject
	private ShatteredRelicsFragmentPresetsSidebarOverlayPanel sidebarOverlay;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private Gson gson;

	@Inject
	private ConfigManager configManager;

    public static final IntPredicate FILTERED_CHARS = c -> "</>:".indexOf(c) == -1;

	public boolean showingFragments = false;
	public Rectangle fragmentWindowBounds = null;
	public Rectangle fragmentListBounds = null;
	public Rectangle fragmentScrollbarInnerBounds = null;

	public Set<FragmentData> fragmentData;
	public Set<Rectangle> presetEquippedFragmentBounds;
    private Set<String> equippedFragmentNames = new HashSet<>();

	public Rectangle devBounds = null;

	public Rectangle newPresetButtonBounds; // set by overlay
    public Rectangle deletePresetButtonBounds; // set by overlay

	private int tickTimer = 0;

	public List<Preset> allPresets = new ArrayList<>();
	public static Type PRESET_LIST_TYPE = new TypeToken<List<Preset>>() {}.getType();
	public Preset activePreset;

	@Override
	protected void startUp() throws Exception {
		loadPersistedPresets();

		mouseManager.registerMouseListener(this);

		overlayManager.add(fragmentOverlay);
		overlayManager.add(sidebarOverlay);
	}

	@Override
	protected void shutDown() throws Exception {
		mouseManager.unregisterMouseListener(this);

		overlayManager.remove(fragmentOverlay);
		overlayManager.remove(sidebarOverlay);
	}

	private String getWidgetString(Widget w) {
		// maybe could also use getNestedChildren
		if (w == null)
			return "";
		String ws = "";
		if (!w.getText().isEmpty())
			ws += w.getText() + "\n";
		// Widget[] children = w.getStaticChildren();
		Widget[] children = w.getDynamicChildren();
		if (children == null)
			return ws;

		for (Widget child : children) {
			ws += getWidgetString(child) + "\n";
		}
		return ws;
	}

	@Subscribe
	public void onClientTick(ClientTick event) {
		tickTimer++;

		// Widget devWidget = client.getWidget(735, 1);
		// if (devWidget != null)
		// devBounds = devWidget.getBounds();
		// devBounds = null;

		Widget fragmentWindow = client.getWidget(735, 1);
		if (fragmentWindow == null) {
			activePreset = null;
			showingFragments = false;
			return;
		}

		Widget fragmentList = client.getWidget(735, 17);
		Widget fragmentScrollbar = client.getWidget(735, 18);
		Widget fragmentScrollbarInner = fragmentScrollbar.getChildren()[0];
		Widget equippedFragmentsContainer = client.getWidget(735, 35);
		Set<Widget> equippedFragmentWidgets = Arrays.stream(equippedFragmentsContainer.getDynamicChildren())
				.filter(child -> child.getName() != null && !child.getName().isEmpty())
				.collect(Collectors.toSet());
		equippedFragmentNames = equippedFragmentWidgets.stream()
				.map(child -> Text.removeTags(child.getName()))
				.collect(Collectors.toSet());

		presetEquippedFragmentBounds = equippedFragmentWidgets.stream()
				.filter(widget -> activePreset != null && activePreset.fragments.contains(Text.removeTags(widget.getName())))
				.map(widget -> widget.getBounds())
				.collect(Collectors.toSet());

		double totalScrollHeight = fragmentList.getScrollHeight();

		fragmentWindowBounds = fragmentWindow.getBounds();
		fragmentListBounds = fragmentList.getBounds();
		fragmentScrollbarInnerBounds = fragmentScrollbarInner.getBounds();

		Set<FragmentData> theseFragmentData = new HashSet<>();

		Widget[] fragmentListSubWidgets = fragmentList.getDynamicChildren();
		for (int i = 0; i < fragmentListSubWidgets.length; i++) {
			Widget subWidget = fragmentListSubWidgets[i];
			String widgetString = getWidgetString(subWidget); // TODO: don't use getWidgetString here
			if (activePreset != null && activePreset.fragments.contains(widgetString.trim())) {
				Widget containerSubWidget = fragmentListSubWidgets[i - 7];
				FragmentData fragmentData = new FragmentData();
				fragmentData.widgetBounds = containerSubWidget.getBounds();
				fragmentData.isEquipped = equippedFragmentNames.contains(widgetString.trim());
				fragmentData.scrollPercentage = containerSubWidget.getRelativeY() / totalScrollHeight;

				theseFragmentData.add(fragmentData);
			}
		}

		fragmentData = theseFragmentData;

		showingFragments = true;
	}

    private void newPreset() {
        chatboxPanelManager.openTextInput("Preset name (use the same name to overwrite):")
            .addCharValidator(FILTERED_CHARS)
            .onDone((presetName) -> {
                String trimmed = presetName.trim();
                if (trimmed.isEmpty()) {
                    return;
                }
                savePreset(trimmed);
            })
            .build();
    }

    private void savePreset(String presetName) {
        Preset preset = new Preset();
        preset.name = presetName;
        preset.fragments = new HashSet<>(equippedFragmentNames);
	
        for (int i = 0; i < allPresets.size(); i++) {
            if (allPresets.get(i).name.equalsIgnoreCase(presetName)) {
				if (activePreset == allPresets.get(i)) {
					activePreset = preset;
				}
                allPresets.set(i, preset);

				persistPresets();
                return;
            }
        }

        allPresets.add(preset);
		persistPresets();
    }

    private void deletePreset() {
        if (activePreset == null) {
            return;
        }

        chatboxPanelManager.openTextInput("Are you sure you want to delete the preset '" + activePreset.name + "'? (y/n)")
            .addCharValidator(FILTERED_CHARS)
            .onDone((presetName) -> {
                String trimmed = presetName.trim();
                if (trimmed.isEmpty()) {
                    return;
                }
                if (trimmed.toLowerCase().startsWith("y")) {
                    allPresets.remove(activePreset);
                    activePreset = null;
					persistPresets();
                }
            })
            .build();
    }

	private void loadPersistedPresets() {
		String json = configManager.getConfiguration(ShatteredRelicsFragmentPresetsConfig.CONFIG_GROUP,
				ShatteredRelicsFragmentPresetsConfig.ALL_PRESETS);
		if (json == null || json.isEmpty()) {
			log.info("null json");
			allPresets = new ArrayList<>();
		} else {
			log.info("got json " + json);
			allPresets = gson.fromJson(json, PRESET_LIST_TYPE);
		}
	}

	private void persistPresets() {
		String json = gson.toJson(allPresets, PRESET_LIST_TYPE);
		configManager.setConfiguration(ShatteredRelicsFragmentPresetsConfig.CONFIG_GROUP,
				ShatteredRelicsFragmentPresetsConfig.ALL_PRESETS, json);

		String json2 = configManager.getConfiguration(ShatteredRelicsFragmentPresetsConfig.CONFIG_GROUP,
				ShatteredRelicsFragmentPresetsConfig.ALL_PRESETS);
		log.info("persisted " + json2);

	}

	@Override
	public MouseEvent mousePressed(MouseEvent mouseEvent) {
		if (!showingFragments)
			return mouseEvent;
		if (mouseEvent.getButton() != 1)
			return mouseEvent;
		if (newPresetButtonBounds == null)
			return mouseEvent;

		if (newPresetButtonBounds.contains(mouseEvent.getPoint())) {
			newPreset();
			mouseEvent.consume();
			return mouseEvent;
		}

        if (deletePresetButtonBounds != null && deletePresetButtonBounds.contains(mouseEvent.getPoint())) {
            deletePreset();
            mouseEvent.consume();
            return mouseEvent;
        }

		for (Preset p : allPresets) {
			if (p.renderedBounds == null)
				continue;
			if (p.renderedBounds.contains(mouseEvent.getPoint())) {
				activePreset = p;
				mouseEvent.consume();
				return mouseEvent;
			}
		}

		return mouseEvent;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent mouseEvent) {
		return mouseEvent;
	}

	@Provides
	ShatteredRelicsFragmentPresetsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ShatteredRelicsFragmentPresetsConfig.class);
	}
}

// TODO:
// - auto scroll to fragment (check that empty preset doesn't break)
//   - turn on flow when clicking on a preset, even if it's alerady selected.
// whenever equipped presets change, scroll to next one. flow state ends when
// all are equipped or when the widget is closed.
// - fix overlay when filter list is up
// - somehow indicate how many fragments are in the preset?
//   - maybe show
// check all TODOs