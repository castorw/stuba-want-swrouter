package net.ctrdn.stuba.want.swrouter.api;

import java.util.Date;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class DefaultAPIMethod implements APIMethod {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RouterController routerController;
    private final String path;

    protected DefaultAPIMethod(RouterController routerController, String path) {
        this.path = path;
        this.routerController = routerController;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    protected boolean processInputBoolean(String boolString) {
        return (boolString == null) ? false : boolString.trim().toLowerCase().equals("true");
    }

    protected long processOutputTimestamp(Date date) {
        if (date != null) {
            return date.getTime();
        }
        return -1;
    }

    protected void insertToJsonObject(JsonObjectBuilder job, String n, Object o) {
        if (o == null) {
            job.addNull(n);
        } else if (o instanceof Integer) {
            job.add(n, (Integer) o);
        } else {
            job.add(n, o.toString());
        }
    }

    protected Logger getLogger() {
        return logger;
    }

    protected RouterController getRouterController() {
        return this.routerController;
    }

    @Override
    public JsonObject executeGet(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        return null;
    }
}
