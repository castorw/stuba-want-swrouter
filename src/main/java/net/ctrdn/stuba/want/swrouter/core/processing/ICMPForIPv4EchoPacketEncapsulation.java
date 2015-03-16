package net.ctrdn.stuba.want.swrouter.core.processing;

import java.util.Arrays;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import org.jnetpcap.protocol.network.Icmp;

public class ICMPForIPv4EchoPacketEncapsulation {

    public enum EchoType {

        REQUEST("0800"),
        REPLY("0000"),
        UNKNOWN("FFFF");

        private final byte[] code;

        private EchoType(String code) {
            this.code = DataTypeHelpers.hexStringToByteArray(code);
        }

        public byte[] getCode() {
            return code;
        }

        public static EchoType valueOf(byte[] bytes) {
            for (EchoType t : EchoType.values()) {
                if (Arrays.equals(t.getCode(), bytes)) {
                    return t;
                }
            }
            return EchoType.UNKNOWN;
        }
    }

    private final Packet packet;

    public ICMPForIPv4EchoPacketEncapsulation(Packet packet) throws PacketException {
        this.packet = packet;
        if (this.packet.getEthernetType() != EthernetType.IPV4 || this.packet.getIPv4Protocol() != IPv4Protocol.ICMP) {
            throw new PacketException("Cannot encapsulate - not an ICMP packet");
        }
    }

    public EchoType getEchoType() throws PacketException {
        return EchoType.valueOf(this.packet.getPacketBuffer().getByteArray(14 + this.packet.getIPv4HeaderLength(), 2));
    }

    public void setEchoType(EchoType type) throws PacketException {
        this.packet.getPacketBuffer().setByteArray(14 + this.packet.getIPv4HeaderLength(), type.getCode());
    }

    public byte[] getChecksum() throws PacketException {
        return this.packet.getPacketBuffer().getByteArray(14 + this.packet.getIPv4HeaderLength() + 2, 2);
    }

    public int getIdentifier() throws PacketException {
        return DataTypeHelpers.getUnsignedShortFromBytes(this.packet.getPacketBuffer().getByte(14 + +this.packet.getIPv4HeaderLength() + 4), this.packet.getPacketBuffer().getByte(14 + +this.packet.getIPv4HeaderLength() + 5));
    }

    public void setIdentifier(int identifier) throws PacketException {
        this.packet.getPacketBuffer().setByteArray(14 + this.packet.getIPv4HeaderLength() + 4, DataTypeHelpers.getUnsignedShort(identifier));
    }

    public int getSequenceNumber() throws PacketException {
        return DataTypeHelpers.getUnsignedShortFromBytes(this.packet.getPacketBuffer().getByte(14 + this.packet.getIPv4HeaderLength() + 6), this.packet.getPacketBuffer().getByte(14 + this.packet.getIPv4HeaderLength() + 7));
    }

    public void setSequenceNumber(int identifier) throws PacketException {
        this.packet.getPacketBuffer().setByteArray(14 + this.packet.getIPv4HeaderLength() + 6, DataTypeHelpers.getUnsignedShort(identifier));
    }

    public byte[] getData() throws PacketException {
        return this.packet.getPacketBuffer().getByteArray(14 + this.packet.getIPv4HeaderLength() + 8, this.packet.getPacketBuffer().size() - (14 + this.packet.getIPv4HeaderLength() + 8));
    }

    public void setData(byte[] data) throws PacketException {
        this.packet.getPacketBuffer().setByteArray(14 + this.packet.getIPv4HeaderLength() + 8, data);
    }

    public void calculateICMPChecksum() throws PacketException {
        this.packet.getPacketBuffer().setByteArray(14 + this.packet.getIPv4HeaderLength() + 2, new byte[]{(byte) 0x00, (byte) 0x00});
        byte[] icmpData = this.packet.getPacketBuffer().getByteArray(14 + this.packet.getIPv4HeaderLength(), 8 + this.getData().length);
        long crc = DataTypeHelpers.RFC1071Checksum(icmpData, icmpData.length);
        this.packet.getPacketBuffer().setByteArray(14 + this.packet.getIPv4HeaderLength() + 2, DataTypeHelpers.getUnsignedShort((int) crc));
    }

}
