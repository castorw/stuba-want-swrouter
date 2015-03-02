package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.common.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;

public interface NetworkInterface {

    public String getName();

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public int getMTU();

    public MACAddress getHardwareAddress();

    public IPv4Address getIPv4Address();

    public IPv4Address getIPv4NetworkMask();

    public void setIPv4Address(IPv4Address address, IPv4Address netmask);

    public void sendPacket(Packet packet);
}
