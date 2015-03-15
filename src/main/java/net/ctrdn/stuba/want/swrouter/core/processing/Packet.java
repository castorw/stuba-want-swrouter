package net.ctrdn.stuba.want.swrouter.core.processing;

import java.nio.ByteBuffer;
import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PeeringException;

public class Packet {

    private final NetworkInterface ingressNetworkInterface;
    private NetworkInterface egressNetworkInterface;
    private final PacketIdentifier packetIdentifier;
    private ProcessingChain processingChain;
    private final JBuffer packetBuffer;

    private IPv4Address forwarderIPv4Address = null;
    private MACAddress forwarderHardwareAddress = null;

    /**
     * Creates a packet object representing received packet. No processing chain
     * is assigned by default.
     *
     * @param ingressNetworkInterface
     * @param pcapPacket
     * @throws PacketException
     */
    public Packet(NetworkInterface ingressNetworkInterface, JBuffer packetBuffer) throws PacketException {
        this.ingressNetworkInterface = ingressNetworkInterface;
        this.packetBuffer = packetBuffer;
        this.packetIdentifier = new PacketIdentifier(true);
    }

    /**
     * Creates a packet object representing an outgoing packet. Default
     * processing chain is set to ProcessingChain.OUTPUT. Ethernet frame is
     * pre-configured with source hardware address of the egress network
     * interface.
     *
     * @param maxLength
     * @param egressInterface
     * @throws net.ctrdn.stuba.want.swrouter.exception.PacketException
     */
    public Packet(int maxLength, NetworkInterface egressInterface) throws PacketException {
        this.packetIdentifier = new PacketIdentifier(false);
        this.packetBuffer = new JBuffer(maxLength);
        this.packetBuffer.setSize(maxLength);
        this.ingressNetworkInterface = null;
        this.processingChain = ProcessingChain.OUTPUT;
        this.setEgressNetworkInterface(egressInterface);
        this.setSourceHardwareAddress(this.egressNetworkInterface.getHardwareAddress());
    }

    final public void adjustSize(int actualSize) {
        this.packetBuffer.setSize(actualSize);
    }

    final public PacketIdentifier getPacketIdentifier() {
        return this.packetIdentifier;
    }

    final public ProcessingChain getProcessingChain() {
        return this.processingChain;
    }

    final public void setProcessingChain(ProcessingChain chain) {
        this.processingChain = chain;
    }

    final public NetworkInterface getIngressNetworkInterface() {
        return this.ingressNetworkInterface;
    }

    final public NetworkInterface getEgressNetworkInterface() {
        return this.egressNetworkInterface;
    }

    final public void setEgressNetworkInterface(NetworkInterface networkInterface) {
        this.egressNetworkInterface = networkInterface;
    }

    final public MACAddress getSourceHardwareAddress() {
        return new MACAddress(this.packetBuffer.getByteArray(6, 6));
    }

    final public void setSourceHardwareAddress(MACAddress hardwareAddress) {
        this.packetBuffer.setByteArray(6, hardwareAddress.getAddressBytes());
    }

    final public MACAddress getDestinationHardwareAddress() {
        return new MACAddress(this.packetBuffer.getByteArray(0, 6));
    }

    final public void setDestinationHardwareAddress(MACAddress hardwareAddress) {
        this.packetBuffer.setByteArray(0, hardwareAddress.getAddressBytes());
    }

    final public EthernetType getEthernetType() {
        return EthernetType.valueOf(this.packetBuffer.getByteArray(12, 2));
    }

    final public void setEthernetType(EthernetType ethernetType) {
        this.packetBuffer.setByteArray(12, ethernetType.getCode());
    }

    public int getIPv4HeaderLength() throws PacketException {
        this.checkIPv4Packet();
        return (this.packetBuffer.getByte(14) & 0x0f) * 4;
    }

    public int getIPv4TotalLength() throws PacketException {
        this.checkIPv4Packet();
        return DataTypeHelpers.getUnsignedShortFromBytes(this.packetBuffer.getByte(16), this.packetBuffer.getByte(17));
    }

