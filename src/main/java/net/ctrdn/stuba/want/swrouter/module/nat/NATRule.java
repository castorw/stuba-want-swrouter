package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.List;
import java.util.UUID;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public interface NATRule {

    public UUID getID();

    public String getTypeString();

    public int getPriority();

    public void setPriority(int priority);

    public NATRuleResult translate(Packet packet);

    public NATRuleResult untranslate(Packet packet);

    public void clear() throws NATException;

    public void onTranslationDeactivated(NATTranslation translation);

    public List<NetworkInterface> getEcmpOutsideInterfaceList();
}
