package net.ctrdn.stuba.want.swrouter.module.routingcore;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4NetworkMask;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.exception.PacketException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(RoutingPipelineBranch.class);
    private final IPv4Prefix multicastPrefix;
    private final RoutingCoreModule routingCoreModule;

    public RoutingPipelineBranch(RoutingCoreModule routingCoreModule) {
        this.routingCoreModule = routingCoreModule;
        try {
            this.multicastPrefix = new IPv4Prefix(IPv4Address.fromString("224.0.0.0"), new IPv4NetworkMask(4));
        } catch (IPv4MathException ex) {
            throw new RuntimeException(ex);
        }
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
        if ((packet.getProcessingChain() == ProcessingChain.FORWARD || packet.getProcessingChain() == ProcessingChain.OUTPUT) && packet.getEthernetType() == EthernetType.IPV4) {
            try {
                if (this.multicastPrefix.containsAddress(packet.getDestinationIPv4Address())) {
                    this.logger.debug("Ignoring packet {} with multicast destination {}", packet.getPacketIdentifier().getUuid().toString(), packet.getDestinationIPv4Address());
                    return PipelineResult.CONTINUE;
                }
                // the target network is accessed via nexthop router
                IPv4Route route = this.routingCoreModule.lookupRoute(packet.getDestinationIPv4Address());
                if (route != null) {
                    IPv4RouteGateway gateway = route.getNextGateway();
                    if (gateway.getGatewayAddress() != null) {
                        IPv4Address nextHopAddress = gateway.getGatewayAddress();
                        NetworkInterface egressInterface = this.lookupInterface(nextHopAddress);
                        if (egressInterface != null) {
                            packet.setEgressNetworkInterface(egressInterface);
                            packet.setForwarderIPv4Address(nextHopAddress);
                            return PipelineResult.CONTINUE;
                        } else {
                            this.logger.warn("No interface available for route {} for packet {}", route, packet.getPacketIdentifier().getUuid().toString());
                        }
                    } else if (gateway.getGatewayInterface() != null) {
                        packet.setEgressNetworkInterface(gateway.getGatewayInterface());
                        packet.setDestinationHardwareAddress(MACAddress.ZERO);
                        return PipelineResult.CONTINUE;
                    }
                }
                this.logger.info("No route to host {} for packet {}", packet.getDestinationIPv4Address(), packet.getPacketIdentifier().getUuid().toString());
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
