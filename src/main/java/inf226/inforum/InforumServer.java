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

  private static Inforum inforum;

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
               Maybe<Stored<Forum>> forum 
                  = inforum.createForum((new Maybe<String> (request.getParameter("name"))).get(), con);
               response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
               response.setHeader("Location", "/forum/" + forum.get().value.handle + "/");
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
           response.setContentType("text/html;charset=utf-8");
           handleForumObject(target.substring(("/forum/").length()),response,request,con);
           baseRequest.setHandled(true);
           return;
        } else if (target.equals("/newforum")) {
           response.setContentType("text/html;charset=utf-8");
           response.setStatus(HttpServletResponse.SC_OK);
           baseRequest.setHandled(true);
           displayNewforumForm(response.getWriter(), con);
           return;
        } else if (target.equals("/newthread")) {
           try {
              String forum = (new Maybe<String> (request.getParameter("forum"))).get();
              response.setContentType("text/html;charset=utf-8");
              response.setStatus(HttpServletResponse.SC_OK);
              baseRequest.setHandled(true);
              displayNewthreadForm(response.getWriter(), forum, con);
              return;
           } catch (Maybe.NothingException e) {
           }
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
      System.err.println("Resolving path:" + object);
      Maybe<Either<Stored<Forum>,Stored<Thread>>>
             resolved = resolvePath(object, context.value);
      try {
         resolved.get().branch(
               forum -> { try {
                             System.err.println("Printing forum.");
                             handleForum(response.getWriter(),object,request,response,forum, context);
                           } catch (IOException e) { }  // TODO: Display forum
                         },
               thread -> { try {
                             handleThread(response.getWriter(),object,request,response, thread, context);
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
               return inforum.login(username,password);
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
               return inforum.registerUser(username,password);
             } catch (Maybe.NothingException e) {
               System.err.println("Broken usage of register");
             }
         }

        // Restore a previous session
        return inforum.restore(UUID.fromString(sessionCookie.get().getValue()));

     } catch (Maybe.NothingException e){
         // Not enough data to construct a proper user session.
         return Maybe.nothing();
     } catch (IllegalArgumentException e) {
         // UUID syntax error
         return Maybe.nothing();
     }
  }



  private void printForumBlob(PrintWriter out, String prefix, Stored<Forum> forum) {
      out.println("<div class=\"blob\">\n"
                + "  <a href=\"" + prefix + "/" + forum.value.handle + "/\"><h3 class=\"topic\">" + forum.value.name + "</h3></a>\n"
                + "</div>");
  }

  private void printThreadBlob(PrintWriter out, String prefix, Stored<Thread> thread) {
      Maybe<Stored<Message>> first = thread.value.messages.head();
      Maybe<String> blabla = first.map(message -> message.value.message);
      out.println("<div class=\"blob\">\n"
                + "  <a href=\"" + prefix + thread.identity + "\"><h3 class=\"topic\">" + thread.value.topic + "</h3></a>\n"
                + "  <div class=\"text\">" + blabla.defaultValue("No messages.") + "</div>"
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
  private void displayNewthreadForm(PrintWriter out, String forum, Stored<UserContext> context) {
        printStandardHead(out);
        out.println("<title>Inforum – create a new thread</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<section class=\"form\">");
        out.println("<header>");
        out.println("<h1 class=\"topic\">New thread</h1>");
        out.println("</header>");
        out.println("<form class=\"login\" action=\"/forum/" + forum + "\" method=\"POST\">"
                  + "<input type=\"hidden\" name=\"forum\" value=\"" + forum + "\">"
                  + "<div class=\"name\"><input type=\"text\" name=\"topic\" placeholder=\"Topic\"></div>"
                  + "<textarea name=\"message\" placeholder=\"Message\" cols=50 rows=10></textarea>"
                  + "<div class=\"submit\"><input type=\"submit\" name=\"newthread\" value=\"Post\"></div>"
                  + "</forum>");
        out.println("</section>");
        out.println("</body>");
        out.println("</html>");
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

  private void handleForum(PrintWriter out,
                     String path,
                     HttpServletRequest request,
                     HttpServletResponse response,
                     Stored<Forum> forum,
                     Stored<UserContext> context) {
       if (request.getMethod().equals("POST") && request.getParameter("newthread") != null) {
          try {
               System.err.println("Crating a new thread.");
               String topic = (new Maybe<String> (request.getParameter("topic"))).get();
               String message = (new Maybe<String> (request.getParameter("message"))).get();
               Maybe<Stored<Thread>> thread 
                  = inforum.createThread(forum,new Thread(topic));
               inforum.postMessage(thread.get(),message,context);
               forum = inforum.refreshForum(forum).get();
          } catch (Maybe.NothingException e) {
               System.err.println("Could not create new thread.");
          }
       }
       printForum(out,path,forum);

  }

  private void handleThread(PrintWriter out,
                     String path,
                     HttpServletRequest request,
                     HttpServletResponse response,
                     Stored<Thread> thread,
                     Stored<UserContext> context) {
       if (request.getMethod().equals("POST") && request.getParameter("newmessage") != null) {
          try {
               System.err.println("Creating a new message.");
               String message = (new Maybe<String> (request.getParameter("message"))).get();
               inforum.postMessage(thread,message,context);
               thread = inforum.refreshThread(thread).get();
          } catch (Maybe.NothingException e) {
               System.err.println("Could not create new thread.");
          }
       } else if (request.getMethod().equals("POST") && request.getParameter("deletemessage") != null) {
          try {
               System.err.println("Deleting a new message.");
               UUID message = UUID.fromString((new Maybe<String> (request.getParameter("message"))).get());
               inforum.deleteMessage(message,context);
               System.err.println("Message deleted.");
               thread = inforum.refreshThread(thread).get();
          } catch (Maybe.NothingException e) {
               System.err.println("Could not create new thread.");
          }

       }
       printThread(out,path,thread,context);

  

  }

  private void printForum(final PrintWriter out, final String path, final Stored<Forum> forum) {
    printStandardHead(out);
    out.println("<title>Inforum – " + forum.value.name + "</title>");
    out.println("</head>");
    out.println("<body>");
    out.println("<section>");
    out.println("  <header class=\"forum-head\">");
    out.println("  <h2 class=\"topic\">" + forum.value.name + "</h2>");
    out.println("  </header>");
    out.println("<div>"
                  + "<a class=\"action\" href=\"/newthread?forum=" + path + "\">New thread</a>"
                  + "<a class=\"action\" href=\"/logout\">Logout</a>"
              + "</div>");
    out.println("  <section class=\"forum\">");
    out.println("  <header><h3>Subforums</h3></header>");
    forum.value.subforums.forEach(
          subforum -> {
             printForumBlob(out, "/forum/" + path, subforum);
          }
        );
    out.println("  </section>");
    out.println("  <section class=\"forum\">");
    out.println("  <header><h3>Threads</h3></header>");
    forum.value.threads.forEach(
        thread -> {printThreadBlob(out, "/forum/" + path, thread);});
    out.println("  </section>");
    out.println("</section>");
    out.println("</body>");
    out.println("</html>");
    
  }

  private void printThread(PrintWriter out, String path, Stored<Thread> stored, Stored<UserContext> context) {
    // TODO: Prevent XSS
    Thread thread = stored.value;
    printStandardHead(out);
    out.println("<title>Inforum – " + thread.topic + "</title>");
    out.println("</head>");
    out.println("<body>");
    out.println("<section>");
    out.println("  <header class=\"thread-head\">");
    out.println("  <h2 class=\"topic\">" + thread.topic + "</h2>");
    try {
       final String starter = thread.messages.last.get().value.sender;
       final Instant date = thread.messages.last.get().value.date;
       out.println("  <div class=\"starter\">" + starter + "</div>");
       out.println("  <div class=\"date\">" + date.toString() + "</div>");
    } catch(Maybe.NothingException e) {
       out.println("  <p>This thread is empty.</p>");
    }
    out.println("  </header>");
    thread.messages.reverse().forEach (sm -> {
         final Message m = sm.value;
         out.println("  <div class=\"entry\">");
         out.println("     <div class=\"user\">" + m.sender + "</div>");
         out.println("     <div class=\"text\">" + m.message + "</div>");
         out.println("     <form class=\"controls\" action=\"/forum/" + path +"\" method=\"POST\">");
         out.println("        <input class=\"controls\" name=\"message\" type=\"hidden\" value=\""+ sm.identity +"\">");
         out.println("        <input type=\"submit\" name=\"edit\" value=\"Edit\"/>\n");
         out.println("        <input type=\"submit\" name=\"deletemessage\" value=\"Delete\"/>\n");
         out.println("     </form>");
         out.println("  </div>");
     });
    out.println("<form class=\"entry\" action=\"/forum/" + path + "\" method=\"post\">");
    out.println("  <div class=\"user\">" + context.value.user.value.name + "</div>");
    out.println("  <textarea class=\"messagebox\" placeholder=\"Post a message in this thread.\" name=\"message\"></textarea>");
    out.println("  <div class=\"controls\"><input style=\"float: right;\" type=\"submit\" name=\"newmessage\" value=\"Send\"></div>");
    out.println("</forum>");
    out.println("</section>");
    out.println("</body>");
    out.println("</html>");
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

    try{
       inforum = new Inforum("test.db");
       Server server = new Server(8080);
       server.setHandler(new InforumServer());
   
       server.start();
       server.join();
    } catch (SQLException e) {
       System.err.println("Inforum failed: " + e);
    }
    inforum.close();
  }
}
