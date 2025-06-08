package com.hbm.tileentity.conductor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hbm.forgefluid.FFPipeNetworkMk2;
import com.hbm.forgefluid.FFUtils;
import com.hbm.interfaces.IFluidPipeMk2;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.PipeUpdatePacket;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

import javax.annotation.Nonnull;

public class TileEntityFFDuctBaseMk2 extends TileEntity implements IFluidPipeMk2, IFluidHandler {

	public EnumFacing[] connections = new EnumFacing[6];
	protected Fluid type;
	protected FFPipeNetworkMk2 network = null;
	public boolean isBeingDestroyed = false;

	public TileEntityFFDuctBaseMk2() {}

	protected void attemptRebuildIfBroken() {
		if ((network != null && network.isValid()) || isBeingDestroyed) return;

		network = null;

		updateConnections();
		joinOrMakeNetwork();
		onNeighborChange();
	}

	public void setType(Fluid f) {
		if(f != type) {
			type = f;
			world.notifyNeighborsOfStateChange(pos, getBlockType(), true);
			world.neighborChanged(pos, getBlockType(), pos);
			IBlockState state = world.getBlockState(pos);
			world.markAndNotifyBlock(pos, world.getChunkFromBlockCoords(pos), state, state, 2);
			rebuildNetworks(world, pos);
			if(world instanceof WorldServer) {
				PlayerChunkMapEntry entry = ((WorldServer) world).getPlayerChunkMap().getEntry(MathHelper.floor(pos.getX()) >> 4, MathHelper.floor(pos.getZ()) >> 4);

				if(entry != null) {
					for(EntityPlayerMP player : entry.getWatchingPlayers()) {
						player.connection.sendPacket(new SPacketUpdateTileEntity(pos, 0, writeToNBT(new NBTTagCompound())));
					}
				}
			}
			if(!world.isRemote)
				PacketDispatcher.wrapper.sendToAllTracking(new PipeUpdatePacket(pos, 1), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
		}
	}

	public Fluid getType() {
		return type;
	}

	@Override
	public @Nonnull NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
		if(type != null)
			compound.setString("fluidType", type.getName());
		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if(compound.hasKey("fluidType"))
			this.type = FluidRegistry.getFluid(compound.getString("fluidType"));
		super.readFromNBT(compound);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket(){
		return new SPacketUpdateTileEntity(this.getPos(), 0, this.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public @Nonnull NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	@Override
		public void onDataPacket(@Nonnull NetworkManager net, SPacketUpdateTileEntity pkt) {
			this.readFromNBT(pkt.getNbtCompound());
		}

	@Override
	public void handleUpdateTag(@Nonnull NBTTagCompound tag) {
		Fluid f = this.type;
		this.readFromNBT(tag);
		if(f == type)
			return;
		for(EnumFacing e : EnumFacing.VALUES) {
			TileEntity te = world.getTileEntity(pos.offset(e));
			if(te instanceof TileEntityFFDuctBaseMk2)
				((TileEntityFFDuctBaseMk2) te).onNeighborChange();
		}
		this.onNeighborChange();
	}

	// Probably called before neighbor changed
	@Override
	public void onLoad() {
		updateConnections();
		joinOrMakeNetwork();
		onNeighborChange();
	}

	public void onNeighborChange() {
		updateConnections();

		if (network != null) {
			for(EnumFacing e : connections) {
				if (e == null) continue;

				BlockPos offset = pos.offset(e);
				TileEntity tileEntity = world.getTileEntity(offset);
				if (tileEntity != null) {
					network.tryAdd(tileEntity);
				} else {
					network.tryRemovePipe(offset);
					network.tryRemoveConsumer(offset);
				}
			}
		}

		if(!world.isRemote)
			PacketDispatcher.wrapper.sendToAllTracking(new PipeUpdatePacket(pos), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
	}

	@Override
	public void onChunkUnload() {
		if(network == null)
			return;

		// Remove the TEs from the network that this duct is connected to
		for (EnumFacing e : connections) {
			if (e == null) continue;
			BlockPos pos = this.pos.offset(e);
			network.tryRemoveConsumer(pos);
		}

		network.checkForRemoval(this);
		this.network = null;
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	// Drillgon200: Has to be static because breakBlock doesn't get called on
	// client, and the tile entity is gone before a packet can reach it.
	public static void breakBlock(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2) {
			((TileEntityFFDuctBaseMk2) te).isBeingDestroyed = true;
		}

		rebuildNetworks(world, pos);
	}

	public static void rebuildNetworks(World world, BlockPos pos) {
		TileEntity center = world.getTileEntity(pos);
		EnumFacing[] dirs = EnumFacing.VALUES;
		if (center instanceof TileEntityFFDuctBaseMk2) {
			TileEntityFFDuctBaseMk2 duct = (TileEntityFFDuctBaseMk2) center;
			duct.updateConnections();
			dirs = duct.connections;
		}

		for(EnumFacing e : dirs) {
			if (e == null) continue;

			TileEntity te = world.getTileEntity(pos.offset(e));
			if(te instanceof IFluidPipeMk2) {
				IFluidPipeMk2 pipe = (IFluidPipeMk2) te;
				if(pipe.getNetwork() != null)
					pipe.getNetwork().destroy();
			}
		}

		if(center instanceof IFluidPipeMk2 && ((IFluidPipeMk2) center).getNetwork() != null)
			((IFluidPipeMk2) center).getNetwork().destroy();

		for(EnumFacing e : dirs) {
			if (e == null) continue;
			FFPipeNetworkMk2.buildNetwork(world.getTileEntity(pos.offset(e)));
		}

		FFPipeNetworkMk2.buildNetwork(center);
	}

	@Override
	public void joinOrMakeNetwork() {
		List<FFPipeNetworkMk2> otherNetworks = new ArrayList<FFPipeNetworkMk2>();
		for(EnumFacing e : connections) {
			if (e == null) continue;

			BlockPos offset = pos.offset(e);
			TileEntity te = world.getTileEntity(offset);
			if(te instanceof IFluidPipeMk2) {
				IFluidPipeMk2 pipe = (IFluidPipeMk2) te;
				if(pipe.getNetwork() != null && pipe.getNetwork().getType() == this.getType() && !otherNetworks.contains(pipe.getNetwork())) {
					otherNetworks.add(pipe.getNetwork());
				}
			}
		}

		if(otherNetworks.isEmpty()) {
			network = new FFPipeNetworkMk2(this);
			network.tryAdd(this);
		} else {
			FFPipeNetworkMk2 net = otherNetworks.remove(0);
			while(!otherNetworks.isEmpty())
				net = FFPipeNetworkMk2.mergeNetworks(net, otherNetworks.remove(0));
			network = net;
			net.tryAdd(this);
		}
	}

	public void dumpState(EntityPlayer player) {
		player.sendMessage(new TextComponentString("Dumping state for the fluid duct at " + pos));
		player.sendMessage(new TextComponentString("--------------------------------"));
		player.sendMessage(new TextComponentString("DUCT TE STATE"));
		player.sendMessage(new TextComponentString("Instance: " + this));
		player.sendMessage(new TextComponentString("Connections: " + Arrays.toString(connections)));
		player.sendMessage(new TextComponentString("Type: " + (type == null ? "None" : type.getName())));
		player.sendMessage(new TextComponentString("Network: " + (network == null ? "None" : network.toString())));
		player.sendMessage(new TextComponentString("Is being destroyed: " + isBeingDestroyed));
		player.sendMessage(new TextComponentString("--------------------------------"));

		if (network != null) {
			player.sendMessage(new TextComponentString("NETWORK STATE"));
			player.sendMessage(new TextComponentString("Valid: " + network.isValid()));
			player.sendMessage(new TextComponentString("Type: " + (network.getType() == null ? "None" : network.getType().getName())));
			player.sendMessage(new TextComponentString("Size: " + network.size()));
			player.sendMessage(new TextComponentString("Pipe Count: " + network.pipes.size()));
			player.sendMessage(new TextComponentString("Fillables Count: " + network.fillables.size()));

			player.sendMessage(new TextComponentString("Fillables: "));
			for (Map.Entry<BlockPos, TileEntity> fillable : network.fillables.entrySet()) {
				player.sendMessage(new TextComponentString("  " + fillable.getKey() + ": " + fillable.getValue()));
			}
		} else {
			player.sendMessage(new TextComponentString("NETWORK DUMP SKIPPED, NO NETWORK"));
		}
	}

	public void updateConnections() {
		if(FFUtils.checkFluidConnectablesMk2(this.world, pos.up(), getType(), EnumFacing.UP.getOpposite()))
			connections[0] = EnumFacing.UP;
		else
			connections[0] = null;

		if(FFUtils.checkFluidConnectablesMk2(this.world, pos.down(), getType(), EnumFacing.DOWN.getOpposite()))
			connections[1] = EnumFacing.DOWN;
		else
			connections[1] = null;

		if(FFUtils.checkFluidConnectablesMk2(this.world, pos.north(), getType(), EnumFacing.NORTH.getOpposite()))
			connections[2] = EnumFacing.NORTH;
		else
			connections[2] = null;

		if(FFUtils.checkFluidConnectablesMk2(this.world, pos.east(), getType(), EnumFacing.EAST.getOpposite()))
			connections[3] = EnumFacing.EAST;
		else
			connections[3] = null;

		if(FFUtils.checkFluidConnectablesMk2(this.world, pos.south(), getType(), EnumFacing.SOUTH.getOpposite()))
			connections[4] = EnumFacing.SOUTH;
		else
			connections[4] = null;

		if(FFUtils.checkFluidConnectablesMk2(this.world, pos.west(), getType(), EnumFacing.WEST.getOpposite()))
			connections[5] = EnumFacing.WEST;
		else
			connections[5] = null;
	}

	@Override
	public FFPipeNetworkMk2 getNetwork() {
		return network;
	}

	@Override
	public void setNetwork(FFPipeNetworkMk2 net) {
		network = net;
	}

	@Override
	public boolean isValidForBuilding() {
		return !isBeingDestroyed;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return network != null ? network.getTankProperties() : new IFluidTankProperties[] {};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return network != null ? network.fill(resource, doFill) : 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return network != null ? network.drain(resource, doDrain) : null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return network != null ? network.drain(maxDrain, doDrain) : null;
	}

	@Override
	public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this) : super.getCapability(capability, facing);
	}
}
