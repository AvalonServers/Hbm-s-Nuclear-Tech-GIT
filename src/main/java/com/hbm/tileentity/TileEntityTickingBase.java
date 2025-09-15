package com.hbm.tileentity;

import com.hbm.packet.NBTPacket;
import com.hbm.packet.PacketDispatcher;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

import java.util.Random;

public abstract class TileEntityTickingBase extends TileEntityLoadedBase implements ITickable, INBTPacketReceiver {
	public static final Random rand = new Random();
	public final int updateOffset = rand.nextInt(20);
	private boolean networkDirty = false;

	public abstract String getInventoryName();
	
	public int getGaugeScaled(int i, FluidTank tank) {
		return tank.getFluidAmount() * i / tank.getCapacity();
	}
	
	public void networkPack(NBTTagCompound nbt, int range) {

		if(!world.isRemote)
			PacketDispatcher.wrapper.sendToAllAround(new NBTPacket(nbt, pos), new TargetPoint(this.world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), range));
	}
	
	public void networkUnpack(NBTTagCompound nbt) { }

	public void setNetworkDirty() {
		if (world.isRemote) return;
		networkDirty = true;
	}

	public boolean shouldSendNetworkUpdate() {
		if (world.isRemote) return false;

		if (networkDirty) {
			networkDirty = false;
			return true;
		}

		// VERTEX: Testing sending updates to the client only once per second for perf, TODO make this use wall clock time? doesn't really matter though since it's tq excluded
		return (world.getTotalWorldTime() + updateOffset) % 20 == 0;
	}
}
