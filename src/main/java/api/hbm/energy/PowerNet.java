package api.hbm.energy;

import com.hbm.util.Tuple;

import java.util.*;

public class PowerNet {

    public boolean valid = true;
    public Set<Nodespace.PowerNode> links = new HashSet();

    /** Maps all active subscribers to a timestamp, handy for handling timeouts. In a good system this shouldn't be necessary, but the previous system taught me to be cautious anyway */
    public HashMap<IEnergyUser, Long> receiverEntries = new HashMap();
    public HashMap<IEnergyGenerator, Long> providerEntries = new HashMap();

    public long energyTracker = 0L;

    public PowerNet() {
        Nodespace.activePowerNets.add(this);
    }

    /// SUBSCRIBER HANDLING ///
    public void addReceiver(IEnergyUser receiver) {
        this.receiverEntries.put(receiver, System.currentTimeMillis());
    }

    public void removeReceiver(IEnergyUser receiver) {
        this.receiverEntries.remove(receiver);
    }

    /// PROVIDER HANDLING ///
    public void addProvider(IEnergyGenerator provider) {
        this.providerEntries.put(provider, System.currentTimeMillis());
    }

    public void removeProvider(IEnergyGenerator provider) {
        this.providerEntries.remove(provider);
    }

    /// LINK JOINING ///

    /** Combines two networks into one */
    public void joinNetworks(PowerNet network) {

        if(network == this) return; //wtf?!

        List<Nodespace.PowerNode> oldNodes = new ArrayList(network.links.size());
        oldNodes.addAll(network.links); // might prevent oddities related to joining - nvm it does nothing

        for(Nodespace.PowerNode conductor : oldNodes) forceJoinLink(conductor);
        network.links.clear();

        for(IEnergyUser connector : network.receiverEntries.keySet()) this.addReceiver(connector);
        for(IEnergyGenerator connector : network.providerEntries.keySet()) this.addProvider(connector);
        network.destroy();
    }

    /** Adds the power node as part of this network's links */
    public PowerNet joinLink(Nodespace.PowerNode node) {
        if(node.net != null) node.net.leaveLink(node);
        return forceJoinLink(node);
    }

    /** Adds the power node as part of this network's links, skips the part about removing it from existing networks */
    public PowerNet forceJoinLink(Nodespace.PowerNode node) {
        this.links.add(node);
        node.setNet(this);
        return this;
    }

    /** Removes the specified power node */
    public void leaveLink(Nodespace.PowerNode node) {
        node.setNet(null);
        this.links.remove(node);
    }

    /// GENERAL POWER NET CONTROL ///
    public void invalidate() {
        this.valid = false;
        Nodespace.activePowerNets.remove(this);
    }

    public boolean isValid() {
        return this.valid;
    }

    public void destroy() {
        this.invalidate();
        for(Nodespace.PowerNode link : this.links) if(link.net == this) link.setNet(null);
        this.links.clear();
        this.receiverEntries.clear();
        this.providerEntries.clear();
    }

    public void resetEnergyTracker() {
        this.energyTracker = 0;
    }

    protected static int timeout = 3_000;

    public void transferPower() {

        if(providerEntries.isEmpty()) return;
        if(receiverEntries.isEmpty()) return;

        long timestamp = System.currentTimeMillis();
        long transferCap = 10_000_000_000_000_000L;

        List<Tuple.Pair<IEnergyGenerator, Long>> providers = new ArrayList<>();
        long powerAvailable = 0;

        Iterator<Map.Entry<IEnergyGenerator, Long>> provIt = providerEntries.entrySet().iterator();
        while(provIt.hasNext()) {
            Map.Entry<IEnergyGenerator, Long> entry = provIt.next();
            if(timestamp - entry.getValue() > timeout) { provIt.remove(); continue; }
            long src = Math.min(entry.getKey().getPower(), entry.getKey().getProviderSpeed());
            providers.add(new Tuple.Pair<>(entry.getKey(), src));
            powerAvailable = Math.min(powerAvailable + src, Long.MAX_VALUE);
        }

        List<Tuple.Pair<IEnergyUser, Long>>[] receivers = new ArrayList[IEnergyUser.ConnectionPriority.values().length];
        for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList<>();
        long[] demand = new long[IEnergyUser.ConnectionPriority.values().length];
        long totalDemand = 0;

        Iterator<Map.Entry<IEnergyUser, Long>> recIt = receiverEntries.entrySet().iterator();

        while(recIt.hasNext()) {
            Map.Entry<IEnergyUser, Long> entry = recIt.next();
            if(timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxPower() - entry.getKey().getPower(), entry.getKey().getReceiverSpeed());
            int p = entry.getKey().getPriority().ordinal();
            receivers[p].add(new Tuple.Pair<>(entry.getKey(), rec));
            demand[p] += rec;
            totalDemand += rec;
        }

        long toTransfer = Math.min(Math.min(powerAvailable, transferCap), totalDemand);
        long energyUsed = 0;

        for(int i = IEnergyUser.ConnectionPriority.values().length - 1; i >= 0; i--) {
            List<Tuple.Pair<IEnergyUser, Long>> list = receivers[i];
            long priorityDemand = demand[i];

            for(Tuple.Pair<IEnergyUser, Long> entry : list) {
                double weight = (double) entry.getValue() / (double) (priorityDemand);
                long toSend = (long) Math.max(toTransfer * weight, 0D);
                energyUsed += (toSend - entry.getKey().transferPower(toSend)); //leftovers are subtracted from the intended amount to use up
            }

            toTransfer -= energyUsed;
        }

        this.energyTracker += energyUsed;

        for(Tuple.Pair<IEnergyGenerator, Long> entry : providers) {
            double weight = (double) entry.getValue() / (double) powerAvailable;
            long toUse = (long) Math.max(energyUsed * weight, 0D);
            entry.getKey().usePower(toUse);
        }
    }

    public long sendPowerDiode(long power) {

        if(receiverEntries.isEmpty()) return power;

        long timestamp = System.currentTimeMillis();

        List<Tuple.Pair<IEnergyUser, Long>>[] receivers = new ArrayList[IEnergyUser.ConnectionPriority.values().length];
        for(int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList<>();
        long[] demand = new long[IEnergyUser.ConnectionPriority.values().length];
        long totalDemand = 0;

        Iterator<Map.Entry<IEnergyUser, Long>> recIt = receiverEntries.entrySet().iterator();

        while(recIt.hasNext()) {
            Map.Entry<IEnergyUser, Long> entry = recIt.next();
            if(timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxPower() - entry.getKey().getPower(), entry.getKey().getReceiverSpeed());
            int p = entry.getKey().getPriority().ordinal();
            receivers[p].add(new Tuple.Pair<>(entry.getKey(), rec));
            demand[p] += rec;
            totalDemand += rec;
        }

        long toTransfer = Math.min(power, totalDemand);
        long energyUsed = 0;

        for(int i = IEnergyUser.ConnectionPriority.values().length - 1; i >= 0; i--) {
            List<Tuple.Pair<IEnergyUser, Long>> list = receivers[i];
            long priorityDemand = demand[i];

            for(Tuple.Pair<IEnergyUser, Long> entry : list) {
                double weight = (double) entry.getValue() / (double) (priorityDemand);
                long toSend = (long) Math.max(toTransfer * weight, 0D);
                energyUsed += (toSend - entry.getKey().transferPower(toSend)); //leftovers are subtracted from the intended amount to use up
            }

            toTransfer -= energyUsed;
        }

        this.energyTracker += energyUsed;

        return power - energyUsed;
    }
}