    public void setIPv4TotalLength(int totalLength) throws PacketException {
        this.checkIPv4Packet();
        this.packetBuffer.setByteArray(16, DataTypeHelpers.getUnsignedShort(totalLength));
    }

    public short getIPv4TimeToLive() throws PacketException {
        this.checkIPv4Packet();
        return DataTypeHelpers.getUnsignedByte(this.packetBuffer.getByte(22));
    }

    public void setIPv4TimeToLive(short ttl) throws PacketException {
        this.checkIPv4Packet();
        this.packetBuffer.setByte(22, DataTypeHelpers.getUnsignedByte(ttl));
    }

    public IPv4Protocol getIPv4Protocol() throws PacketException {
        this.checkIPv4Packet();
        return IPv4Protocol.valueOf(this.packetBuffer.getByte(23));
    }

    public void setIPv4Protocol(IPv4Protocol protocol) throws PacketException {
        this.checkIPv4Packet();
        this.packetBuffer.setByte(23, protocol.getCode());
    }

    public byte[] getIPv4HeaderChecksum() throws PacketException {
        this.checkIPv4Packet();
        return this.packetBuffer.getByteArray(24, 2);
    }

    public IPv4Address getSourceIPv4Address() throws PacketException {
        this.checkIPv4Packet();
        return new IPv4Address(this.packetBuffer.getByteArray(26, 4));
    }

    public void setSourceIPv4Address(IPv4Address ipv4Address) throws PacketException {
        this.checkIPv4Packet();
        this.packetBuffer.setByteArray(26, ipv4Address.getBytes());
    }

    public IPv4Address getDestinationIPv4Address() throws PacketException {
        this.checkIPv4Packet();
        return new IPv4Address(this.packetBuffer.getByteArray(30, 4));
    }

    public void setDestinationIPv4Address(IPv4Address ipv4Address) throws PacketException {
        this.checkIPv4Packet();
        this.packetBuffer.setByteArray(30, ipv4Address.getBytes());
    }

    public void defaultIPv4Setup() throws PacketException {
        this.checkIPv4Packet();
        this.packetBuffer.setByte(14, (byte) 0x45);   // Version + IHL
        this.packetBuffer.setByte(15, (byte) 0x00);   // DSCP + ECN
        this.packetBuffer.setByteArray(18, new byte[]{(byte) 0x00, (byte) 0x00}); // Identification
        this.packetBuffer.setByte(20, (byte) 0x40);   // DF Flag + Fragment Offset
        this.packetBuffer.setByte(21, (byte) 0x00);   // Rest of Fragment Offset
        this.packetBuffer.setByteArray(24, new byte[]{(byte) 0x00, (byte) 0x00}); // Zero checksum by default
    }

    public void calculateIPv4Checksum() throws PacketException {
        this.packetBuffer.setByteArray(24, new byte[]{0x00, 0x00});
        byte[] ipData = this.packetBuffer.getByteArray(14, this.getIPv4HeaderLength());
        long crc = DataTypeHelpers.RFC1071Checksum(ipData, ipData.length);
        this.packetBuffer.setByteArray(24, DataTypeHelpers.getUnsignedShort((int) crc));
    }

    public MACAddress getForwarderHardwareAddress() {
        return this.forwarderHardwareAddress;
    }

    public void setForwarderHardwareAddress(MACAddress hardwareAddress) {
        this.forwarderHardwareAddress = hardwareAddress;
    }

    public IPv4Address getForwarderIPv4Address() throws PacketException {
        this.checkIPv4Packet();
        return this.forwarderIPv4Address;
    }

    public void setForwarderIPv4Address(IPv4Address ipv4Address) throws PacketException {
        this.checkIPv4Packet();
        this.forwarderIPv4Address = ipv4Address;
    }

    public JBuffer getPacketBuffer() {
        return this.packetBuffer;
    }

    private void checkIPv4Packet() throws PacketException {
        if (this.packetBuffer == null) {
            throw new PacketException("Packet is not initialized");
        } else if (this.getEthernetType() != EthernetType.IPV4) {
            throw new PacketException("Packet is not an IPv4 packet");
        }
    }
}
