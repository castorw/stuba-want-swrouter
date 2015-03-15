package net.ctrdn.stuba.want.swrouter.module.arpmanager;

import java.util.Arrays;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;

public final class ARPForIPv4PacketEncapsulation {

    public enum Operation {

        REQUEST("0001"),
        REPLY("0002"),
        UNKNOWN("FFFF");

        private final byte[] code;

        private Operation(String code) {
            this.code = DataTypeHelpers.hexStringToByteArray(code);
        }

        public byte[] getCode() {
            return code;
        }

        public static Operation valueOf(byte[] bytes) {
            for (Operation t : Operation.values()) {
                if (Arrays.equals(t.getCode(), bytes)) {
                    return t;
                }
            }
            return Operation.UNKNOWN;
        }
    }

    private final Packet packet;

    public ARPForIPv4PacketEncapsulation(Packet packet) throws PacketException {
        this.packet = packet;
        if (this.packet.getEthernetType() != EthernetType.ARP) {
            throw new PacketException("Cannot encapsulate - not an ARP packet");
        }
    }

    public Packet getPacket() {
        return this.packet;
    }

    public int getHardwareType() {
        return DataTypeHelpers.getUnsignedShortFromBytes(this.packet.getPacketBuffer().getByte(14), this.packet.getPacketBuffer().getByte(15));
    }

    public void setHardwareType(int hardwareType) {
        this.packet.getPacketBuffer().setByteArray(14, DataTypeHelpers.getUnsignedShort(hardwareType));
    }

    public EthernetType getProtocolType() {
        return EthernetType.valueOf(this.packet.getPacketBuffer().getByteArray(16, 2));
    }

    public void setProtocolType(EthernetType protocolType) {
        this.packet.getPacketBuffer().setByteArray(16, protocolType.getCode());
    }

    public short getHardwareAddressLength() {
        return DataTypeHelpers.getUnsignedByteValue(this.packet.getPacketBuffer().getByte(18));
    }

    public void setHardwareAddressLength(short length) {
        this.packet.getPacketBuffer().setByte(18, DataTypeHelpers.getUnsignedByte(length));
    }

    public short getProtocolAddressLength() {
        return DataTypeHelpers.getUnsignedByteValue(this.packet.getPacketBuffer().getByte(19));
    }

    public void setProtocolAddressLength(short length) {
        this.packet.getPacketBuffer().setByte(19, DataTypeHelpers.getUnsignedByte(length));
    }

    public Operation getOperation() {
        return Operation.valueOf(this.packet.getPacketBuffer().getByteArray(20, 2));
    }

    public void setOperation(Operation operation) {
        this.packet.getPacketBuffer().setByteArray(20, operation.getCode());
    }

    public MACAddress getSenderHardwareAddress() {
        return new MACAddress(this.packet.getPacketBuffer().getByteArray(22, 6));
    }

    public void setSenderHardwareAddress(MACAddress hardwareAddress) {
        this.packet.getPacketBuffer().setByteArray(22, hardwareAddress.getAddressBytes());
    }

    public IPv4Address getSenderProtocolAddress() {
        return new IPv4Address(this.packet.getPacketBuffer().getByteArray(28, 4));
    }

    public void setSenderProtocolAddress(IPv4Address address) {
        this.packet.getPacketBuffer().setByteArray(28, address.getBytes());
    }

    public MACAddress getTargetHardwareAddress() {
        return new MACAddress(this.packet.getPacketBuffer().getByteArray(32, 6));
    }

    public void setTargetHardwareAddress(MACAddress hardwareAddress) {
        this.packet.getPacketBuffer().setByteArray(32, hardwareAddress.getAddressBytes());
    }

    public IPv4Address getTargetProtocolAddress() {
        return new IPv4Address(this.packet.getPacketBuffer().getByteArray(38, 4));
    }

    public void setTargetProtocolAddress(IPv4Address address) {
        this.packet.getPacketBuffer().setByteArray(38, address.getBytes());
    }
}
