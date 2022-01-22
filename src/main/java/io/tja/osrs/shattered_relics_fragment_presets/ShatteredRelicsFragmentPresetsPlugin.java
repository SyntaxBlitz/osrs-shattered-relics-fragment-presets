package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.inject.Provides;
import javax.inject.Inject;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.function.Consumer;
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

	private int tickTimer = 0;

	public List<Preset> allPresets = new ArrayList<>();
	public Preset activePreset;

	@Override
	protected void startUp() throws Exception {
		Preset p = new Preset();
		p.name = "Gathering stuff";
		p.fragments = new HashSet<>();
		p.fragments.add("Message In A Bottle");
		p.fragments.add("Rock Solid");
		p.fragments.add("Molten Miner");
		p.fragments.add("Chef's Catch");

		Preset p2 = new Preset();
		p2.name = "Combat";
		p2.fragments = new HashSet<>();
		p2.fragments.add("Unholy Warrior");
		p2.fragments.add("Unholy Ranger");
		p2.fragments.add("Unholy Wizard");

		allPresets.add(p);
		allPresets.add(p2);

		activePreset = p;

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

	// @Subscribe
	// public void onGameStateChanged(GameStateChanged gameStateChanged)
	// {
	// if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
	// {
	// client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " +
	// config.greeting(), null);
	// }
	// }

	// @Subscribe
	// public void onScriptPreFired(ScriptPreFired event) {
	// int scriptId = event.getScriptId();
	// if (scriptId == 4731 || scriptId == 4730 || scriptId == 4731 || scriptId ==
	// 4671 || scriptId == 4672 || scriptId == 2512 || scriptId == 2513 || scriptId
	// == 1004 || scriptId == 4730 || scriptId == 4731 || scriptId == 3350 ||
	// scriptId == 3351 || scriptId == 2100 || scriptId == 2101 || scriptId == 4730
	// || scriptId == 4731 || scriptId == 4730 || scriptId == 4731 || scriptId ==
	// 4730 || scriptId == 4731 || scriptId == 4671 || scriptId == 4672 || scriptId
	// == 2512 || scriptId == 2513 || scriptId == 1004 || scriptId == 4730 ||
	// scriptId == 4731) return;
	// if (scriptId == 998 || scriptId == 900 || scriptId == 2250 || scriptId ==
	// 1972 || scriptId == 100 || scriptId == 1445 || scriptId == 2476) return;
	//
	// // sus: 5796, 5795
	//
	//// if (scriptId != 5796 && scriptId != 5795) return;
	//
	// log.info("script id " + scriptId);
	//// log.info(event.getScriptEvent());
	// }

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

	// @Subscribe
	// public void onWidgetLoaded(WidgetLoaded event) {
	// log.info("widget group " + event.getGroupId());
	// // log.info("widget " + getWidgetString(event.));
	// // 654: info
	// // 657: tasks
	// // 733: unlocks
	// if (event.getGroupId() == 735) { // fragment screen
	// log.info("wpx " + Arrays.toString(client.getWidgetPositionsX()));
	// // client.getWidgetPositionsY();
	// // 17: left list
	// Widget w = client.getWidget(735, 17);
	// log.info("wcl " + w.getCanvasLocation());
	// log.info("wi " + w.getIndex());
	// log.info("wox " + w.getOriginalX());
	// log.info("ncl " + w.getNestedChildren().length);
	// log.info("scl " + w.getStaticChildren().length);
	// log.info("dcl " + w.getDynamicChildren().length);

	// log.info("w " + w);

	// log.info("scrollHeight " + w.getScrollHeight());
	// log.info("scrollWidth " + w.getScrollWidth());
	// log.info("scrollX " + w.getScrollX());
	// log.info("scrollY " + w.getScrollY());

	// log.info("relativeX " + w.getRelativeX());
	// log.info("relativeY " + w.getRelativeY());

	// log.info("bounds " + w.getBounds());

	// // log.info("wi size " + w.getWidgetItems().size());

	// for (Widget w2 : w.getDynamicChildren()) {
	// if (getWidgetString(w2).contains("Unholy")) {
	// log.info("w2i " + w.getIndex());
	// log.info("w2cl " + w.getCanvasLocation());
	// log.info("w2 relativey " + w2.getBounds() + " " + getWidgetString(w2) + " " +
	// w2.getRelativeY());
	// log.info("parent " + w2.getParent());
	// // log.info("hiding " + getWidgetString(w2));
	// // w2.setHidden(true);
	// }
	// }
	// // for (int i = 0; i < 10000; i++) {
	// // Widget w = client.getWidget(735, i);
	// // if (w != null) {
	// // log.info(i + ": " + getWidgetString(w));
	// //// Widget[] nc = w.getNestedChildren();
	// //// if (nc == null) {
	// ////
	// //// } else {
	// ////
	// //// }
	// // }
	// // }
	// }
	// }

	@Subscribe
	public void onClientTick(ClientTick event) {
		tickTimer++;

		// Widget devWidget = client.getWidget(735, 1);
		// if (devWidget != null)
		// devBounds = devWidget.getBounds();
		// devBounds = null;

		Widget fragmentWindow = client.getWidget(735, 1);
		if (fragmentWindow == null) {
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
				.filter(widget -> activePreset.fragments.contains(Text.removeTags(widget.getName())))
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
			String widgetString = getWidgetString(subWidget);
			if (activePreset.fragments.contains(widgetString.trim())) {
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
                if (presetName.isEmpty()) {
                    return;
                }
                savePreset(presetName);
            })
            .build();
    }

    private void savePreset(String presetName) {
        Preset preset = new Preset();
        preset.name = presetName;
        preset.fragments = new HashSet<>(equippedFragmentNames);
	
        for (int i = 0; i < allPresets.size(); i++) {
            if (allPresets.get(i).name.equalsIgnoreCase(presetName)) {
                allPresets.set(i, preset);
                return;
            }
        }

        allPresets.add(preset);
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
// - "new preset"
// - "delete preset"
// - persistent presets
// - auto scroll to fragment
// - turn on flow when clicking on a preset, even if it's alerady selected.
// whenever equipped presets change, scroll to next one. flow state ends when
// all are equipped or when the widget is closed.
// fix overlay when filter list is up
// somehow indicate how many fragments are in the preset?