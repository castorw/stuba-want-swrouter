package net.ctrdn.stuba.want.swrouter.core.processing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;

public class TCPForIPv4PacketEncapsulation {

    private final Packet packet;

    public TCPForIPv4PacketEncapsulation(Packet packet) throws PacketException {
        this.packet = packet;
        if (this.packet.getEthernetType() != EthernetType.IPV4 || this.packet.getIPv4Protocol() != IPv4Protocol.TCP) {
            throw new PacketException("Cannot encapsulate - not an IPV4 TCP packet");
        }
    }

    public int getSourcePort() throws PacketException {
        byte msb = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 0);
        byte lsb = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 1);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setSourcePort(int port) throws PacketException {
        this.getPacket().getPcapPacket().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 0, DataTypeHelpers.getUnsignedShort(port));
    }

    public int getDestinationPort() throws PacketException {
        byte msb = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 2);
        byte lsb = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 3);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public void setDestinationPort(int port) throws PacketException {
        this.getPacket().getPcapPacket().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 2, DataTypeHelpers.getUnsignedShort(port));
    }

    public int getChecksum() throws PacketException {
        byte msb = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 16);
        byte lsb = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 17);
        return DataTypeHelpers.getUnsignedShortFromBytes(msb, lsb);
    }

    public int getDataOffset() throws PacketException {
        byte dornsByte = this.getPacket().getPcapPacket().getByte(14 + this.getPacket().getIPv4HeaderLength() + 12);
        dornsByte = (byte) ((dornsByte >> 4) & 0x0f);
        return DataTypeHelpers.getUnsignedByteValue(dornsByte) * 4;
    }

    public byte[] getData() throws PacketException {
        int dataLength = this.getPacket().getPcapPacket().size() - (14 + this.getPacket().getIPv4HeaderLength() + this.getDataOffset());
        return this.getPacket().getPcapPacket().getByteArray(14 + this.getPacket().getIPv4HeaderLength() + this.getDataOffset(), dataLength);
    }

    public void setData(byte[] data) throws PacketException {
        this.getPacket().getPcapPacket().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + this.getDataOffset(), data);
    }

    public void calculateTCPChecksum() throws PacketException, IOException {
        this.getPacket().getPcapPacket().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 16, DataTypeHelpers.getUnsignedShort(0));
        int headerAndDataLength = this.getPacket().getPcapPacket().size() - (14 + this.getPacket().getIPv4HeaderLength());
        ByteArrayOutputStream pseudoPacketBaos = new ByteArrayOutputStream(32 + (this.getDataOffset() - 20) + this.getData().length);
        pseudoPacketBaos.write(this.getPacket().getSourceIPv4Address().getBytes());
        pseudoPacketBaos.write(this.getPacket().getDestinationIPv4Address().getBytes());
        pseudoPacketBaos.write((byte) 0);
        pseudoPacketBaos.write(this.getPacket().getIPv4Protocol().getCode());
        pseudoPacketBaos.write(DataTypeHelpers.getUnsignedShort(headerAndDataLength));
        pseudoPacketBaos.write(this.getPacket().getPcapPacket().getByteArray(14 + this.getPacket().getIPv4HeaderLength(), headerAndDataLength));
        long crc = DataTypeHelpers.RFC1071Checksum(pseudoPacketBaos.toByteArray(), pseudoPacketBaos.size());
        this.getPacket().getPcapPacket().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 16, DataTypeHelpers.getUnsignedShort((int) crc));
    }

    public Packet getPacket() {
        return packet;
    }
}
