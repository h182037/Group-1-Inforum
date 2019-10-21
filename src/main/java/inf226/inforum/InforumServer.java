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


import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

import java.time.Instant;
import inf226.inforum.storage.*;
import inf226.inforum.storage.DeletedException;

public class InforumServer extends AbstractHandler
{
  private static final Mutable<Thread> example_thread = Mutable.init(new Thread("Serious discussion topic"));
  private final static TransientStorage<Message> message_store
     = new TransientStorage<Message>();
  private final String view_thread_path = "/forum/";
  private final File style = new File("style.css");
  private final File login = new File("login.html");
  private final File register = new File("register.html");

  private static final String dburl = "jdbc:sqlite:production.db";

  private static MessageStorage messageStore;
  private static UserStorage userStore;
  private static ThreadStorage threadStore;
  private static ForumStorage forumStore;
  private static UserContextStorage contextStore;


  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException
  {
    final Map<String,Cookie> cookies = getCookies(request);



    // Pages which do not require login
    
    if (target.equals("/style.css")) {
        serveFile(response,style,"text/css;charset=utf-8");
        baseRequest.setHandled(true);
        return;
    } else if (target.equals("/login")) {
        serveFile(response,login, "text/html;charset=utf-8");
        baseRequest.setHandled(true);
        return;
    } else if (target.equals("/register")) {
        serveFile(response,register, "text/html;charset=utf-8");
        baseRequest.setHandled(true);
        return;
    }

    System.err.println("Creating session.");
    final Maybe<Stored<UserContext>> session = getSession(cookies, request);

    // Pages which require login

    try {
        Stored<UserContext> con = session.get();
        System.err.println("Logged in as user: " + con.value.user.value.name);
        // TODO: Set the correct flags on cookie.
        response.addCookie(new Cookie("session",con.identity.toString()));

        // Handle actions
        if (request.getMethod().equals("POST")) {
           if (request.getParameter("newforum") != null) {
               System.err.println("Crating a new forum.");
               Maybe<Stored<Forum>> forum = createForum((new Maybe<String> (request.getParameter("name"))).get(), con);
               response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
               response.setHeader("Location", "/forum/" + forum.get().value.handle);
               baseRequest.setHandled(true);
               return ;
           }
        }

        // Display pages
        if (target.equals("/")) {
           System.err.println("Displaying main page");
           response.setContentType("text/html;charset=utf-8");
           response.setStatus(HttpServletResponse.SC_OK);
           displayFrontPage(response.getWriter(), con);
           baseRequest.setHandled(true);
           return;
        } else if (target.startsWith("/forum/")) {
           handleForumObject(target.substring(("/forum/").length()),response,request,con);
           baseRequest.setHandled(true);
           return;
        } else if (target.equals("/newforum")) {
           response.setContentType("text/html;charset=utf-8");
           response.setStatus(HttpServletResponse.SC_OK);
           baseRequest.setHandled(true);
           displayNewforumForm(response.getWriter(), con);
           return;
        }
    } catch (Maybe.NothingException e) {
        // User was not logged in, redirect to login.
        System.err.println("User was not logged in, redirect to login.");
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", "/login");
        baseRequest.setHandled(true);
        return;
    }
  }

  private void handleForumObject(String object,
                                 HttpServletResponse response,
                                 HttpServletRequest request,
                                 Stored<UserContext> context) {
      Maybe<Either<Stored<Forum>,Stored<Thread>>>
             resolved = resolvePath(object, context.value);
      try {
         resolved.get().branch(
               forum -> { return ; // TODO: Display forum
                         },
               thread -> { try {
                             printThread(response.getWriter(), thread.value);
                           } catch (IOException e) { }} );
     } catch (Maybe.NothingException e) {
         // Object resolution failed
         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
     }
  }

  private Maybe<Stored<UserContext>> getSession(Map<String,Cookie> cookies, HttpServletRequest request) {
     final Maybe<Cookie> sessionCookie = new Maybe<Cookie>(cookies.get("session"));

     try {
         // The user is logging in
         if (request.getMethod().equals("POST") && request.getParameter("login") != null) {
             try {
               String username = (new Maybe<String> (request.getParameter("username"))).get();
               String password = (new Maybe<String> (request.getParameter("password"))).get();
               return login(username,password);
             } catch (Maybe.NothingException e) {
               // Not enough data suppied for login
               System.err.println("Broken usage of login");
             }
         } else if (request.getMethod().equals("POST") && request.getParameter("register") != null) {
             try {
              // Request for registering a new user
               String username = (new Maybe<String> (request.getParameter("username"))).get();
               String password = (new Maybe<String> (request.getParameter("password"))).get();
               String password_repeat = (new Maybe<String> (request.getParameter("password_repeat"))).get();
               // TODO: Validate username. Check that passwords are valid and match
               return registerUser(username,password);
             } catch (Maybe.NothingException e) {
               System.err.println("Broken usage of register");
             }
         }

        // Restore a previous session
        return restore(UUID.fromString(sessionCookie.get().getValue()));

     } catch (Maybe.NothingException e){
         // Not enough data to construct a proper user session.
         return Maybe.nothing();
     } catch (IllegalArgumentException e) {
         // UUID syntax error
         return Maybe.nothing();
     }
  }

