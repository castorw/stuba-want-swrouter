package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4InterfaceAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;

public interface NetworkInterface {

    public String getName();

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public int getMTU();

    public MACAddress getHardwareAddress();

    public IPv4InterfaceAddress getIPv4InterfaceAddress();

    public void setIPv4InterfaceAddress(IPv4InterfaceAddress interfaceAddress);

    public void sendPacket(Packet packet);

    public long getTransmittedPacketCount();

    public long getTransmittedByteCount();

    public long getReceivedPacketCount();

    public long getReceivedByteCount();

    public void resetStats();
}
