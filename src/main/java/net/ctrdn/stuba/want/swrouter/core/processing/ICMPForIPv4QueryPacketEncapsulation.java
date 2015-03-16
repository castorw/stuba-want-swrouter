package net.ctrdn.stuba.want.swrouter.core.processing;

import java.util.Arrays;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;

public class ICMPForIPv4QueryPacketEncapsulation {

    public Packet getPacket() {
        return packet;
    }

    public enum IcmpTypeCode {

        ECHO_REQUEST("0800"),
        ECHO_REPLY("0000"),
        UNKNOWN("FFFF");

        private final byte[] code;

        private IcmpTypeCode(String code) {
            this.code = DataTypeHelpers.hexStringToByteArray(code);
        }

        public byte[] getCode() {
            return code;
        }

        public static IcmpTypeCode valueOf(byte[] bytes) {
            for (IcmpTypeCode t : IcmpTypeCode.values()) {
                if (Arrays.equals(t.getCode(), bytes)) {
                    return t;
                }
            }
            return IcmpTypeCode.UNKNOWN;
        }
    }

    private final Packet packet;

    public ICMPForIPv4QueryPacketEncapsulation(Packet packet) throws PacketException {
        this.packet = packet;
        if (this.packet.getEthernetType() != EthernetType.IPV4 || this.packet.getIPv4Protocol() != IPv4Protocol.ICMP) {
            throw new PacketException("Cannot encapsulate - not an ICMP packet");
        }
    }

    public IcmpTypeCode getIcmpTypeCode() throws PacketException {
        return IcmpTypeCode.valueOf(this.getPacket().getPacketBuffer().getByteArray(14 + this.getPacket().getIPv4HeaderLength(), 2));
    }

    public void setIcmpTypeCode(IcmpTypeCode type) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength(), type.getCode());
    }

    public byte[] getChecksum() throws PacketException {
        return this.getPacket().getPacketBuffer().getByteArray(14 + this.getPacket().getIPv4HeaderLength() + 2, 2);
    }

    public int getIdentifier() throws PacketException {
        return DataTypeHelpers.getUnsignedShortFromBytes(this.getPacket().getPacketBuffer().getByte(14 + +this.packet.getIPv4HeaderLength() + 4), this.getPacket().getPacketBuffer().getByte(14 + this.packet.getIPv4HeaderLength() + 5));
    }

    public void setIdentifier(int identifier) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 4, DataTypeHelpers.getUnsignedShort(identifier));
    }

    public int getSequenceNumber() throws PacketException {
        return DataTypeHelpers.getUnsignedShortFromBytes(this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 6), this.getPacket().getPacketBuffer().getByte(14 + this.getPacket().getIPv4HeaderLength() + 7));
    }

    public void setSequenceNumber(int identifier) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 6, DataTypeHelpers.getUnsignedShort(identifier));
    }

    public byte[] getData() throws PacketException {
        return this.getPacket().getPacketBuffer().getByteArray(14 + this.getPacket().getIPv4HeaderLength() + 8, this.getPacket().getPacketBuffer().size() - (14 + this.getPacket().getIPv4HeaderLength() + 8));
    }

    public void setData(byte[] data) throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 8, data);
    }

    public void calculateICMPChecksum() throws PacketException {
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 2, new byte[]{(byte) 0x00, (byte) 0x00});
        byte[] icmpData = this.getPacket().getPacketBuffer().getByteArray(14 + this.getPacket().getIPv4HeaderLength(), 8 + this.getData().length);
        long crc = DataTypeHelpers.RFC1071Checksum(icmpData, icmpData.length);
        this.getPacket().getPacketBuffer().setByteArray(14 + this.getPacket().getIPv4HeaderLength() + 2, DataTypeHelpers.getUnsignedShort((int) crc));
    }

    public boolean isQueryBasedMessage() throws PacketException {
        return (this.getIcmpTypeCode() == IcmpTypeCode.ECHO_REPLY || this.getIcmpTypeCode() == IcmpTypeCode.ECHO_REQUEST);
    }

}
