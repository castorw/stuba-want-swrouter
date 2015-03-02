package net.ctrdn.stuba.want.swrouter.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.APIRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIServlet extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(APIServlet.class);
    private final RouterController routerController;

    public APIServlet(RouterController routerController) {
        this.routerController = routerController;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        int outputStatus = HttpServletResponse.SC_OK;

        response.setContentType("text/json");
        String apiCallName = request.getRequestURI().replace(request.getServletPath() + "/", "");
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        try {
            APIMethod method = APIMethodRegistry.getInstance().getMethod(apiCallName);
            if (method != null) {
                try {
                    JsonObjectBuilder methodJob = method.execute(this.routerController, request, response);
                    responseJob.add("Response", methodJob);
                    responseJob.add("Status", true);
                } catch (APIMethodUserException ex) {
                    responseJob.add("Status", true);
                    responseJob.add("UserError", ex.getMessage());
                    this.logger.trace("[" + request.getRemoteAddr() + "] API method produced user error: " + ex.getMessage());
                } catch (APIMethodException ex) {
                    responseJob.add("Status", false);
                    responseJob.add("Error", "ApiMethodException: " + ex.getMessage());
                    this.logger.info("[" + request.getRemoteAddr() + "] API method invocation failed", ex);
                }
            } else {
                responseJob.add("Status", false);
                responseJob.add("Error", "Unknown method " + apiCallName);
                this.logger.info("[" + request.getRemoteAddr() + "] Unknown API method requested " + apiCallName);
            }
        } catch (APIRegistryException ex) {
            responseJob.add("Status", false);
            responseJob.add("Error", "Internal error while processing request");
            outputStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            this.logger.warn("Failed to resolve API method", ex);
        }

        response.setStatus(outputStatus);
        Map<String, Object> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());
        jw.writeObject(responseJob.build());
    }
}
