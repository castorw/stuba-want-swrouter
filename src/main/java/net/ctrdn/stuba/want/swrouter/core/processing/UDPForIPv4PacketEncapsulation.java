package net.ctrdn.stuba.want.swrouter.core.processing;

import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;

public class UDPForIPv4PacketEncapsulation {

    private final Packet packet;

    public UDPForIPv4PacketEncapsulation(Packet packet) throws PacketException {
        this.packet = packet;
        if (this.packet.getEthernetType() != EthernetType.IPV4 || this.packet.getIPv4Protocol() != IPv4Protocol.UDP) {
            throw new PacketException("Cannot encapsulate - not an IPV4 UDP packet");
        }
    }

    public int getSourcePort() throws PacketException {
        byte msb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 0);
        byte lsb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 1);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setSourcePort(int port) throws PacketException {
        this.packet.getPcapPacket().setByteArray(14 + this.packet.getIPv4HeaderLength() + 0, DataTypeHelpers.getUnsignedShort(port));
    }

    public int getDestinationPort() throws PacketException {
        byte msb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 2);
        byte lsb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 3);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setDestinationPort(int port) throws PacketException {
        this.packet.getPcapPacket().setByteArray(14 + this.packet.getIPv4HeaderLength() + 2, DataTypeHelpers.getUnsignedShort(port));
    }

    public int getLength() throws PacketException {
        byte msb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 4);
        byte lsb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 5);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setLength(int length) throws PacketException {
        this.packet.getPcapPacket().setByteArray(14 + this.packet.getIPv4HeaderLength() + 4, DataTypeHelpers.getUnsignedShort(length));
    }

    public int getChecksum() throws PacketException {
        byte msb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 6);
        byte lsb = this.packet.getPcapPacket().getByte(14 + this.packet.getIPv4HeaderLength() + 7);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public byte[] getData() throws PacketException {
        return this.packet.getPcapPacket().getByteArray(14 + this.packet.getIPv4HeaderLength() + 8, this.packet.getPcapPacket().size() - (14 + this.packet.getIPv4HeaderLength() + 8));
    }

    public void setData(byte[] data) throws PacketException {
        this.packet.getPcapPacket().setByteArray(14 + this.packet.getIPv4HeaderLength() + 8, data);
    }

    public void calculateICMPChecksum() throws PacketException {
        this.packet.getPcapPacket().setByteArray(14 + this.packet.getIPv4HeaderLength() + 6, new byte[]{(byte) 0x00, (byte) 0x00});
        byte[] udpHeaderAndData = this.packet.getPcapPacket().getByteArray(14 + this.packet.getIPv4HeaderLength(), 8 + this.getData().length);
        long crc = DataTypeHelpers.RFC1071Checksum(udpHeaderAndData, udpHeaderAndData.length);
        this.packet.getPcapPacket().setByteArray(14 + this.packet.getIPv4HeaderLength() + 6, DataTypeHelpers.getUnsignedShort((int) crc));
    }
}
