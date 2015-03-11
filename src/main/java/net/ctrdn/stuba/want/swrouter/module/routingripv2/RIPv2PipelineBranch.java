package net.ctrdn.stuba.want.swrouter.module.routingripv2;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.core.processing.UDPForIPv4PacketEncapsulation;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.exception.RIPv2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RIPv2PipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(RIPv2PipelineBranch.class);
    private final RIPv2RoutingModule routingModule;

    public RIPv2PipelineBranch(RIPv2RoutingModule routingModule) {
        this.routingModule = routingModule;
    }

    @Override
    public String getName() {
        return "RIPV2";
    }

    @Override
    public String getDescription() {
        return "Processes RIPv2 packets";
    }

    @Override
    public int getPriority() {
        return 64000;
    }

    @Override
    public PipelineResult process(Packet packet) {
        try {
            if ((packet.getProcessingChain() == ProcessingChain.FORWARD || packet.getProcessingChain() == ProcessingChain.INPUT) && packet.getEthernetType() == EthernetType.IPV4 && (packet.getDestinationIPv4Address().equals(this.routingModule.getRIPv2MulticastIPv4Address()) || packet.getDestinationIPv4Address().equals(packet.getIngressNetworkInterface().getIPv4InterfaceAddress().getAddress())) && packet.getDestinationHardwareAddress().equals(this.routingModule.getRIPv2MulticastIPv4Address().getMulticastMACAddress())) {
                if (packet.getIPv4Protocol() == IPv4Protocol.UDP) {
                    UDPForIPv4PacketEncapsulation udpEncap = new UDPForIPv4PacketEncapsulation(packet);
                    if (udpEncap.getDestinationPort() != 520) {
                        return PipelineResult.CONTINUE;
                    }
                } else {
                    return PipelineResult.CONTINUE;
                }
                RIPv2NetworkInterfaceConfiguration interfaceConfiguration = this.routingModule.getNetworkInterfaceConfiguration(packet.getIngressNetworkInterface());
                if (interfaceConfiguration.isEnabled()) {
                    this.processRIPPacket(packet);
                } else {
                    this.logger.debug("Ignoring RIP packet on interface {} - RIP is not enabled on the interface", packet.getIngressNetworkInterface().getName());
                }
                return PipelineResult.HANDLED;
            }
        } catch (PacketException | IPv4MathException ex) {
            this.logger.warn("Error processing RIPv2 packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
        }
        return PipelineResult.CONTINUE;
    }

    private void processRIPPacket(Packet packet) throws PacketException {
        try {
            RIPv2PacketEncapsulation ripEncap = new RIPv2PacketEncapsulation(new UDPForIPv4PacketEncapsulation(packet), false);
            if (ripEncap.getCommand() == (short) 2) {
                this.logger.debug("Received RIPv2 Response from {}", packet.getSourceIPv4Address());
                for (RIPv2RouteEntry entry : ripEncap.getRouteEntries()) {
                    this.routingModule.processRouteEntry(entry);
                }
            } else {
                this.logger.debug("Received RIPv2 Request from {}", packet.getSourceIPv4Address());
                RIPv2RouteEntry[] entries = ripEncap.getRouteEntries();
                if (entries.length == 1) {
                    RIPv2RouteEntry entry = entries[0];
                    if (entry.isFullTableRequest()) {
                        this.routingModule.processRouteEntry(entry);
                    }
                }
                throw new UnsupportedOperationException("RIPv2 specific route entry update not supported");
            }
        } catch (PacketException ex) {
            this.logger.warn("Failed processing RIP packet", ex);
        } catch (RIPv2Exception ex) {
            this.logger.info("Received unspported RIP packet from {} at interface {}", packet.getSourceIPv4Address(), packet.getIngressNetworkInterface().getName(), ex);
        }
    }
}
