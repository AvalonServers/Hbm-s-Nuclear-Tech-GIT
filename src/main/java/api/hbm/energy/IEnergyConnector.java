package api.hbm.energy;

import com.hbm.lib.ForgeDirection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IEnergyConnector {

    /**
     * Whether the given side can be connected to
     * dir refers to the side of this block, not the connecting block doing the check
     * @param dir
     * @return
     */
    public default boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN;
    }
}