package io.tja.osrs.shattered_relics_fragment_presets;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ShatteredRelicsFragmentPresetsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ShatteredRelicsFragmentPresetsPlugin.class);
		RuneLite.main(args);
	}
}