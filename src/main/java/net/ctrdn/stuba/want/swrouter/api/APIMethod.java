package net.ctrdn.stuba.want.swrouter.api;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public interface APIMethod {

    public String getPath();

    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException;

    public JsonObject executeGet(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException;
}
