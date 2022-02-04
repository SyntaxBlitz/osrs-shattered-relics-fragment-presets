package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
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
import java.util.*;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

@PluginDescriptor(name = "Fragment Presets")
public class ShatteredRelicsFragmentPresetsPlugin extends Plugin implements MouseListener {
    private final Set<String> FRAGMENTS = new HashSet<String>(Arrays.asList(new String[]{
        "Alchemaniac",
        "Arcane Conduit",
        "Armadylean Decree",
        "Bandosian Might",
        "Barbarian Pest Wars",
        "Bottomless Quiver",
        "Catch Of The Day",
        "Certified Farmer",
        "Chef's Catch",
        "Chinchonkers",
        "Clued In",
        "Deeper Pockets",
        "Dine & Dash",
        "Divine Restoration",
        "Dragon On a Bit",
        "Enchanted Jeweler",
        "Golden Brick Road",
        "Grave Robber",
        "Homewrecker",
        "Hot on the Trail",
        "Imcando's Apprentice",
        "Just Druid!",
        "Larger Recharger",
        "Livin' On A Prayer",
        "Message In A Bottle",
        "Mixologist",
        "Molten Miner",
        "Mother's Magic Fossils",
        "Plank Stretcher",
        "Praying Respects",
        "Pro Tips",
        "Profletchional",
        "Rock Solid",
        "Rogues' Chompy Farm",
        "Rooty Tooty 2x Runeys",
        "Rumple-Bow-String",
        "Rune Escape",
        "Saradominist Defence",
        "Seedy Business",
        "Slash & Burn",
        "Slay 'n' Pay",
        "Slay All Day",
        "Smithing Double",
        "Smooth Criminal",
        "Special Discount",
        "Superior Tracking",
        "Tactical Duelist",
        "Thrall Damage",
        "Unholy Ranger",
        "Unholy Warrior",
        "Unholy Wizard",
        "Venomaster",
        "Zamorakian Sight"
    }).stream().map(s -> s.toLowerCase()).collect(Collectors.toList()));

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
    private boolean scrollFlowActive = false;
    private Set<String> lastEquippedFragmentsForScrollFlow = null;
    public boolean suppressFilterOverlay = false;
    public Rectangle fragmentWindowBounds = null;
    public Rectangle fragmentListBounds = null;
    public Rectangle fragmentScrollbarInnerBounds = null;

    public Set<FragmentData> fragmentData;
    public Set<Rectangle> presetEquippedFragmentBounds;
    public Set<String> equippedFragmentNames = new HashSet<>();

    public Rectangle newPresetButtonBounds; // set by overlay
    public Rectangle deletePresetButtonBounds; // set by overlay

    public List<Preset> allPresets = new ArrayList<>();
    public static Type PRESET_LIST_TYPE = new TypeToken<List<Preset>>() {
    }.getType();
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

    private void updateDragAndDropBehavior(Collection<Widget> widgets) {
        switch (config.dragMode()) {
            case ENABLED:
                break;
            case IMPROVED:
                for (Widget widget : widgets) {
                    widget.setDragDeadTime(10);
                    widget.setDragDeadZone(16);
                }
                break;
            case DISABLED:
                for (Widget widget : widgets) {
                    widget.setDragDeadTime(Integer.MAX_VALUE);
                }
        }

    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        Widget fragmentWindow = client.getWidget(735, 1);
        if (fragmentWindow == null) {
            activePreset = null;
            showingFragments = false;
            scrollFlowActive = false;
            return;
        }

        Widget showFiltersButton = client.getWidget(735, 9);
        suppressFilterOverlay = showFiltersButton.getText().equals(("Hide Filters"));

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

        Set<Widget> draggableWidgets =
                Arrays.stream(fragmentList.getDynamicChildren())
                        .filter(widget -> widget.getDragDeadTime() > 0)
                        .collect(Collectors.toSet());
        draggableWidgets.addAll(equippedFragmentWidgets);
        updateDragAndDropBehavior(draggableWidgets);

        presetEquippedFragmentBounds = equippedFragmentWidgets.stream()
                .filter(widget -> activePreset != null
                        && activePreset.fragments.contains(Text.removeTags(widget.getName())))
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
            if (activePreset != null && activePreset.fragments.contains(subWidget.getText())) {
                Widget containerSubWidget = fragmentListSubWidgets[i - 7];
                FragmentData fragmentData = new FragmentData();
                fragmentData.widgetBounds = containerSubWidget.getBounds();
                fragmentData.isEquipped = equippedFragmentNames.contains(subWidget.getText());
                fragmentData.scrollPercentage = containerSubWidget.getRelativeY() / totalScrollHeight;

                theseFragmentData.add(fragmentData);
            }
        }

