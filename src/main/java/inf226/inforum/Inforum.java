package inf226.inforum;


import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.lang.IllegalArgumentException;

import java.io.Closeable;
import java.io.IOException;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

import java.time.Instant;
import inf226.inforum.storage.*;
import inf226.inforum.storage.DeletedException;


/**
 *
 * 
 *
 **/

public class Inforum implements Closeable
{

  private final MessageStorage messageStore;
  private final UserStorage userStore;
  private final ThreadStorage threadStore;
  private final ForumStorage forumStore;
  private final UserContextStorage contextStore;
  private final Connection connection;

  public Inforum(String path) throws SQLException {
       final String dburl = "jdbc:sqlite:" + path;
       connection = DriverManager.getConnection(dburl);
       connection.createStatement().executeUpdate("PRAGMA foreign_keys = ON");
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
  }

  @Override
  public void close() throws IOException{
     try {
       connection.close();
     } catch (SQLException e) {
       throw new IOException(e.toString());
     }
  }


  public Maybe<Stored<UserContext>> login(String username, String password) {
     return userStore
         .getUser(username)
         .bind( user -> contextStore.getUserContext(user, password) );
  }


  public Maybe<Stored<UserContext>> registerUser(String username, String password) {
     try {
        Stored<User> user = userStore.save(new User(username,"/img/user.svg",Instant.now()));
        return Maybe.just(contextStore.save(new UserContext(user)));
     } catch (SQLException e) {
         // Mostlikely the username is not unique
         System.err.println(e);
     }
     return Maybe.nothing();
  }

  public Maybe<Stored<Forum>> createForum(String name, Stored<UserContext> context) {
     try {
         String handle = name.toLowerCase();
         Stored<Forum> forum = forumStore.save(new Forum(handle, name));
         Util.updateSingle(context, contextStore,
            con -> con.value.addForum(forum));
         return Maybe.just(forum);
     } catch (SQLException e) {
         System.err.println(e);
     } catch (DeletedException e) {
         System.err.println(e);
     }
     return Maybe.nothing();
  }

  public Maybe<Stored<UserContext>> restore(UUID id) {
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

  public Maybe<Stored<Message>> postMessage(Stored<Thread> thread, String message, Stored<UserContext> context) {
     try {
        Stored<Message> m = messageStore.save(new Message(context.value.user.value.name, message, Instant.now()));
        Util.updateSingle(thread, threadStore,
          t -> t.value.addMessage(m));
        return Maybe.just(m);
     } catch (Exception e) {
        System.err.println(e);
        return Maybe.nothing();
     }
  }

  public Maybe<Stored<Thread>> createThread(Stored<Forum> forum, Thread thread) {
     try {
        Stored<Thread> t = threadStore.save(thread);
        Util.updateSingle(forum,forumStore,
          f -> f.value.addThread(t));
        return Maybe.just(t);
     } catch (Exception e) {
        return Maybe.nothing();
     }
  }

  public Maybe<Stored<Forum>> refreshForum(Stored<Forum> forum) {
     try {
        return Maybe.just(forumStore.renew(forum.identity));
     } catch (Exception e) {
        System.err.println(e);
        return Maybe.nothing();
     }
  }
  
  public Maybe<Stored<Thread>> refreshThread(Stored<Thread> thread) {
     try {
        return Maybe.just(threadStore.renew(thread.identity));
     } catch (Exception e) {
        System.err.println(e);
        return Maybe.nothing();
     }
  }

  public void deleteMessage(UUID message, Stored<UserContext> context) {
     try {
        messageStore.delete(messageStore.renew(message));
     } catch (Exception e) {
        System.err.println(e);
     }
  }
}