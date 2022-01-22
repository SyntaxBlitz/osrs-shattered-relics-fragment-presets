package io.tja.osrs.shattered_relics_fragment_presets;

import java.awt.*;

public class FragmentData implements Comparable<FragmentData> {
    Rectangle widgetBounds;
    boolean isEquipped;
    double scrollPercentage;

    @Override
    public int compareTo(FragmentData o) {
        return Double.compare(scrollPercentage, o.scrollPercentage);
    }
}