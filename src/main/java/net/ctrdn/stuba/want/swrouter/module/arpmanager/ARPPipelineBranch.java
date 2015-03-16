package net.ctrdn.stuba.want.swrouter.module.arpmanager;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ARPPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(ARPPipelineBranch.class);
    private final ARPManagerModule arpManagerModule;

    public ARPPipelineBranch(ARPManagerModule arpManagerModule) {
        this.arpManagerModule = arpManagerModule;
    }

    @Override
    public String getName() {
        return "ARP";
    }

    @Override
    public String getDescription() {
        return "ARP cache and address resolution";
    }

    @Override
    public int getPriority() {
        return 65535;
    }

    @Override
    public PipelineResult process(Packet packet) {
        if (packet.getEthernetType() == EthernetType.ARP) {
            if (packet.getProcessingChain() == ProcessingChain.INPUT) {
                try {
                    ARPForIPv4PacketEncapsulation arpEncapsulation = new ARPForIPv4PacketEncapsulation(packet);
                    if (packet.getDestinationHardwareAddress().isBroadcast() && arpEncapsulation.getOperation() == ARPForIPv4PacketEncapsulation.Operation.REQUEST) {
                        if (this.processARPRequest(arpEncapsulation)) {
                            return PipelineResult.HANDLED;
                        }
                    } else if (arpEncapsulation.getOperation() == ARPForIPv4PacketEncapsulation.Operation.REPLY) {
                        if (this.processARPReply(arpEncapsulation)) {
                            return PipelineResult.HANDLED;
                        }
                    }
                } catch (PacketException ex) {
                    this.logger.debug("Not processing ARP packet {}", packet.getPacketIdentifier().getUuid(), ex);
                }
            } else if (packet.getProcessingChain() == ProcessingChain.OUTPUT) {
                return PipelineResult.CONTINUE;
            }
            return PipelineResult.DROP;
        } else if (packet.getEthernetType() == EthernetType.IPV4) {
            try {
                if ((packet.getProcessingChain() == ProcessingChain.FORWARD || packet.getProcessingChain() == ProcessingChain.OUTPUT) && packet.getForwarderIPv4Address() != null && (packet.getForwarderHardwareAddress() == null || packet.getForwarderHardwareAddress().equals(MACAddress.ZERO))) {
                    ARPTableEntry arpTableEntry = this.resolveARPTableEntry(packet.getForwarderIPv4Address(), packet.getEgressNetworkInterface());
                    if (arpTableEntry.isComplete()) {
                        packet.setForwarderHardwareAddress(arpTableEntry.getHardwareAddress());
                    } else {
                        this.logger.info("Failed to resolve forwarder {} hardware address using ARP for packet {} on interface {}", packet.getForwarderIPv4Address(), packet.getPacketIdentifier().getUuid().toString(), packet.getEgressNetworkInterface().getName());
                        return PipelineResult.DROP;
                    }
                } else if ((packet.getProcessingChain() == ProcessingChain.OUTPUT && packet.getDestinationHardwareAddress().equals(MACAddress.ZERO))
                        || (packet.getProcessingChain() == ProcessingChain.FORWARD && packet.getForwarderIPv4Address() == null && (packet.getDestinationHardwareAddress() == null || packet.getDestinationHardwareAddress().equals(MACAddress.ZERO)) && packet.getEgressNetworkInterface() != null)) {
                    ARPTableEntry arpTableEntry = this.resolveARPTableEntry(packet.getDestinationIPv4Address(), packet.getEgressNetworkInterface());
                    if (arpTableEntry.isComplete()) {
                        packet.setDestinationHardwareAddress(arpTableEntry.getHardwareAddress());
                    } else {
                        this.logger.info("Failed to resolve destination {} hardware address using ARP for packet {} on interface {}", packet.getDestinationIPv4Address(), packet.getPacketIdentifier().getUuid().toString(), packet.getEgressNetworkInterface().getName());
                        return PipelineResult.DROP;
                    }
                }
            } catch (PacketException ex) {
                this.logger.warn("Failed processing ARP on IPv4 packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
            }
        }
        return PipelineResult.CONTINUE;
    }

    private boolean processARPRequest(ARPForIPv4PacketEncapsulation requestEncap) {
        if (requestEncap.getTargetProtocolAddress().equals(requestEncap.getPacket().getIngressNetworkInterface().getIPv4InterfaceAddress().getAddress()) || (this.arpManagerModule.getVirtualAddressMap().containsKey(requestEncap.getTargetProtocolAddress()) && this.arpManagerModule.getVirtualAddressMap().get(requestEncap.getTargetProtocolAddress()).equals(requestEncap.getPacket().getIngressNetworkInterface()))) {
            try {
                this.logger.debug("Received ARP request targeted for us from {}@{} on interface {}", requestEncap.getSenderProtocolAddress(), requestEncap.getSenderHardwareAddress(), requestEncap.getPacket().getIngressNetworkInterface().getName());
                Packet replyPacket = new Packet(42, requestEncap.getPacket().getIngressNetworkInterface());
                replyPacket.setEthernetType(EthernetType.ARP);
                ARPForIPv4PacketEncapsulation replyEncap = new ARPForIPv4PacketEncapsulation(replyPacket);
                replyEncap.getPacket().setDestinationHardwareAddress(requestEncap.getSenderHardwareAddress());
                replyEncap.setHardwareType(1);
                replyEncap.setProtocolType(EthernetType.IPV4);
                replyEncap.setHardwareAddressLength((short) 6);
                replyEncap.setProtocolAddressLength((short) 4);
                replyEncap.setOperation(ARPForIPv4PacketEncapsulation.Operation.REPLY);
                replyEncap.setSenderHardwareAddress(requestEncap.getPacket().getIngressNetworkInterface().getHardwareAddress());
                replyEncap.setSenderProtocolAddress(requestEncap.getTargetProtocolAddress());
                replyEncap.setTargetHardwareAddress(requestEncap.getSenderHardwareAddress());
                replyEncap.setTargetProtocolAddress(requestEncap.getSenderProtocolAddress());
                this.arpManagerModule.getRouterController().getPacketProcessor().processPacket(replyEncap.getPacket());
                this.logger.debug("Sending ARP reply about local address {}@{} on interface {} to {}@{}", requestEncap.getPacket().getIngressNetworkInterface().getIPv4InterfaceAddress().getAddress(), requestEncap.getPacket().getIngressNetworkInterface().getHardwareAddress(), requestEncap.getPacket().getIngressNetworkInterface().getName(), replyEncap.getTargetProtocolAddress(), replyEncap.getTargetHardwareAddress());
                this.arpManagerModule.updateARPTable(replyEncap.getTargetProtocolAddress(), replyEncap.getTargetHardwareAddress(), replyEncap.getPacket().getEgressNetworkInterface());
                return true;
            } catch (PacketException ex) {
                this.logger.warn("Failed to generate ARP reply", ex);
            }
        }
        return false;
    }

    private boolean processARPReply(ARPForIPv4PacketEncapsulation replyEncap) {
        if (replyEncap.getTargetHardwareAddress().equals(replyEncap.getPacket().getIngressNetworkInterface().getHardwareAddress()) && replyEncap.getTargetProtocolAddress().equals(replyEncap.getPacket().getIngressNetworkInterface().getIPv4InterfaceAddress().getAddress())) {
            this.logger.debug("Received ARP reply from {}@{} on interface {}", replyEncap.getSenderProtocolAddress(), replyEncap.getSenderHardwareAddress(), replyEncap.getPacket().getIngressNetworkInterface().getName());
            this.arpManagerModule.updateARPTable(replyEncap.getSenderProtocolAddress(), replyEncap.getSenderHardwareAddress(), replyEncap.getPacket().getIngressNetworkInterface());
            return true;
        }
        return false;
    }

    private void sendARPRequest(IPv4Address protocolAddress, NetworkInterface networkInterface) throws PacketException {
        Packet requestPacket = new Packet(42, networkInterface);
        requestPacket.setEthernetType(EthernetType.ARP);
        ARPForIPv4PacketEncapsulation requestEncap = new ARPForIPv4PacketEncapsulation(requestPacket);
        requestEncap.getPacket().setDestinationHardwareAddress(MACAddress.BROADCAST);
        requestEncap.setHardwareType(1);
        requestEncap.setProtocolType(EthernetType.IPV4);
        requestEncap.setHardwareAddressLength((short) 6);
        requestEncap.setProtocolAddressLength((short) 4);
        requestEncap.setOperation(ARPForIPv4PacketEncapsulation.Operation.REQUEST);
        requestEncap.setSenderHardwareAddress(networkInterface.getHardwareAddress());
        requestEncap.setSenderProtocolAddress(networkInterface.getIPv4InterfaceAddress().getAddress());
        requestEncap.setTargetHardwareAddress(MACAddress.ZERO);
        requestEncap.setTargetProtocolAddress(protocolAddress);

        this.arpManagerModule.getRouterController().getPacketProcessor().processPacket(requestPacket);
        this.logger.debug("Sending ARP request broadcast about {} on interface {}", protocolAddress, networkInterface.getName());
    }

    private ARPTableEntry resolveARPTableEntry(IPv4Address protocolAddress, NetworkInterface networkInterface) {
        ARPTableEntry entry = this.arpManagerModule.getARPTableEntry(protocolAddress, networkInterface);
        if (!entry.isComplete()) {
            try {
                this.sendARPRequest(protocolAddress, networkInterface);
                synchronized (entry.getUpdateLock()) {
                    entry.getUpdateLock().wait(this.arpManagerModule.getPipelineResolutionTimeout());
                }
            } catch (InterruptedException | PacketException ex) {
                this.logger.info("ARP resolution has failed with error", ex);
            }
        }
        return entry;
    }
}
