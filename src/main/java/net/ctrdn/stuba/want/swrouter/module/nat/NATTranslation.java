package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.Date;
import net.ctrdn.stuba.want.swrouter.core.processing.Packet;
import net.ctrdn.stuba.want.swrouter.exception.NATTranslationException;

public interface NATTranslation {

    public boolean isActive();

    public Date getLastActivityDate();

    public boolean matchAndApply(Packet packet) throws NATTranslationException;
}
