package net.ctrdn.stuba.want.swrouter.module.arpmanager;

import java.util.Date;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public class ARPTableEntry {

    private final IPv4Address protocolAddress;
    private MACAddress hardwareAddress = null;
    private final NetworkInterface networkInterface;
    private Date lastUpdateDate = null;
    private final Object updateLock = new Object();

    public ARPTableEntry(IPv4Address protocolAddress, NetworkInterface networkInterface) {
        this.protocolAddress = protocolAddress;
        this.networkInterface = networkInterface;
        this.lastUpdateDate = new Date();
    }

    public IPv4Address getProtocolAddress() {
        return protocolAddress;
    }

    public MACAddress getHardwareAddress() {
        return hardwareAddress;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public boolean isComplete() {
        return this.hardwareAddress != null && this.networkInterface != null;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void update(MACAddress hardwareAddress) {
        this.lastUpdateDate = new Date();
        this.hardwareAddress = hardwareAddress;
        synchronized (this.updateLock) {
            this.updateLock.notifyAll();
        }
    }

    public Object getUpdateLock() {
        return this.updateLock;
    }
}
