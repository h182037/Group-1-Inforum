package inf226.inforum;


import java.io.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;

import com.lambdaworks.crypto.SCryptUtil;

import inf226.inforum.storage.Stored;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;



/**
 * The InforumServer class handles all HTTP and HTML component.
 * Functions called display⋯ and print⋯ output HTML.
 * Functions called handle⋯ process HTTP requests.
 */

public class InforumServer extends AbstractHandler
{
  // Static resources:
  private final File style = new File("style.css");
  private final File login = new File("login.html");
  private final File register = new File("register.html");
  private static Inforum inforum;

  /**
   * This is the entry point for HTTP requests.
   * Some requests require login, while some can be processed
   * without a valid session.
   */
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response)
    throws IOException, ServletException
  {
    final Map<String,Cookie> cookies = getCookies(request);



    // Pages which do not require login
      // TASK 2 TASK 3 TASK ω adding some header options to further protect response/request handling
      response.addHeader("X-FRAME-OPTIONS", "DENY");
      response.addHeader("X-XSS-Protection", "1");
      response.addHeader("mode", "block");
      response.addHeader("X-Content-Type-Options", "nosniff");
      response.addHeader("Content-Security-Policy", "default-src 'self'");
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
        //TASK 2 Set HttpOnly flag to true
        Cookie cookie = new Cookie("session",con.identity.toString());
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // Handle actions
        //TASK 3 checking csrf token to allow newforum POST,we solved this with a rather gullible approach using the session cookie as csrf token
        if (request.getMethod().equals("POST")) {
           if (request.getParameter("newforum") != null
                   && request.getParameter("token").equals(con.identity.toString())) {
               System.err.println("Crating a new forum.");
               Maybe<Stored<Forum>> forum 
                  = inforum.createForum((new Maybe<String> (request.getParameter("name"))).get(), con);
               response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
               response.setHeader("Location", "/forum/" + forum.get().value.handle + "/");
               baseRequest.setHandled(true);
               return ;
           }
           if (request.getParameter("edit") != null) {
               displayEditMessageForm(response.getWriter(),target,(new Maybe<String> (request.getParameter("content"))).get(), (Maybe.just(request.getParameter("message"))).get());
               response.setStatus(HttpServletResponse.SC_OK);
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
        } else if (target.equals("/logout")) {
           response.addCookie(new Cookie("session",""));
           response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
           response.setHeader("Location", "/");
           baseRequest.setHandled(true);
           return ;
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

  /**
   * Display or modify threads or forums ("forum objects")
   */
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
                           } catch (IOException | Maybe.NothingException e) { }  // TODO: Display forum
                         },
               thread -> { try {
                             handleThread(response.getWriter(),object,request,response, thread, context);
                           } catch (IOException e) { }} );
     } catch (Maybe.NothingException e) {
         // Object resolution failed
         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
     }
  }

  /**
   * Restore a session, login or register a user.
   *
   *
   */
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
               // Not enough data supplied for login
               System.err.println("Broken usage of login");
             }
         } else if (request.getMethod().equals("POST") && request.getParameter("register") != null) {
             try {
              // Request for registering a new user
               String username = (new Maybe<String> (request.getParameter("username"))).get();
               String password = (new Maybe<String> (request.getParameter("password"))).get();
               String password_repeat = (new Maybe<String> (request.getParameter("password_repeat"))).get();
               // TODO: Validate username. Check that passwords are valid and match
                // TASK 0 Password and username constraints
                 if(Util.checkString(username) && Util.checkPassword(password) && password.equals(password_repeat)){
                     return inforum.registerUser(username,password);
                 }

             } catch (Maybe.NothingException e) {
               System.err.println("Broken usage of register");
             } catch (UnsupportedEncodingException e) {
                 e.printStackTrace();
             } catch (GeneralSecurityException e) {
                 e.printStackTrace();
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


  /**
   * Display forums as small blobs in HTML
   */
  private void printForumBlob(PrintWriter out, String prefix, Stored<Forum> forum) {
      final StringBuilder topiclist = new StringBuilder();
      try {
          ImmutableList<Stored<Thread>> tlist = forum.value.threads;
          Stored<Thread> thread = tlist.head().get();
          String handle = forum.value.handle;
          topiclist.append("<li><a href=\"" + prefix + "/" + handle + "/" + thread.identity +"\">" +
                  thread.value.topic + "</a></li>\n");
          tlist = tlist.tail().get();
          thread = tlist.head().get();
          topiclist.append("<li><a href=\"" + prefix + "/" + handle + "/" + thread.identity +"\">" +
                  thread.value.topic + "</a></li>\n");
          tlist = tlist.tail().get();
          thread = tlist.head().get();
          topiclist.append("<li><a href=\"" + prefix + "/" + handle + "/" + thread.identity +"\">" +
                  thread.value.topic + "</a></li>\n");
          tlist = tlist.tail().get();
          thread = tlist.head().get();
          topiclist.append("<li><a href=\"" + prefix + "/" + handle + "/" + thread.identity +"\">" +
                  thread.value.topic + "</a></li>\n");
      } catch (Maybe.NothingException e) { }
      //TASK 2 Escaping HTML
      out.println("<div class=\"blob\">\n"
                + "  <a href=\"" + prefix + "/" + StringEscapeUtils.escapeHtml4(forum.value.handle) + "/\"><h3 class=\"topic\">" + StringEscapeUtils.escapeHtml4(forum.value.name) + "</h3></a>\n"
                + "  <div class=\"text\"><ul>" + topiclist.toString() + "</ul></div>"
                + "</div>");
  }

  /**
   * Display threads as small blobs in HTML
   */
  private void printThreadBlob(PrintWriter out, String prefix, Stored<Thread> thread) {
      Maybe<Stored<Message>> first = thread.value.messages.head();
      Maybe<String> blabla = first.map(message -> message.value.message);
      //TASK 2 Escaping HTML
      out.println("<div class=\"blob\">\n"
                + "  <a href=\"" + prefix + thread.identity + "\"><h3 class=\"topic\">" +
              StringEscapeUtils.escapeHtml4(thread.value.topic) + "</h3></a>\n"
                + "  <div class=\"text\">" + blabla.defaultValue("No messages.") + "</div>"
                + "</div>");
  }

  private void printStandardHead(PrintWriter out) {
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en-GB\">");
        out.println("<head>");
        //Only allow content from this application.
        out.println("<meta name=\"viewport\" width=device-width initial-scale=1.0 user-scalable=yes\">");
        out.println("<style type=\"text/css\">code{white-space: pre;}</style>");
        out.println("<link rel=\"stylesheet\" href=\"/style.css\">");
  }

  /**
   * Render the front page as HTML
   */
  private void displayFrontPage(PrintWriter out, Stored<UserContext> context) {
        printStandardHead(out);
      //TASK 2 Escaping HTML
      out.println("<title>Inforum – " +
                StringEscapeUtils.escapeHtml4(context.value.user.value.name) + "</title>");
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
  }

  private void displayEditMessageForm(PrintWriter out, String ret, String content, String message) {
        printStandardHead(out);
        out.println("<title>Inforum – Edit message</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<section class=\"form\">");
        out.println("<header>");
        out.println("<h1 class=\"topic\">Edit message</h1>");
        out.println("</header>");
        out.println("<form class=\"login\" action=\"" + ret + "\" method=\"POST\">"
                  + "<input type=\"hidden\" name=\"message\" value=\"" + message + "\">"
                  + "<textarea name=\"content\" placeholder=\"Message\" cols=50 rows=10>"+ content +"</textarea>"
                  + "<div class=\"submit\"><input type=\"submit\" name=\"editmessage\" value=\"Update\"></div>"
                  + "</form>");
        out.println("</section>");
        out.println("</body>");
        out.println("</html>");
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
                + "<input type=\"hidden\" name=\"token\" value=\"" + context.identity.toString() + "\">"
                + "<div class=\"submit\"><input type=\"submit\" name=\"newforum\" value=\"Create forum\"></div>"
                  + "</form>");
        out.println("</section>");
        out.println("</body>");
        out.println("</html>");
  }
  private void displayNewthreadForm(PrintWriter out, String forum, Stored<UserContext> context) {
        printStandardHead(out);;
        out.println("<title>Inforum – create a new thread</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<section class=\"form\">");
        out.println("<header>");
        out.println("<h1 class=\"topic\">New thread</h1>");
        out.println("</header>");
      //TASK 2 Escaping HTML
        out.println("<form class=\"login\" action=\"/forum/" + StringEscapeUtils.escapeHtml4(forum) + "\" method=\"POST\">"
                  + "<input type=\"hidden\" name=\"forum\" value=\"" + StringEscapeUtils.escapeHtml4(forum) + "\">"
                  + "<div class=\"name\"><input type=\"text\" name=\"topic\" placeholder=\"Topic\"></div>"
                  + "<textarea name=\"message\" placeholder=\"Message\" cols=50 rows=10></textarea>"
                + "<input type=\"hidden\" name=\"token\" value=\"" + context.identity.toString() + "\">"
                  + "<div class=\"submit\"><input type=\"submit\" name=\"newthread\" value=\"Post\"></div>"
                  + "</form>");
        out.println("</section>");
        out.println("</body>");
        out.println("</html>");
  }



  /**
   * Load all the cookies into a map for easy retrieval.
   */
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

  /**
   * Resolve a path to a forum or subforum.
   */
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

  /**
   * Handle an HTTP request to a forum
   */
  private void handleForum(PrintWriter out,
                     String p,
                     HttpServletRequest request,
                     HttpServletResponse response,
                     Stored<Forum> forum,
                     Stored<UserContext> context) throws Maybe.NothingException {
      //TASK 2 Escaping HTML
      String path = StringEscapeUtils.escapeHtml4(p);
       if (request.getMethod().equals("POST") && request.getParameter("newthread") != null
               && request.getParameter("token").equals(context.identity.toString())) {
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
       } else if (request.getMethod().equals("POST") && request.getParameter("invite") != null) {
          try {
               System.err.println("Inviting into forum");
               String user = (new Maybe<String> (request.getParameter("user"))).get();
               if(inforum.invite(context, user, forum)) {
                  System.err.println("Invited!");
               }
               forum = inforum.refreshForum(forum).get();
          } catch (Maybe.NothingException e) {
               System.err.println("Could not invite.");
          }
       }
       printForum(out,path,forum);
  }

  /**
   * Handle an HTTP request to a thread
   */
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
               System.err.println("Could not post message");
          }
       } else if (request.getMethod().equals("POST") && request.getParameter("deletemessage") != null) {
          try {
               System.err.println("Deleting a message.");
               UUID message = UUID.fromString((new Maybe<String> (request.getParameter("message"))).get());
               inforum.deleteMessage(message,context);
               System.err.println("Message deleted.");
               thread = inforum.refreshThread(thread).get();
          } catch (Maybe.NothingException e) {
               System.err.println("Could not delete message.");
          }

       } else if (request.getMethod().equals("POST") && request.getParameter("editmessage") != null) {
          try {
               System.err.println("Editing a new message.");
               UUID message = UUID.fromString((new Maybe<String> (request.getParameter("message"))).get());
               String content = (new Maybe<String> (request.getParameter("content"))).get();
               inforum.editMessage(message,content,context);
               System.err.println("Message deleted.");
               thread = inforum.refreshThread(thread).get();
          } catch (Maybe.NothingException e) {
               System.err.println("Could not create new thread.");
          }

       }
       printThread(out,path,thread,context);
  }

  private void printForum(final PrintWriter out, final String p, final Stored<Forum> forum) {
    printStandardHead(out);
      //TASK 2 Escaping HTML
    String name = StringEscapeUtils.escapeHtml4(forum.value.name);
    String path = StringEscapeUtils.escapeHtml4(p);
    out.println("<title>Inforum – " + name + "</title>");
    out.println("</head>");
    out.println("<body>");
    out.println("<section>");
    out.println("  <header class=\"forum-head\">");
    out.println("  <h2 class=\"topic\">" + name + "</h2>");
    out.println("  </header>");
    out.println("<form class=\"action\" name=\"invite\" action=\"/forum/" + path + "\" method=\"POST\">"
                  + "<a class=\"action\" href=\"/newthread?forum=" + path + "\">New thread</a>"
                  + "<input style=\"margin-left: 3em;\" type=\"text\" name=\"user\" placeholder=\"Invite user\"><input type=\"submit\" name=\"invite\" value=\"Invite!\">"
                  + "<a style=\"margin-left: 3em;\" class=\"action\" href=\"/logout\">Logout</a>"
              + "</form>");
    out.println("  <section class=\"forum\">");
    out.println("  <header><h3>Subforums</h3></header>");
    forum.value.subforums.forEach(
            //TASK 2 Escaping HTML
          subforum -> {
             printForumBlob(out, "/forum/" + StringEscapeUtils.escapeHtml4(path), subforum);
          }
        );
    out.println("  </section>");
    out.println("  <section class=\"forum\">");
    out.println("  <header><h3>Threads</h3></header>");
    forum.value.threads.forEach(
            //TASK 2 Escaping HTML
        thread -> {printThreadBlob(out, "/forum/" + StringEscapeUtils.escapeHtml4(path), thread);});
    out.println("  </section>");
    out.println("</section>");
    out.println("</body>");
    out.println("</html>");
    
  }

  private void printThread(PrintWriter out, String path, Stored<Thread> stored, Stored<UserContext> context){
    // TODO: Prevent XSS
      try {
          Thread thread = stored.value;
          printStandardHead(out);
          //TASK 2 Escaping HTML
          String topic = StringEscapeUtils.escapeHtml4(thread.topic);
          out.println("<title>Inforum – " + topic + "</title>");
          out.println("</head>");
          out.println("<body>");
          out.println("<section>");
          out.println("  <header class=\"thread-head\">");
          out.println("  <h2 class=\"topic\">" + topic + "</h2>");
          //TASK 2 Escaping HTML
          final String starter = StringEscapeUtils.escapeHtml4(thread.messages.last.get().value.sender);
          final Instant date = thread.messages.last.get().value.date;
          out.println("  <div class=\"starter\">" + starter + "</div>");
          out.println("  <div class=\"date\">" + date.toString() + "</div>");
          out.println("  </header>");
          thread.messages.reverse().forEach (sm -> {
                  final Message m = sm.value;
                  out.println("  <div class=\"entry\">");
              //TASK 2 Escaping HTML
                  out.println("     <div class=\"user\">" + StringEscapeUtils.escapeHtml4(m.sender) + "</div>");
                  out.println("     <div class=\"text\">" + m.message + "</div>");
              //TASK 2 Escaping HTML
                  out.println("     <form class=\"controls\" action=\"/forum/" + StringEscapeUtils.escapeHtml4(path) +"\" method=\"POST\">");
                  out.println("        <input class=\"controls\" name=\"message\" type=\"hidden\" value=\""+ sm.identity +"\">");
                  out.println("        <input class=\"controls\" name=\"content\" type=\"hidden\" value=\""+ m.message +"\">");
                  out.println("        <input type=\"submit\" name=\"edit\" value=\"Edit\"/>\n");
                  out.println("        <input type=\"submit\" name=\"deletemessage\" value=\"Delete\"/>\n");
                  out.println("     </form>");
                  out.println("  </div>");
          });
          //TASK 2 Escaping HTML
          out.println("<form class=\"entry\" action=\"/forum/" + StringEscapeUtils.escapeHtml4(path) + "\" method=\"post\">");
          out.println("  <div class=\"user\">" + StringEscapeUtils.escapeHtml4(context.value.user.value.name) + "</div>");
          out.println("  <textarea class=\"messagebox\" placeholder=\"Post a message in this thread.\" name=\"message\"></textarea>");
          out.println("  <div class=\"controls\"><input style=\"float: right;\" type=\"submit\" name=\"newmessage\" value=\"Send\"></div>");
          out.println("</form>");
          out.println("</section>");
          out.println("</body>");
          out.println("</html>");
      } catch (Maybe.NothingException e) {
          e.printStackTrace();
      }
  }

  /**
   * Serve a static file from file system.
   */
  private void serveFile(HttpServletResponse response, File file, String contentType) {
      response.setContentType(contentType);
      try {
        final InputStream is = new FileInputStream(file);
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

  /**
   * main function. Sets up the forum.
   */
  public static void main(String[] args) throws Exception
  {
    try{
       inforum = new Inforum("production.db");






       Server server = new Server(8083);

       server.setHandler(new InforumServer());

   
       server.start();
       server.join();
    } catch (SQLException e) {
       System.err.println("Inforum failed: " + e);
    }
    inforum.close();
  }

}
