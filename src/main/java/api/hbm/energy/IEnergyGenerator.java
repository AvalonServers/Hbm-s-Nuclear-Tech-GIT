package api.hbm.energy;

import com.hbm.lib.ForgeDirection;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.Compat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/** If it sends energy, use this */
public interface IEnergyGenerator extends IEnergyHandler {

    /** Uses up available power, default implementation has no sanity checking, make sure that the requested power is lequal to the current power */
    public default void usePower(long power) {
        this.setPower(this.getPower() - power);
    }

    public default long getProviderSpeed() {
        return this.getMaxPower();
    }

    public default void sendPower(World world, BlockPos pos, ForgeDirection dir) {

        TileEntity te = Compat.getTileStandard(world, pos.getX(), pos.getY(), pos.getZ());
        boolean red = false;

        if(te instanceof IEnergyConductor) {
            IEnergyConductor con = (IEnergyConductor) te;
            if(con.canConnect(dir.getOpposite())) {

                Nodespace.PowerNode node = Nodespace.getNode(world, pos);

                if(node != null && node.net != null) {
                    node.net.addProvider(this);
                    red = true;
                }
            }
        }

        if(te instanceof IEnergyUser && te != this) {
            IEnergyUser rec = (IEnergyUser) te;
            if(rec.canConnect(dir.getOpposite())) {
                long provides = Math.min(this.getPower(), this.getProviderSpeed());
                long receives = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
                long toTransfer = Math.min(provides, receives);
                toTransfer -= rec.transferPower(toTransfer);
                this.usePower(toTransfer);
            }
        }

        if(particleDebug) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("type", "network");
            data.setString("mode", "power");
            double posX = pos.getX() + 0.5 - dir.offsetX * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
            double posY = pos.getY() + 0.5 - dir.offsetY * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
            double posZ = pos.getZ() + 0.5 - dir.offsetZ * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
            data.setDouble("mX", dir.offsetX * (red ? 0.025 : 0.1));
            data.setDouble("mY", dir.offsetY * (red ? 0.025 : 0.1));
            data.setDouble("mZ", dir.offsetZ * (red ? 0.025 : 0.1));
            PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, posX, posY, posZ), new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 25));
        }
    }

    public default void sendPower(World world, BlockPos pos){
        for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
            this.sendPower(world, pos.add(dir.offsetX, dir.offsetY, dir.offsetZ), dir);
    }
}