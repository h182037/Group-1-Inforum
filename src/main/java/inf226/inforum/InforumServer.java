package inf226.inforum;

import java.io.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class InforumServer extends AbstractHandler
{
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException
  {
    if (target.equals("/style.css")) {
        serveFile(response,new File("style.css"),"text/css;charset=utf-8");
        baseRequest.setHandled(true);
    } else {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("<!DOCTYPE html>");
        response.getWriter().println("<html lang=\"en-GB\">");
        response.getWriter().println("<head>");
        response.getWriter().println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">");
        response.getWriter().println("<style type=\"text/css\">code{white-space: pre;}</style>");
        response.getWriter().println("<link rel=\"stylesheet\" href=\"/style.css\">");
        response.getWriter().println("</head>");
        response.getWriter().println("<body>");
        response.getWriter().println("<h1>Inforum</h1>");
        response.getWriter().println("</body>");
        response.getWriter().println("</html>");
    }
  }

  private void serveFile(HttpServletResponse response, File file, String contentType) {
      response.setContentType(contentType);
      try {
        final InputStream is = new FileInputStream(file);
        is.transferTo(response.getOutputStream());
        is.close();
        response.setStatus(HttpServletResponse.SC_OK);
      } catch (IOException e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
  }

  public static void main(String[] args) throws Exception
  {
    Server server = new Server(8080);
    server.setHandler(new InforumServer());

    server.start();
    server.join();
  }
}
