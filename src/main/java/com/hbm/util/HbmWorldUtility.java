package com.hbm.util;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

public class HbmWorldUtility {

	public static void setImmediateScheduledUpdates(World world, boolean update){
		world.scheduledUpdatesAreImmediate = update;
	}

	public static World getProviderWorld(WorldProvider provider){
		return provider.world;
	}
}
