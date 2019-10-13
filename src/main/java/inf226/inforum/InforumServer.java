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
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.lang.IllegalArgumentException;

import java.time.Instant;
import inf226.inforum.storage.*;

public class InforumServer extends AbstractHandler
{
  private static final Mutable<Thread> example_thread = Mutable.init(new Thread("Serious discussion topic"));
  private final static TransientStorage<Message> message_store
     = new TransientStorage<Message>();
  private final String view_thread_path = "/viewThread/";
  private final File style = new File("style.css");
  private final File login = new File("login.html");
  private final static TransientStorage<UserContext> contextStorage
    = new TransientStorage<UserContext>();

  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException
  {
    
    final Map<String,Cookie> cookies = new TreeMap<String,Cookie>();
    final Cookie[] carray = request.getCookies();
    if(carray != null) {
       for(int i = 0; i < carray.length; ++i) {
         cookies.put(carray[i].getName(),carray[i]);
       }
    }


    final Maybe<String> username = new Maybe<String>(request.getParameter("username"));

    username.forEach( uname -> {
           try {
              Stored<UserContext> con = contextStorage.save(new UserContext(uname));
              Cookie c = new Cookie("session",con.identity.toString());
              response.addCookie(c);
              cookies.put("session",c);
           } catch (IOException foo) {}
       });

    final Maybe<Cookie> userCookie = new Maybe<Cookie>(cookies.get("session"));

    try { // All logged in actions go inside this try.
        final UUID contextID = UUID.fromString(userCookie.get().getValue());
        final Stored<UserContext> context = contextStorage.lookup(contextID).head().get();
        if (target.startsWith(view_thread_path)) {
           Maybe<Either<Stored<Forum>,Stored<Thread>>>
             resolved = resolvePath(target.substring(view_thread_path.length()), context.value);
           resolved.get().branch(
               forum -> { return ; // TODO: Display forum
                         },
               thread -> { try {
                             printThread(response.getWriter(), thread.value);
                           } catch (IOException e) { }} );
        }
    } catch (Maybe.NothingException e) {
        // User is not logged in.
    } catch (IllegalArgumentException e) {
        // The UUID was invalid.
    }
    if (target.equals("/style.css")) {
        serveFile(response,style,"text/css;charset=utf-8");
        baseRequest.setHandled(true);
    } else if (target.equals("/login")) {
        serveFile(response,login, "text/html;charset=utf-8");
        baseRequest.setHandled(true);
    } else if (target.equals("/") || target.equals("/post")) {
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
        
        printThread(response.getWriter(), example_thread.get());
        response.getWriter().println("<form class=\"login\" action=\"/post\" method=\"post\">");
        response.getWriter().println("<div class=\"messagebox\">Message: <input type=\"text\" name=\"message\"></div>");
        response.getWriter().println("<div class=\"sendbutton\"><input type=\"submit\" name=\"send\" value=\"Send\"></div>");
        response.getWriter().println("</form>");

        response.getWriter().println("</body>");
        response.getWriter().println("</html>");
    }
  }

  private Maybe<Either<Stored<Forum>,Stored<Thread>>>
       resolvePath(final String forum_path, final UserContext context) {
   final Maybe.Builder<Either<Stored<Forum>,Stored<Thread>>> result = Maybe.builder();
   context.forums.forEach( forum ->{
                if (forum_path.startsWith(forum.value.handle + "/")) {
                    final Pair<Stored<Forum>,String>
                      pair = Forum.resolveSubforum(forum,forum_path.substring(forum.value.handle.length() + 1));
                    final Stored<Forum> subforum = pair.first;
                    final String remaining_path = pair.second;
                    if (remaining_path.equals("")) {
                        result.accept(Either.left(subforum));
                    } else {
                       UUID threadID = UUID.fromString(pair.second);
                       subforum.value.threads.forEach(thread -> {
                         if(thread.identity.equals(threadID))
                            result.accept(Either.right(thread));
                       });
                    }
                }});
    return result.getMaybe();
  }

  private void printForum(final PrintWriter w, final Forum forum) {
    w.println("<section>");
    w.println("  <header class=\"forum-head\">");
    w.println("  <h2 class=\"topic\">" + forum.name + "</h2>");
    w.println("  </header>");
    w.println("  <section>");
    w.println("  <header><h3>Subforums</h3></header>");
    w.println("  <list class=\"subforum\">");
    forum.subforums.forEach(
        subforum -> { w.println("    <li>" + "<a href=\"" + subforum.value.handle + "/\">" + subforum.value.name  + "</a></li>") ; });
    w.println("  </list>");
    w.println("  </section>");
    w.println("  <section class=\"forum-threads\">");
    w.println("  <header><h3>Threads</h3></header>");
    forum.threads.forEach(
        thread -> { w.println("    <div class=\"thread-stub\">" + "<a href=\"" + thread.identity + "/\">" + thread.value.topic  + "</a></div>") ; });
    w.println("  </section>");
    w.println("</section>");
    
  }

  private void printThread(PrintWriter w, Thread thread) {
    // TODO: Prevent XSS
    w.println("<section>");
    w.println("  <header class=\"thread-head\">");
    w.println("  <h2 class=\"topic\">" + thread.topic + "</h2>");
    try {
       final String starter = thread.messages.last.get().sender;
       final Instant date = thread.messages.last.get().date;
       w.println("  <div class=\"starter\">" + starter + "</div>");
       w.println("  <div class=\"date\">" + date.toString() + "</div>");
    } catch(Maybe.NothingException e) {
       w.println("  <p>This thread is empty.</p>");
    }
    w.println("  </header>");
    thread.messages.reverse().forEach (m -> {
         w.println("  <div class=\"entry\">");
         w.println("     <div class=\"user\">" + m.sender + "</div>");
         w.println("     <div class=\"text\">" + m.message + "</div>");
         w.println("     <form class=\"controls\" action=\"message_action\" method=\"POST\">");
         w.println("        <input class=\"controls\" name=\"messageid\" type=\"hidden\" value=\"\">");
         w.println("        <input class=\"controls\" type=\"submit\" name=\"edit\" value=\"Edit\"/>\n");
         w.println("        <input class=\"controls\" type=\"submit\" name=\"delete\" value=\"Delete\"/>\n");
         w.println("     </form>");
         w.println("  </div>");
     });
    w.println("</section>");
  }

  private void serveFile(HttpServletResponse response, File file, String contentType) {
      response.setContentType(contentType);
      try {
        final InputStream is = new FileInputStream(file);
        // Better if we can upgrade to JDK 1.9: is.transferTo(response.getOutputStream());
        final OutputStream os = response.getOutputStream();
        final byte[] buffer = new byte[1024];
        for(int len = is.read(buffer);
                len >= 0;
                len = is.read(buffer)) {
           os.write(buffer, 0, len);
        }
        is.close();
        response.setStatus(HttpServletResponse.SC_OK);
      } catch (IOException e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
  }

  public static void main(String[] args) throws Exception
  {
    Message testMessage = new Message("Joe","<p> This a message. </p>", Instant.now());
    Message testMessage2 = new Message("Bob","<p> This another message. </p>", Instant.now());
    example_thread.accept(
        new Thread("A serious discussion!",
          ImmutableList.cons(testMessage2,ImmutableList.cons(testMessage,ImmutableList.empty()))));

    Server server = new Server(8080);
    server.setHandler(new InforumServer());

    server.start();
    server.join();
  }
}
