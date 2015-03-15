package net.ctrdn.stuba.want.swrouter.core.processing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public int getHeaderLength() {
        return 8;
    }

    public int getSourcePort() throws PacketException {
        byte msb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 0);
        byte lsb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 1);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setSourcePort(int port) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 0, DataTypeHelpers.getUnsignedShort(port));
    }

    public int getDestinationPort() throws PacketException {
        byte msb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 2);
        byte lsb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 3);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setDestinationPort(int port) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 2, DataTypeHelpers.getUnsignedShort(port));
    }

    public int getLength() throws PacketException {
        byte msb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 4);
        byte lsb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 5);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setLength(int length) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 4, DataTypeHelpers.getUnsignedShort(length));
    }

    public int getChecksum() throws PacketException {
        byte msb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 6);
        byte lsb = this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 7);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public byte[] getData() throws PacketException {
        return this.getPacket().getPacketBuffer().getByteArray(14 + this.getPacket().getIPv4HeaderLength() + 8, this.getPacket().getIPv4TotalLength() - (this.getPacket().getIPv4HeaderLength() + 8));
    }

    public void setData(byte[] data) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 8, data);
    }

    public void calculateUDPChecksum() throws PacketException, IOException {
        ByteArrayOutputStream pseudoPacketBaos = new ByteArrayOutputStream(20 + this.getLength() - this.getHeaderLength());
        pseudoPacketBaos.write(this.getPacket().getSourceIPv4Address().getBytes());
        pseudoPacketBaos.write(this.getPacket().getDestinationIPv4Address().getBytes());
        pseudoPacketBaos.write((byte) 0);
        pseudoPacketBaos.write(this.getPacket().getIPv4Protocol().getCode());
        pseudoPacketBaos.write(DataTypeHelpers.getUnsignedShort(this.getLength()));
        pseudoPacketBaos.write(DataTypeHelpers.getUnsignedShort(this.getSourcePort()));
        pseudoPacketBaos.write(DataTypeHelpers.getUnsignedShort(this.getDestinationPort()));
        pseudoPacketBaos.write(DataTypeHelpers.getUnsignedShort(this.getLength()));
        pseudoPacketBaos.write(DataTypeHelpers.getUnsignedShort(0));
        pseudoPacketBaos.write(this.getData());
        long crc = DataTypeHelpers.RFC1071Checksum(pseudoPacketBaos.toByteArray(), pseudoPacketBaos.size());
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 6, DataTypeHelpers.getUnsignedShort((int) crc));
    }

    public Packet getPacket() {
        return packet;
    }
}
