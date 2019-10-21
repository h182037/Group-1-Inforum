package inf226.inforum.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import inf226.inforum.ImmutableList;
import inf226.inforum.Maybe;
import inf226.inforum.Message;
import inf226.inforum.Mutable;
import inf226.inforum.Thread;
import inf226.inforum.Util;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class ThreadStorage implements Storage<Thread,String,SQLException> {
   final Connection connection;
   final Storage<Message,String,SQLException> messageStore;

    public ThreadStorage(Storage<Message,String,SQLException> messageStore, Connection connection) throws SQLException {
      this.messageStore = messageStore;
      
      this.connection = connection; 
    }


   public synchronized void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS Thread (id TEXT PRIMARY KEY, version TEXT, topic TEXT)");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS ThreadMessage (thread TEXT, message TEXT, ordinal INTEGER, PRIMARY KEY(thread, message), FOREIGN KEY(message) REFERENCES Message(id), FOREIGN KEY(thread) REFERENCES Thread(id))");
   }

   @Override
   public synchronized Stored<Thread> renew(UUID id) throws DeletedException,SQLException {
      final String threadsql = "SELECT version,topic FROM Thread WHERE id = '" + id.toString() + "'";
      final String messagesql = "SELECT message,ordinal FROM ThreadMessage WHERE thread = '" + id.toString() + "' ORDER BY ordinal DESC";

      final Statement threadStatement = connection.createStatement();
      final Statement messageStatement = connection.createStatement();

      final ResultSet threadResult = threadStatement.executeQuery(threadsql);
      final ResultSet messageResult = messageStatement.executeQuery(messagesql);

      if(threadResult.next()) {
          final UUID version = UUID.fromString(threadResult.getString("version"));
          final String topic = threadResult.getString("topic");
          // Get all the messages in this thread
          final ImmutableList.Builder<Stored<Message>> messages = ImmutableList.builder();
          while(messageResult.next()) {
              final UUID messageId = UUID.fromString(messageResult.getString("message"));
              messages.accept(messageStore.renew(messageId));
          }
          return (new Stored<Thread>(new Thread(topic,messages.getList()),id,version));
      } else {
          throw new DeletedException();
      }
   }

   @Override
   public synchronized Stored<Thread> save(Thread thread) throws SQLException {
     final Stored<Thread> stored = new Stored<Thread>(thread);
     final String sql =  "INSERT INTO Thread VALUES('" + stored.identity + "','"
                                                       + stored.version  + "','"
                                                       + thread.topic  + "')";
     connection.createStatement().executeUpdate(sql);
     final Maybe.Builder<SQLException> exception = Maybe.builder();
     final Mutable<Integer> ordinal = new Mutable<Integer>(0);
     thread.messages.forEach(message -> {
        final String msql = "INSERT INTO ThreadMessage VALUES('" + stored.identity + "','"
                                                                 + message.identity + "','"
                                                                 + ordinal.get().toString() + "')";
        try { connection.createStatement().executeUpdate(msql); }
        catch (SQLException e) { exception.accept(e) ; }
        ordinal.accept(ordinal.get() + 1);
      });

     Util.throwMaybe(exception.getMaybe());
         
     return stored;
   }

   @Override
   public synchronized Stored<Thread> update(Stored<Thread> thread, Thread new_thread) throws UpdatedException,DeletedException,SQLException {
     final Stored<Thread> current = renew(thread.identity);
     final Stored<Thread> updated = current.newVersion(new_thread);
     if(current.version.equals(thread.version)) {
        String sql =  "REPLACE INTO Thread VALUES('" + updated.identity + "','"
                                                     + updated.version  + "','"
                                                     + new_thread.topic + "')";
        connection.createStatement().executeUpdate(sql);
        connection.createStatement().executeUpdate("DELETE FROM ThreadMessage WHERE thread='" + thread.identity + "'");
        
        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<Integer>(0);
        new_thread.messages.forEach(message -> {
           final String msql = "INSERT INTO ThreadMessage VALUES('" + updated.identity + "','"
                                                                    + message.identity + "','"
                                                                    + ordinal.get().toString() + "')";
           try {connection.createStatement().executeUpdate(msql);}
           catch (SQLException e) { exception.accept(e);}
           ordinal.accept(ordinal.get() + 1);
         });
        Util.throwMaybe(exception.getMaybe());
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<Thread> thread) throws UpdatedException,DeletedException,SQLException {
     final Stored<Thread> current = renew(thread.identity);
     if(current.version.equals(thread.version)) {
        connection.createStatement().executeUpdate("DELETE FROM ThreadMessage WHERE thread='" + thread.identity + "'");
        String sql =  "DELETE FROM Thread WHERE id ='" + thread.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized ImmutableList< Stored<Thread> > lookup(String query) throws SQLException {
     return null;
   }
}
