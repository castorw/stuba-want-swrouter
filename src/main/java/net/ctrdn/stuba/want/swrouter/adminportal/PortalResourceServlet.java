package net.ctrdn.stuba.want.swrouter.adminportal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PortalResourceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURI();
        if (requestUrl.equals("/")) {
            requestUrl = "/site.html";
        }
        String requestFileName = "/net/ctrdn/stuba/want/swrouter/adminportal" + requestUrl;
        InputStream is = getClass().getResourceAsStream(requestFileName);
        if (is != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            String fileNameLc = requestFileName.toLowerCase();
            if (fileNameLc.endsWith(".html")) {
                response.setContentType("text/html");
            } else if (fileNameLc.endsWith(".js")) {
                response.setContentType("text/javascript");
            } else if (fileNameLc.endsWith(".css")) {
                response.setContentType("text/css");
            } else if (fileNameLc.endsWith(".png")) {
                response.setContentType("image/png");
            } else if (fileNameLc.endsWith(".svg")) {
                response.setContentType("image/svg");
            } else if (fileNameLc.endsWith(".eot")) {
                response.setContentType("font/opentype");
            } else if (fileNameLc.endsWith(".ttf")) {
                response.setContentType("application/x-font-ttf");
            } else if (fileNameLc.endsWith(".woff")) {
                response.setContentType("application/x-font-woff");
            } else {
                throw new ServletException("Unknown file type");
            }
            if (fileNameLc.endsWith(".html")) {
                byte[] data = this.preprocess(is);
                response.getOutputStream().write(data, 0, data.length);
            } else {
                byte[] buffer = new byte[1024];
                while (is.available() > 0) {
                    int rd = is.read(buffer, 0, buffer.length);
                    response.getOutputStream().write(buffer, 0, rd);
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("404 Not Found");
        }
    }

    private byte[] preprocess(InputStream is) throws IOException, ServletException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (is.available() > 0) {
            int rd = is.read(buffer, 0, buffer.length);
            baos.write(buffer, 0, rd);
        }

        String htmlString = baos.toString("UTF-8");
        return htmlString.getBytes("UTF-8");
    }
}
