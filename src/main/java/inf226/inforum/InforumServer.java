package inf226.inforum;

import java.io.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import java.util.TreeMap;
import java.util.Map;
import java.util.UUID;
import java.lang.IllegalArgumentException;

import java.time.Instant;
import inf226.inforum.storage.*;

public class InforumServer extends AbstractHandler
{
  private final String view_thread_path = "/viewThread/";
  private final File style = new File("style.css");
  private final static TransientStorage<UserContext> contextStorage
    = new TransientStorage<UserContext>();

  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException
  {
    
    final Map<String,Cookie> cookies = new TreeMap<String,Cookie>();
    for(Cookie c : request.getCookies()) {
       cookies.put(c.getName(),c);
    }
    final Maybe<Cookie> userCookie = new Maybe<Cookie>(cookies.get("user"));

    try { // All logged in actions go inside this try.
        final UUID contextID = UUID.fromString(userCookie.get().getValue());
        final Stored<UserContext> context = contextStorage.lookup(contextID).head().get();

        if (target.startsWith(view_thread_path)) {
           final String forum_path = target.substring(view_thread_path.length());
           context.value.forums.forEach( forum ->{
                if (forum_path.startsWith(forum.value.handle + "/")) {
                    Pair<Forum,String> pair = forum.value.resolveSubforum(forum_path.substring(forum.value.handle.length() + 1));
                    Forum subforum = pair.first;
                    UUID threadID = UUID.fromString(pair.second);
                    subforum.threads.forEach(thread -> {
                        // TODO check if this is the correct forum and print it.
                    });
                }});
                   
        }
    } catch (Maybe.NothingException e) {
        // User is not logged in.
    } catch (IllegalArgumentException e) {
        // The UUID was invalid.
    }
    if (target.equals("/style.css")) {
        serveFile(response,style,"text/css;charset=utf-8");
        baseRequest.setHandled(true);
    } else if (target.equals("/")) {
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
        Stored<Message> testMessage = new Stored<Message>(new Message("Joe","<p> This a message. </p>", Instant.now()));
        Stored<Message> testMessage2 = new Stored<Message>(new Message("Bob","<p> This another message. </p>", Instant.now()));
        Thread t = new Thread("A test topic!",ImmutableList.cons(testMessage2,ImmutableList.cons(testMessage,ImmutableList.empty())));
        printThread(response.getWriter(), t);
        response.getWriter().println("</body>");
        response.getWriter().println("</html>");
    }
  }

  private void printThread(PrintWriter w, Thread thread) {
    // TODO: Prevent XSS
    w.println("<section>");
    w.println("  <header class=\"thread-head\">");
    w.println("  <h2 class=\"topic\">" + thread.topic + "</h2>");
    try {
       final String starter = thread.messages.last.get().value.sender;
       final Instant date = thread.messages.last.get().value.date;
       w.println("  <div class=\"starter\">" + starter + "</div>");
       w.println("  <div class=\"date\">" + date.toString() + "</div>");
    } catch(Maybe.NothingException e) {
       w.println("  <p>This thread is empty.</p>");
    }
    w.println("  </header>");
    thread.messages.reverse().forEach (m -> {
         w.println("  <div class=\"entry\">");
         w.println("     <div class=\"user\">" + m.value.sender + "</div>");
         w.println("     <div class=\"text\">" + m.value.message + "</div>");
         w.println("  </div>");
     });
    w.println("</section>");
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
