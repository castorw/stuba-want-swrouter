package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.UUID;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.exception.NATException;

public interface NATRule {

    public UUID getID();

    public String getTypeString();

    public int getPriority();

    public NATRuleResult translate(Packet packet);

    public NATRuleResult untranslate(Packet packet);

    public void clear() throws NATException;

    public void onTranslationDeactivated(NATTranslation translation);
}
