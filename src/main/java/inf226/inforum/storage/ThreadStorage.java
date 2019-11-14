package inf226.inforum.storage;

import java.sql.*;
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

public class ThreadStorage implements Storage<Thread,SQLException> {
   final Connection connection;
   final Storage<Message,SQLException> messageStore;

    public ThreadStorage(Storage<Message,SQLException> messageStore, Connection connection) throws SQLException {
      this.messageStore = messageStore;
      
      this.connection = connection; 
    }


   public synchronized void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS Thread (id TEXT PRIMARY KEY, version TEXT, topic TEXT)");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS ThreadMessage (thread TEXT, message TEXT, ordinal INTEGER, PRIMARY KEY(thread, message), FOREIGN KEY(message) REFERENCES Message(id) ON DELETE CASCADE, FOREIGN KEY(thread) REFERENCES Thread(id) ON DELETE CASCADE)");
   }

   @Override
   public synchronized Stored<Thread> renew(UUID id) throws DeletedException,SQLException {


      final Statement threadStatement = connection.createStatement();
      final Statement messageStatement = connection.createStatement();

      PreparedStatement stmt = connection.prepareStatement("SELECT version,topic FROM Thread WHERE id = ?");
      stmt.setString(1, id.toString());
      final ResultSet threadResult = stmt.executeQuery();

      PreparedStatement stmt2 = connection.prepareStatement("SELECT message,ordinal FROM ThreadMessage WHERE thread = ?");
      stmt2.setString(1,id.toString());
      final ResultSet messageResult = stmt2.executeQuery();





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

       PreparedStatement stmt = connection.prepareStatement("INSERT INTO Thread VALUES(?,?,?)");
       stmt.setString(1,stored.identity.toString());
       stmt.setString(2,stored.version.toString());
       stmt.setString(3,thread.topic);
       stmt.executeUpdate();

     final Maybe.Builder<SQLException> exception = Maybe.builder();
     final Mutable<Integer> ordinal = new Mutable<Integer>(0);
     thread.messages.forEach(message -> {

        try {
            PreparedStatement stmt2 = connection.prepareStatement("INSERT INTO ThreadMessage VALUES(?,?,?)");
            stmt.setString(1,stored.identity.toString());
            stmt.setString(2,message.identity.toString());
            stmt.setString(3,ordinal.get().toString());
            stmt.executeUpdate();

        }
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

         PreparedStatement stmt = connection.prepareStatement("UPDATE Thread SET (version,topic)=(?,?) WHERE id= ?");
         stmt.setString(1,updated.version.toString());
         stmt.setString(2,new_thread.topic);
         stmt.setString(3,updated.identity.toString());
         stmt.executeUpdate();


        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<Integer>(0);
        new_thread.messages.forEach(message -> {

           try {
               PreparedStatement stmt2 = connection.prepareStatement("INSERT INTO ThreadMessage VALUES(?,?,?)");

               stmt2.setString(1,updated.identity.toString());
               stmt2.setString(2,message.identity.toString());
               stmt2.setString(3,ordinal.get().toString());
               stmt2.executeUpdate();
           }
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

        PreparedStatement stmt = connection.prepareStatement("DELETE FROM ThreadMessage WHERE thread=?");
        stmt.setString(1,thread.identity.toString());
        stmt.executeUpdate();
         PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM Thread WHERE id =?");
         stmt2.setString(1,thread.identity.toString());
         stmt2.executeUpdate();

     } else {
        throw new UpdatedException(current);
     }
   }

}
