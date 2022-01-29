package io.tja.osrs.shattered_relics_fragment_presets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

@PluginDescriptor(name = "Fragment Presets")
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

        if (config.shitClickEquipFragment() && !client.isMenuOpen() && client.isKeyPressed(KeyCode.KC_SHIFT)) {
            swapFragmentsMenuEntry();
        }

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

        chatboxPanelManager
                .openTextMenuInput("Are you sure you want to delete the preset '" + activePreset.name + "'?")
                .option("Delete", () -> {
                    allPresets.remove(activePreset);
                    activePreset = null;
                    persistPresets();
                })
                .option("Nevermind!", () -> {})
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

    private void swapFragmentsMenuEntry() {
        /* This will only be called when the fragment window is open, we can do some simple filtering to ensure
            we have the correct menu */
        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries.length != 3) return;
        int equipIndex = 1;
        int viewIndex = 2;
        // Sanity check "Cancel" option
        int cancelIndex = 0;

        boolean equipExists = (Text.removeTags(menuEntries[equipIndex].getOption()).equals("Equip"));
        boolean viewExists = Text.removeTags(menuEntries[viewIndex].getOption()).equals("View");
        boolean cancelExists = Text.removeTags(menuEntries[cancelIndex].getOption()).equals("Cancel");


        if (equipExists && viewExists && cancelExists) {
            MenuEntry leftClickEntry = menuEntries[equipIndex];
            MenuEntry entry2 = menuEntries[viewIndex];

            menuEntries[viewIndex] = leftClickEntry;
            menuEntries[equipIndex] = entry2;

            client.setMenuEntries(menuEntries);
        }
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