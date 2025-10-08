package com.hbm.blocks.machine;

import api.hbm.energy.IEnergyConnectorBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.interfaces.IDummy;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;

public abstract class DummyOldBase extends BlockContainer implements IDummy, IEnergyConnectorBlock {

	public boolean port = false;

	public DummyOldBase(Material mat, String s, boolean port) {
		super(mat);
		this.port = port;
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setCreativeTab(null);
		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override 
	public boolean canConnect(IBlockAccess world, BlockPos pos, ForgeDirection dir){
		return port; 
	}
}
