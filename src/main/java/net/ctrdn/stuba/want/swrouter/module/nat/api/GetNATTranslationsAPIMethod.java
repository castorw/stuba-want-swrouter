package net.ctrdn.stuba.want.swrouter.module.nat.api;

import java.util.Date;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATTranslation;

public class GetNATTranslationsAPIMethod extends DefaultAPIMethod {

    public GetNATTranslationsAPIMethod(RouterController routerController) {
        super(routerController, "get-nat-translations");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder xlationsJab = Json.createArrayBuilder();
            for (NATTranslation xlation : natModule.getTranslationList()) {
                JsonObjectBuilder xlationJob = Json.createObjectBuilder();
                xlationJob.add("Rule", xlation.toString());
                xlationJob.add("LastActivityDate", xlation.getLastActivityDate().toString());
                xlationJob.add("Timeout", xlation.getTimeout());
                xlationJob.add("TimeRemaining", xlation.getTimeout() - (new Date().getTime() - xlation.getLastActivityDate().getTime()));
                xlationJob.add("TranslateHitCount", xlation.getTranslateHitCount());
                xlationJob.add("UntranslateHitCount", xlation.getUntranslateHitCount());
                xlationJob.add("Active", xlation.isActive());
                xlationsJab.add(xlationJob);
            }
            responseJob.add("NATTranslations", xlationsJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        }
    }

}
