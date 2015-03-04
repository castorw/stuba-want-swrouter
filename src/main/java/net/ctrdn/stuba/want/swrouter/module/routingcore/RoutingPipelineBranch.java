package net.ctrdn.stuba.want.swrouter.module.routingcore;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingPipelineBranch extends DefaultPipelineBranch {
    
    private final Logger logger = LoggerFactory.getLogger(RoutingPipelineBranch.class);
    private final RoutingCoreModule routingCoreModule;
    
    public RoutingPipelineBranch(RoutingCoreModule routingCoreModule) {
        this.routingCoreModule = routingCoreModule;
    }
    
    @Override
    public String getName() {
        return "ROUTING";
    }
    
    @Override
    public String getDescription() {
        return "Provides routing information required for packet forwarding";
    }
    
    @Override
    public int getPriority() {
        return 2048;
    }
    
    @Override
    public PipelineResult process(Packet packet) {
        if (packet.getProcessingChain() == ProcessingChain.FORWARD && packet.getEthernetType() == EthernetType.IPV4) {
            try {
                NetworkInterface destinationInterfaceLookup = this.lookupInterface(packet.getDestinationIPv4Address());
                if (destinationInterfaceLookup != null) {
                    // the target network is connected
                    packet.setDestinationHardwareAddress(MACAddress.ZERO);
                    packet.setEgressNetworkInterface(destinationInterfaceLookup);
                    return PipelineResult.CONTINUE;
                } else {
                    // the target network is accessed via nexthop router
                    IPv4Route route = this.routingCoreModule.lookupRoute(packet.getDestinationIPv4Address());
                    if (route != null) {
                        NetworkInterface egressInterface = this.lookupInterface(route.getNextHopAddress());
                        if (egressInterface != null) {
                            packet.setEgressNetworkInterface(egressInterface);
                            packet.setForwarderIPv4Address(route.getNextHopAddress());
                            return PipelineResult.CONTINUE;
                        } else {
                            this.logger.info("No interface for route {} for packet {}", route, packet.getPacketIdentifier().getUuid().toString());
                        }
                    } else {
                        this.logger.info("No route to host {} for packet {}", packet.getDestinationIPv4Address(), packet.getPacketIdentifier().getUuid().toString());
                    }
                }
            } catch (PacketException ex) {
                this.logger.warn("Routing lookup failed for packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
            }
            return PipelineResult.DROP;
        } else if (packet.getProcessingChain() == ProcessingChain.FORWARD) {
            this.logger.debug("Unsupported protocol with FORWARD chain attached {} on packet {}", packet.getEthernetType(), packet.getPacketIdentifier().getUuid().toString());
            return PipelineResult.DROP;
        }
        return PipelineResult.CONTINUE;
    }
    
    private NetworkInterface lookupInterface(IPv4Address address) {
        try {
            InterfaceManagerModule interfaceManagerModule = this.routingCoreModule.getRouterController().getModule(InterfaceManagerModule.class);
            for (NetworkInterface nic : interfaceManagerModule.getNetworkInterfaces()) {
                if (nic.getIPv4InterfaceAddress() != null && nic.getIPv4InterfaceAddress().getPrefix().containsAddress(address)) {
                    return nic;
                }
            }
            return null;
        } catch (NoSuchModuleException ex) {
            throw new RuntimeException(ex);
        }
    }
}