  private Maybe<Stored<UserContext>> restore(UUID id) {
     try {
         return Maybe.just(contextStore.renew(id));
     } catch (DeletedException e){
         // Session token expired or otherwise deleted from storage
         System.err.println("Session token expried:" + id);
     } catch (SQLException e) {
         // Retrieving session from storage failed
         System.err.println(e);
     } 
    return Maybe.nothing();
  }

  private Maybe<Stored<UserContext>> login(String username, String password) {
     return userStore
         .getUser(username)
         .bind( user -> contextStore.getUserContext(user, password) );
  }

  private void printForumBlob(PrintWriter out, String prefix, Stored<Forum> forum) {
      out.println("<div class=\"blob\">\n"
                + "  <a href=\"" + prefix + "/" + forum.value.handle + "\"><h3 class=\"topic\">" + forum.value.name + "</h3></a>\n"
                + "</div>");
  }

  private void printStandardHead(PrintWriter out) {
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en-GB\">");
        out.println("<head>");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">");
        out.println("<style type=\"text/css\">code{white-space: pre;}</style>");
        out.println("<link rel=\"stylesheet\" href=\"/style.css\">");
  }

  private void displayFrontPage(PrintWriter out, Stored<UserContext> context) {
        printStandardHead(out);
        out.println("<title>Inforum – " + context.value.user.value.name + "</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<section class=\"forum\">");
        out.println("<header>");
        out.println("<h1 class=\"topic\">Inforum</h1>");
        out.println("<div>"
                  + "<a class=\"action\" href=\"/newforum\">New forum</a>"
                  + "<a class=\"action\" href=\"/logout\">Logout</a>"
                  + "</div>");
        out.println("</header>");
        context.value.forums.forEach(
          forum -> {
             printForumBlob(out, "/forum" , forum);
          }
        );
        out.println("</section>");
        out.println("</body>");
        out.println("</html>");
     // TODO
  }
  private void displayNewforumForm(PrintWriter out, Stored<UserContext> context) {
        printStandardHead(out);
        out.println("<title>Inforum – create a new forum</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<section class=\"form\">");
        out.println("<header>");
        out.println("<h1 class=\"topic\">New forum</h1>");
        out.println("</header>");
        out.println("<form class=\"login\" action=\"/\" method=\"POST\">"
                  + "<div class=\"name\"><input type=\"text\" name=\"name\" placeholder=\"Forum name\"></div>"
                  + "<div class=\"submit\"><input type=\"submit\" name=\"newforum\" value=\"Create forum\"></div>"
                  + "</forum>");
        out.println("</section>");
        out.println("</body>");
        out.println("</html>");
  }

  private Maybe<Stored<UserContext>> registerUser(String username, String password) {
     try {
        Stored<User> user = userStore.save(new User(username,"/img/user.svg",Instant.now()));
        return Maybe.just(contextStore.save(new UserContext(user)));
     } catch (SQLException e) {
         // Mostlikely the username is not unique
         System.err.println(e);
     }
     return Maybe.nothing();
  }

  private Maybe<Stored<Forum>> createForum(String name, Stored<UserContext> context) {
     try {
         String handle = name.toLowerCase();
         Stored<Forum> forum = forumStore.save(new Forum(handle, name));
         Util.updateSingle(context, contextStore,
            con -> con.value.addForum(forum));
         return Maybe.just(forum);
     } catch (SQLException e) {

     } catch (DeletedException e) {

     }
     return Maybe.nothing();
  }

  private static Map<String,Cookie> getCookies (HttpServletRequest request) {
    final Map<String,Cookie> cookies = new TreeMap<String,Cookie>();
    final Cookie[] carray = request.getCookies();
    if(carray != null) {
       for(int i = 0; i < carray.length; ++i) {
         cookies.put(carray[i].getName(),carray[i]);
       }
    }
    return cookies;
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
       final String starter = thread.messages.last.get().value.sender;
       final Instant date = thread.messages.last.get().value.date;
       w.println("  <div class=\"starter\">" + starter + "</div>");
       w.println("  <div class=\"date\">" + date.toString() + "</div>");
    } catch(Maybe.NothingException e) {
       w.println("  <p>This thread is empty.</p>");
    }
    w.println("  </header>");
    thread.messages.reverse().forEach (sm -> {
         final Message m = sm.value;
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
        // Shorter, if we can upgrade to JDK 1.9: is.transferTo(response.getOutputStream());
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
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
  }

  public static void main(String[] args) throws Exception
  {
    Stored<Message> testMessage 
      = new Stored<Message>(new Message("Joe",
                                        "<p> This a message. </p>",
                                        Instant.now()));
    Stored<Message> testMessage2
      = new Stored<Message>(new Message("Bob",
                                        "<p> This another message. </p>",
                                        Instant.now()));
    example_thread.accept(
        new Thread("A serious discussion!",
          ImmutableList.cons(testMessage2,ImmutableList.cons(testMessage,ImmutableList.empty()))));

    try(Connection connection = DriverManager.getConnection(dburl)){
       messageStore = new MessageStorage(connection);
       messageStore.initialise();
       userStore = new UserStorage(connection);
       userStore.initialise();
       threadStore = new ThreadStorage(messageStore, connection);
       threadStore.initialise();
       forumStore = new ForumStorage(threadStore, connection);
       forumStore.initialise();
       contextStore = new UserContextStorage(forumStore, userStore, connection);
       contextStore.initialise();
   
       Server server = new Server(8080);
       server.setHandler(new InforumServer());
   
       server.start();
       server.join();
    } catch (SQLException e) {
       System.err.println("Inforum failed: " + e);
    }
  }
}
