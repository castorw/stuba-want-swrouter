package net.ctrdn.stuba.want.swrouter.module.nat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.core.processing.ICMPForIPv4QueryPacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.core.processing.TCPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NATTranslationException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public class NATTranslation {

    private final NATModule natModule;
    private final IPv4Protocol protocol;
    private final NetworkInterface outsideInterface;
    private final NATAddress outsideAddress;
    private final Integer outsideProtocolSpecificIdentifier;
    private final IPv4Address insideAddress;
    private final Integer insideProtocolSpecificIdentifier;
    private final List<NetworkInterface> ecmpOutsideInterfaceList = new ArrayList<>();
    private boolean active = true;
    private Date lastActivityDate;

    public final static NATTranslation newAddressTranslation(NATModule natModule, NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress) throws NATTranslationException {
        if (outsideAddress.isConfiguredForPortTranslation()) {
            throw new NATTranslationException("NAT address " + outsideAddress.getAddress() + " is already configured for port translation and cannot be used for NAT");
        }
        outsideAddress.setConfiguredForAddressTranslation(true);
        return new NATTranslation(natModule, null, outsideAddress, outsideInterface, null, insideAddress, null);
    }

    public final static NATTranslation newPortTranslation(NATModule natModule, IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress, Integer insideProtocolSpecificIdentifier, Integer outsideProtocolSpecificIdentifier) throws NATException {
        if (outsideAddress.isConfiguredForAddressTranslation()) {
            throw new NATTranslationException("NAT address " + outsideAddress.getAddress() + " is already configured for address translation and cannot be used for PAT");
        }
        outsideAddress.setConfiguredForPortTranslation(true);
        Integer opsi = outsideProtocolSpecificIdentifier;
        if (opsi != null && opsi == -1) {
            opsi = (protocol == IPv4Protocol.TCP) ? outsideAddress.allocateTCPPort() : (protocol == IPv4Protocol.UDP) ? outsideAddress.allocateUDPPort() : (protocol == IPv4Protocol.ICMP) ? outsideAddress.allocateICMPIdentifier() : null;
        }
        return new NATTranslation(natModule, protocol, outsideAddress, outsideInterface, opsi, insideAddress, insideProtocolSpecificIdentifier);
    }

    public final static NATTranslation newPortTranslation(NATModule natModule, IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, IPv4Address insideAddress, Integer insideProtocolSpecificIdentifier) throws NATException {
        return NATTranslation.newPortTranslation(natModule, protocol, outsideAddress, outsideInterface, insideAddress, insideProtocolSpecificIdentifier, -1);
    }

    private NATTranslation(NATModule natModule, IPv4Protocol protocol, NATAddress outsideAddress, NetworkInterface outsideInterface, Integer outsideProtocolSpecificIdentifier, IPv4Address insideAddress, Integer insideProtocolSpecificIdentifier) throws NATTranslationException {
        this.natModule = natModule;
        this.protocol = protocol;
        this.outsideAddress = outsideAddress;
        this.outsideInterface = outsideInterface;
        this.insideAddress = insideAddress;
        this.insideProtocolSpecificIdentifier = insideProtocolSpecificIdentifier;
        this.outsideProtocolSpecificIdentifier = outsideProtocolSpecificIdentifier;
        this.lastActivityDate = new Date();
    }

    public IPv4Protocol getProtocol() {
        return protocol;
    }

    public NetworkInterface getOutsideInterface() {
        return outsideInterface;
    }

    public NATAddress getOutsideAddress() {
        return outsideAddress;
    }

    public Integer getOutsideProtocolSpecificIdentifier() {
        return outsideProtocolSpecificIdentifier;
    }

    public IPv4Address getInsideAddress() {
        return insideAddress;
    }

    public Integer getInsideProtocolSpecificIdentifier() {
        return insideProtocolSpecificIdentifier;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() throws NATTranslationException {
        this.active = false;
    }

    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    @Override
    public String toString() {
        if (this.protocol == null) {
            return "NAT inside " + this.getInsideAddress() + " <---> outside " + this.getOutsideAddress().getAddress() + " on " + this.getOutsideInterface().getName();
        } else {
            return "PAT inside " + this.getInsideAddress() + " " + this.getProtocol().name() + ((this.getInsideProtocolSpecificIdentifier() != null) ? "/" + this.getInsideProtocolSpecificIdentifier() : "") + " <---> outside " + this.getOutsideAddress().getAddress() + " " + this.getProtocol().name() + ((this.getOutsideProtocolSpecificIdentifier() != null) ? "/" + this.getOutsideProtocolSpecificIdentifier() : "") + " on " + this.getOutsideInterface().getName();
        }
    }

    public boolean apply(Packet packet) throws NATTranslationException {
        if (this.isActive()) {
            if ((packet.getProcessingChain() == ProcessingChain.INPUT || packet.getProcessingChain() == ProcessingChain.FORWARD) && packet.getEthernetType() == EthernetType.IPV4) {
                try {
                    if (packet.getProcessingChain() == ProcessingChain.FORWARD && this.getProtocol() == null && (this.getOutsideInterface().equals(packet.getEgressNetworkInterface()) || this.ecmpOutsideInterfaceList.contains(packet.getEgressNetworkInterface())) && this.getInsideAddress().equals(packet.getSourceIPv4Address())) {
                        // NAT XLATE
                        packet.setSourceIPv4Address(this.getOutsideAddress().getAddress());
                        this.calculateProtocolChecksumIfNeeded(packet);
                        packet.calculateIPv4Checksum();
                        this.updateLastActivity();
                        return true;
                    } else if (this.getProtocol() == null && (this.getOutsideInterface().equals(packet.getIngressNetworkInterface()) || this.ecmpOutsideInterfaceList.contains(packet.getIngressNetworkInterface())) && this.getOutsideAddress().getAddress().equals(packet.getDestinationIPv4Address())) {
                        // NAT UNXLATE
                        packet.setDestinationIPv4Address(this.getInsideAddress());
                        packet.setDestinationHardwareAddress(MACAddress.ZERO);
                        this.calculateProtocolChecksumIfNeeded(packet);
                        packet.calculateIPv4Checksum();
                        packet.setProcessingChain(ProcessingChain.FORWARD);
                        this.updateLastActivity();
                        return true;
                    } else if (packet.getProcessingChain() == ProcessingChain.FORWARD && this.getProtocol() == packet.getIPv4Protocol() && (this.getOutsideInterface().equals(packet.getEgressNetworkInterface()) || this.ecmpOutsideInterfaceList.contains(packet.getEgressNetworkInterface())) && this.getInsideAddress().equals(packet.getSourceIPv4Address())) {
                        // Possible PAT XLATE
                        switch (this.getProtocol()) {
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncapsulation.getSourcePort() == this.getInsideProtocolSpecificIdentifier()) {
                                    // PAT TCP XLATE
                                    tcpEncapsulation.setSourcePort(this.getOutsideProtocolSpecificIdentifier());
                                    tcpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    tcpEncapsulation.calculateTCPChecksum();
                                    tcpEncapsulation.getPacket().calculateIPv4Checksum();
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncapsulation.getSourcePort() == this.getInsideProtocolSpecificIdentifier()) {
                                    // PAT UDP XLATE
                                    udpEncapsulation.setSourcePort(this.getOutsideProtocolSpecificIdentifier());
                                    udpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    udpEncapsulation.calculateUDPChecksum();
                                    udpEncapsulation.getPacket().calculateIPv4Checksum();
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case ICMP: {
                                ICMPForIPv4QueryPacketEncapsulation icmpEncapsulation = new ICMPForIPv4QueryPacketEncapsulation(packet);
                                if (this.getInsideProtocolSpecificIdentifier() == null || (icmpEncapsulation.getIdentifier() == this.getInsideProtocolSpecificIdentifier() && icmpEncapsulation.isQueryBasedMessage())) {
                                    // PAT ICMP XLATE
                                    if (icmpEncapsulation.isQueryBasedMessage() && this.outsideProtocolSpecificIdentifier != null) {
                                        icmpEncapsulation.setIdentifier(this.getOutsideProtocolSpecificIdentifier());
                                        icmpEncapsulation.calculateICMPChecksum();
                                    }
                                    icmpEncapsulation.getPacket().setSourceIPv4Address(this.outsideAddress.getAddress());
                                    icmpEncapsulation.getPacket().calculateIPv4Checksum();
                                    this.updateLastActivity();
                                    return true;
                                }
                            }
                        }
                    } else if (this.getProtocol() == packet.getIPv4Protocol() && (this.getOutsideInterface().equals(packet.getIngressNetworkInterface()) || this.ecmpOutsideInterfaceList.contains(packet.getIngressNetworkInterface())) && this.getOutsideAddress().getAddress().equals(packet.getDestinationIPv4Address())) {
                        // Possible PAT UNXLATE
                        switch (this.getProtocol()) {
                            case TCP: {
                                TCPForIPv4PacketEncapsulation tcpEncapsulation = new TCPForIPv4PacketEncapsulation(packet);
                                if (tcpEncapsulation.getDestinationPort() == this.getOutsideProtocolSpecificIdentifier()) {
                                    // PAT TCP UNXLATE
                                    tcpEncapsulation.setDestinationPort(this.getInsideProtocolSpecificIdentifier());
                                    tcpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    tcpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    tcpEncapsulation.calculateTCPChecksum();
                                    tcpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case UDP: {
                                UDPForIPv4PacketEncapsulation udpEncapsulation = new UDPForIPv4PacketEncapsulation(packet);
                                if (udpEncapsulation.getDestinationPort() == this.getOutsideProtocolSpecificIdentifier()) {
                                    // PAT UDP UNXLATE
                                    udpEncapsulation.setDestinationPort(this.getInsideProtocolSpecificIdentifier());
                                    udpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    udpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    udpEncapsulation.calculateUDPChecksum();
                                    udpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    this.updateLastActivity();
                                    return true;
                                }
                                break;
                            }
                            case ICMP: {
                                ICMPForIPv4QueryPacketEncapsulation icmpEncapsulation = new ICMPForIPv4QueryPacketEncapsulation(packet);
                                if (this.getInsideProtocolSpecificIdentifier() == null || (icmpEncapsulation.getIdentifier() == this.getOutsideProtocolSpecificIdentifier() && icmpEncapsulation.isQueryBasedMessage())) {
                                    // PAT ICMP UNXLATE
                                    if (icmpEncapsulation.isQueryBasedMessage() && this.insideProtocolSpecificIdentifier != null) {
                                        icmpEncapsulation.setIdentifier(this.getInsideProtocolSpecificIdentifier());
                                        icmpEncapsulation.calculateICMPChecksum();
                                    }
                                    icmpEncapsulation.getPacket().setDestinationIPv4Address(this.getInsideAddress());
                                    icmpEncapsulation.getPacket().setDestinationHardwareAddress(MACAddress.ZERO);
                                    icmpEncapsulation.getPacket().calculateIPv4Checksum();
                                    packet.setProcessingChain(ProcessingChain.FORWARD);
                                    this.updateLastActivity();
                                    return true;
                                }
                            }
                        }
                    }
                } catch (PacketException | IOException ex) {
                    throw new NATTranslationException("Packet NAT Translation has failed", ex);
                }
            } else {
                throw new NATTranslationException("NAT Translation cannot process packet in processing chain " + packet.getProcessingChain().name() + " of ethernet type " + packet.getEthernetType().name());
            }
        }
        return false;
    }

    private void calculateProtocolChecksumIfNeeded(Packet packet) throws PacketException, IOException {
        if (packet.getEthernetType() == EthernetType.IPV4) {
            if (packet.getIPv4Protocol() == IPv4Protocol.TCP) {
                TCPForIPv4PacketEncapsulation encap = new TCPForIPv4PacketEncapsulation(packet);
                encap.calculateTCPChecksum();
            } else if (packet.getIPv4Protocol() == IPv4Protocol.UDP) {
                UDPForIPv4PacketEncapsulation encap = new UDPForIPv4PacketEncapsulation(packet);
                encap.calculateUDPChecksum();
            }
        }
    }

    private void updateLastActivity() {
        this.lastActivityDate = new Date();
    }

    public List<NetworkInterface> getEcmpOutsideInterfaceList() {
        return ecmpOutsideInterfaceList;
    }
}
