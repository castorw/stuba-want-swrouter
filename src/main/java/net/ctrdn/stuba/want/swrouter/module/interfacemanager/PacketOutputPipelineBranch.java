package net.ctrdn.stuba.want.swrouter.module.interfacemanager;

import net.ctrdn.stuba.want.swrouter.common.DataTypeHelpers;
import net.ctrdn.stuba.want.swrouter.common.MACAddress;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketOutputPipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(PacketOutputPipelineBranch.class);

    @Override
    public String getName() {
        return "PACKET_OUTPUT";
    }

    @Override
    public String getDescription() {
        return "Transmission of output traffic";
    }

    @Override
    public int getPriority() {
        return 90000;
    }

    @Override
    public PipelineResult process(Packet packet) {
        if (packet.getProcessingChain() == ProcessingChain.OUTPUT) {
            if (packet.getDestinationHardwareAddress() != MACAddress.ZERO && packet.getEgressNetworkInterface() != null) {
                packet.getEgressNetworkInterface().sendPacket(packet);
                this.logger.debug("Transmitted packet {} over network interface {}", packet.getPacketIdentifier().getUuid().toString(), packet.getEgressNetworkInterface().getName());
                return PipelineResult.HANDLED;
            } else {
                this.logger.warn("Packet {} has no destination hardware address or egress interface", packet.getDestinationHardwareAddress());
                return PipelineResult.DROP;
            }
        }
        return PipelineResult.CONTINUE;
    }

}