        fragmentData = theseFragmentData;

        if (scrollFlowActive && activePreset != null) { // activePreset should always be non-null here but good to check
            if (!equippedFragmentNames.equals(lastEquippedFragmentsForScrollFlow)) {
                long oldInPresetCount = lastEquippedFragmentsForScrollFlow == null ? 0 :
                        lastEquippedFragmentsForScrollFlow.stream().filter(name -> activePreset.fragments.contains(name)).count();
                long newInPresetCount =
                        equippedFragmentNames.stream().filter(name -> activePreset.fragments.contains(name)).count();
                if (newInPresetCount < oldInPresetCount) {
                    // if we just unequipped a fragment that IS in the preset, unselect the preset. we're not using it
                    // right now, obviously. useful for when you're creating a new preset off of an old one
                    scrollFlowActive = false;
                    activePreset = null;
                } else {
                    // scroll to the next one
                    FragmentData[] sortedByScrollY = fragmentData.toArray(new FragmentData[0]);
                    Arrays.sort(sortedByScrollY);
                    FragmentData nextFragment = Arrays.stream(sortedByScrollY)
                            .filter(fragment -> !fragment.isEquipped)
                            .findFirst()
                            .orElse(null);
                    if (nextFragment == null) {
                        scrollFlowActive = false;
                    } else {
                        int scrollY = (int) (totalScrollHeight * nextFragment.scrollPercentage);
                        // shouldn't be necessary but let's be safe. not sure scrollPercentage will be
                        // 0-1
                        int clampedScroll = (int) Math.max(0, Math.min(totalScrollHeight, scrollY));
                        fragmentList.setScrollY(clampedScroll);
                        client.runScript(
                                ScriptID.UPDATE_SCROLLBAR,
                                fragmentScrollbar.getId(),
                                fragmentList.getId(),
                                clampedScroll);

                    }
                }
            }

            lastEquippedFragmentsForScrollFlow = equippedFragmentNames;
        }

        showingFragments = true;
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (
                !config.shiftClickToggleFragment() ||
                !client.isKeyPressed(KeyCode.KC_SHIFT) ||
                !event.getOption().equals("View") ||
                event.getType() != MenuAction.CC_OP.getId() ||
                event.getTarget().length() == 0
        ) return;

        String target = Text.removeTags(event.getTarget());

        if (FRAGMENTS.contains(target.toLowerCase())) {
            MenuEntry[] entries = client.getMenuEntries();
            client.setMenuEntries(Arrays.copyOf(entries, entries.length - 1));
        }
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

        chatboxPanelManager
                .openTextMenuInput("Are you sure you want to delete the preset '" + activePreset.name + "'?")
                .option("Delete", () -> {
                    allPresets.remove(activePreset);
                    activePreset = null;
                    persistPresets();
                })
                .option("Nevermind!", () -> {
                })
                .build();
    }

    private void loadPersistedPresets() {
        String json = configManager.getConfiguration(ShatteredRelicsFragmentPresetsConfig.CONFIG_GROUP,
                ShatteredRelicsFragmentPresetsConfig.ALL_PRESETS);
        if (json == null || json.isEmpty()) {
            allPresets = new ArrayList<>();
        } else {
            allPresets = gson.fromJson(json, PRESET_LIST_TYPE);
        }
    }

    private void persistPresets() {
        String json = gson.toJson(allPresets, PRESET_LIST_TYPE);
        configManager.setConfiguration(ShatteredRelicsFragmentPresetsConfig.CONFIG_GROUP,
                ShatteredRelicsFragmentPresetsConfig.ALL_PRESETS, json);
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
                scrollFlowActive = true;
                lastEquippedFragmentsForScrollFlow = null;
                mouseEvent.consume();
                return mouseEvent;
            }
        }

        return mouseEvent;
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        switch (event.getGroupId()) {
            case WidgetID.FIXED_VIEWPORT_GROUP_ID:
                this.sidebarOverlay.setIsFixedViewport(true);
                break;
            case WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID:
            case WidgetID.RESIZABLE_VIEWPORT_BOTTOM_LINE_GROUP_ID:
                this.sidebarOverlay.setIsFixedViewport(false);
                break;
        }
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

    public int getOffsetX() {
        return config.resizableOffsetX();
    }

    public int getOffsetY() {
        return config.resizableOffsetY();
    }

    @Provides
    ShatteredRelicsFragmentPresetsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ShatteredRelicsFragmentPresetsConfig.class);
    }
}