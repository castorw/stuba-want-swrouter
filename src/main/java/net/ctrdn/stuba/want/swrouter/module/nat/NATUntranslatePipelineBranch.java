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
                for (int i = this.natModule.getTranslationList().size() - 1; i > -1; i--) {
                    NATTranslation translation = this.natModule.getTranslationList().get(i);
                    if (translation.apply(packet)) {
                        this.logger.debug("Packet {} has been un-translated using {}", packet.getPacketIdentifier().getUuid(), translation);
                        return PipelineResult.CONTINUE;
                    }
                }
            } catch (NATTranslationException ex) {
                this.logger.warn("NAT Translation processing has failed un-translating packet {}", packet.getPacketIdentifier().getUuid().toString(), ex);
                return PipelineResult.DROP;
            }
            for (NATRule rule : this.natModule.getRuleList()) {
                NATRuleResult result = rule.untranslate(packet);
                if (result == NATRuleResult.HANDLED) {
                    this.logger.trace("NAT Rule #{} {} UNXLATE has handled packet {} received via {}", rule.getPriority(), rule.getTypeString(), packet.getPacketIdentifier().getUuid().toString(), packet.getIngressNetworkInterface().getName());
                    return PipelineResult.CONTINUE;
                } else if (result == NATRuleResult.DROP) {
                    this.logger.info("NAT Rule #{} {} UNXLATE has dropped packet {} received via {}", rule.getPriority(), rule.getTypeString(), packet.getPacketIdentifier().getUuid().toString(), packet.getIngressNetworkInterface().getName());
                    return PipelineResult.DROP;
                }
            }
        }
        this.logger.trace("No NAT UNXLATE done on packet {}", packet.getPacketIdentifier().getUuid().toString());
        return PipelineResult.CONTINUE;
    }
}
