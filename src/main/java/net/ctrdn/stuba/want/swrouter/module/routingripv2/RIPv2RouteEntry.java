package net.ctrdn.stuba.want.swrouter.module.routingripv2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4NetworkMask;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.RIPv2Exception;

public class RIPv2RouteEntry {

    private final int addressFamilyIdentifier;
    private final int routeTag;
    private final IPv4Prefix targetPrefix;
    private final IPv4Address nextHopAddress;
    private int metric;
    private Date lastUpdateDate;
    private final IPv4Address senderAddress;
    private RIPv2RouteEntryState lastState;
    private final boolean fullTableRequest;

    protected final static RIPv2RouteEntry fromBytes(IPv4Address senderAddress, byte[] data, int offset) throws RIPv2Exception {
        try {
            int addressFamily = DataTypeHelpers.getUnsignedShortFromBytes(data[offset + 0], data[offset + 1]);
            int routeTag = DataTypeHelpers.getUnsignedShortFromBytes(data[offset + 2], data[offset + 3]);
            int metric = DataTypeHelpers.getUnsignedShortFromBytes(data[offset + 18], data[offset + 19]);
            IPv4Address prefixAddress = new IPv4Address(new byte[]{data[offset + 4], data[offset + 5], data[offset + 6], data[offset + 7]});
            IPv4NetworkMask prefixNetworkMask = IPv4NetworkMask.fromBytes(new byte[]{data[offset + 8], data[offset + 9], data[offset + 10], data[offset + 11]});
            IPv4Address nextHopAddress = new IPv4Address(new byte[]{data[offset + 12], data[offset + 13], data[offset + 14], data[offset + 15]});
            IPv4Prefix prefix = new IPv4Prefix(prefixAddress, prefixNetworkMask);
            if (addressFamily != 2 && (addressFamily != 0 && metric != 16)) {
                throw new RIPv2Exception("Unsupported RIPv2 AFI " + addressFamily);
            }
            return new RIPv2RouteEntry(addressFamily, senderAddress, routeTag, prefix, nextHopAddress, metric);
        } catch (IPv4MathException ex) {
            throw new RIPv2Exception("Failed to construct RIPv2 FIB entry", ex);
        }
    }

    public RIPv2RouteEntry(int addressFamilyIdentifier, IPv4Address senderAddress, int routeTag, IPv4Prefix targetPrefix, IPv4Address nextHopAddress, int metric) throws IPv4MathException {
        this.addressFamilyIdentifier = addressFamilyIdentifier;
        this.routeTag = routeTag;
        this.targetPrefix = targetPrefix;
        this.metric = metric;
        this.lastUpdateDate = new Date();
        this.senderAddress = senderAddress;
        this.nextHopAddress = nextHopAddress;
        this.fullTableRequest = this.metric == 16 && this.addressFamilyIdentifier == 0;
    }

    public int getRouteTag() {
        return routeTag;
    }

    public IPv4Prefix getTargetPrefix() {
        return targetPrefix;
    }

    public IPv4Address getNextHopAddress() {
        return nextHopAddress;
    }

    public int getMetric() {
        return metric;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RIPv2RouteEntry other = (RIPv2RouteEntry) obj;
        if (this.routeTag != other.routeTag) {
            return false;
        }
        if (!Objects.equals(this.targetPrefix, other.targetPrefix)) {
            return false;
        }
        if (!Objects.equals(this.nextHopAddress, other.nextHopAddress)) {
            return false;
        }
        return Objects.equals(this.senderAddress, other.senderAddress);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + this.routeTag;
        hash = 61 * hash + Objects.hashCode(this.targetPrefix);
        hash = 61 * hash + Objects.hashCode(this.nextHopAddress);
        hash = 61 * hash + Objects.hashCode(this.senderAddress);
        return hash;
    }

    public IPv4Address getSenderAddress() {
        return senderAddress;
    }

    public boolean onUpdateReceived(RIPv2RouteEntry updateEntry) {
        boolean changes = false;
        this.lastUpdateDate = updateEntry.getLastUpdateDate();
        if (this.metric != updateEntry.getMetric()) {
            changes = true;
        }
        this.metric = updateEntry.getMetric();
        if (this.metric >= 16) {
            changes = true;
            this.setLastState(RIPv2RouteEntryState.HOLD_DOWN);
        }
        return changes;
    }

    protected RIPv2RouteEntryState getLastState() {
        return lastState;
    }

    protected void setLastState(RIPv2RouteEntryState lastState) {
        this.lastState = lastState;
    }

    public byte[] getBytes() throws IOException, IPv4MathException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(DataTypeHelpers.getUnsignedShort(2));
        baos.write(DataTypeHelpers.getUnsignedShort(this.routeTag));
        baos.write(this.targetPrefix.getAddress().getBytes());
        baos.write(this.targetPrefix.getNetworkMask().getBytes());
        baos.write(IPv4Address.fromString("0.0.0.0").getBytes());
        baos.write((byte) 0);
        baos.write((byte) 0);
        baos.write(DataTypeHelpers.getUnsignedShort(this.metric));
        return baos.toByteArray();
    }

    public boolean isFullTableRequest() {
        return fullTableRequest;
    }
}
