package net.ctrdn.stuba.want.swrouter.module.routingripv2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.InvalidRIPVersionException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.exception.RIPv2Exception;

final public class RIPv2PacketEncapsulation {

    private final UDPForIPv4PacketEncapsulation udpEncapsulation;

    public RIPv2PacketEncapsulation(UDPForIPv4PacketEncapsulation encap, boolean newPacket) throws PacketException, InvalidRIPVersionException {
        this.udpEncapsulation = encap;
        if (!newPacket) {
            if (encap.getDestinationPort() != 520) {
                throw new InvalidRIPVersionException("Cannot encapsulate packet - not an UDP/520 targetted packet " + this.getVersion());
            } else if (this.getVersion() != 2) {
                throw new InvalidRIPVersionException("Cannot encapsulate packet - unsupported RIP version " + this.getVersion());
            }
        } else {
            if (encap.getDestinationPort() != 520) {
                throw new InvalidRIPVersionException("Cannot encapsulate packet - not an UDP/520 targetted packet " + this.getVersion());
            }
            this.setVersion((short) 2);
            this.udpEncapsulation.getPacket().getPacketBuffer().setByteArray(this.getOffset(2), new byte[]{0x00, 0x00});
        }
    }

    public short getCommand() throws PacketException {
        return this.getUdpEncapsulation().getPacket().getPacketBuffer().getByte(this.getOffset(0));
    }

    public void setCommand(short command) throws PacketException {
        this.getUdpEncapsulation().getPacket().getPacketBuffer().setByte(this.getOffset(0), DataTypeHelpers.getUnsignedByte(command));
    }

    public short getVersion() throws PacketException {
        return this.getUdpEncapsulation().getPacket().getPacketBuffer().getByte(this.getOffset(1));
    }

    public void setVersion(short version) throws PacketException {
        this.getUdpEncapsulation().getPacket().getPacketBuffer().setByte(this.getOffset(1), DataTypeHelpers.getUnsignedByte(version));
    }

    public RIPv2RouteEntry[] getRouteEntries() throws PacketException, RIPv2Exception {
        byte[] udpData = this.getUdpEncapsulation().getData();
        int entryCount = (this.getUdpEncapsulation().getLength() - 4) / 20;
        RIPv2RouteEntry[] entries = new RIPv2RouteEntry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            entries[i] = RIPv2RouteEntry.fromBytes(this.getUdpEncapsulation().getPacket().getSourceIPv4Address(), udpData, 4 + (i * 20));
        }
        return entries;
    }

    public void setRouteEntries(RIPv2RouteEntry[] entries) throws RIPv2Exception, PacketException {
        try {
            ByteArrayOutputStream routeEntriesBaos = new ByteArrayOutputStream(entries.length * 20);
            for (RIPv2RouteEntry entry : entries) {
                routeEntriesBaos.write(entry.getBytes());
            }
            this.getUdpEncapsulation().getPacket().getPacketBuffer().setByteArray(this.getOffset(4), routeEntriesBaos.toByteArray());
        } catch (IOException | IPv4MathException ex) {
            throw new RIPv2Exception("Failed to add route entries to RIPv2 packet", ex);
        }
    }

    public void setRequestWholeTable() throws PacketException {
        this.getUdpEncapsulation().getPacket().getPacketBuffer().setByteArray(this.getOffset(4), DataTypeHelpers.hexStringToByteArray("0000000000000000000000000000000000000010"));
    }

    private int getOffset(int offset) throws PacketException {
        return 14 + this.getUdpEncapsulation().getPacket().getIPv4HeaderLength() + this.getUdpEncapsulation().getHeaderLength() + offset;
    }

    public UDPForIPv4PacketEncapsulation getUdpEncapsulation() {
        return udpEncapsulation;
    }
}
