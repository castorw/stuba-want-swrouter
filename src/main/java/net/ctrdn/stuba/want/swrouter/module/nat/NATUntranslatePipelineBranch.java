package net.ctrdn.stuba.want.swrouter.module.nat;

import net.ctrdn.stuba.want.swrouter.common.EthernetType;
import net.ctrdn.stuba.want.swrouter.core.processing.DefaultPipelineBranch;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineResult;
import net.ctrdn.stuba.want.swrouter.core.processing.ProcessingChain;
import net.ctrdn.stuba.want.swrouter.exception.NATTranslationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NATUntranslatePipelineBranch extends DefaultPipelineBranch {

    private final Logger logger = LoggerFactory.getLogger(NATUntranslatePipelineBranch.class);
    private final NATModule natModule;

    public NATUntranslatePipelineBranch(NATModule natModule) {
        this.natModule = natModule;
    }

    @Override
    public String getName() {
        return "NAT_UNXLATE";
    }

    @Override
    public String getDescription() {
        return "Performs NAT untranslation";
    }

    @Override
    public int getPriority() {
        return 512;
    }

    @Override
    public PipelineResult process(Packet packet) {
        if ((packet.getProcessingChain() == ProcessingChain.FORWARD || packet.getProcessingChain() == ProcessingChain.INPUT) && packet.getIngressNetworkInterface() != null && packet.getEthernetType() == EthernetType.IPV4) {
            try {
                for (NATTranslation translation : this.natModule.getTranslationList()) {
                    if (translation.matchAndApply(packet)) {
                        this.logger.debug("NAT Translation {} has address/port untranslation on packet {}", translation, packet.getPacketIdentifier().getUuid());
                        return PipelineResult.CONTINUE;
                    }
                }
            } catch (NATTranslationException ex) {
                this.logger.warn("NAT Translation processing has failed untranslating packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
                return PipelineResult.DROP;
            }
            this.logger.trace("No NAT UNXLATE done on packet {}", packet.getPacketIdentifier().getUuid().toString());
        }
        return PipelineResult.CONTINUE;
    }
}