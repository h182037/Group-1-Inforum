package inf226.inforum.storage;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

import inf226.inforum.ImmutableList;
import inf226.inforum.Message;
import inf226.inforum.User;
import inf226.inforum.UserContext;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class MessageStorage implements Storage<Message,SQLException> {
   final Connection connection;

    public MessageStorage(Connection connection) throws SQLException {
      this.connection = connection;
    }


   public synchronized  void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS Message (id TEXT PRIMARY KEY, version TEXT, sender TEXT, message TEXT, date TEXT)");
   }

   @Override
   public Stored<Message> save(Message message) throws SQLException {
     final Stored<Message> stored = new Stored<Message>(message);

     PreparedStatement stmt = connection.prepareStatement("INSERT INTO Message VALUES(?,?,?,?,?)");
     stmt.setString(1,stored.identity.toString());
     stmt.setString(2,stored.version.toString());
     stmt.setString(3,message.sender);
     stmt.setString(4,message.message);
     stmt.setString(5,message.date.toString());
     stmt.executeUpdate();

     return stored;
   }

   @Override
   public synchronized Stored<Message> update(Stored<Message> message, Message new_message) throws UpdatedException,DeletedException,SQLException {
     final Stored<Message> current = renew(message.identity);
     final Stored<Message> updated = current.newVersion(new_message);
     if(current.version.equals(message.version)) {

         PreparedStatement stmt = connection.prepareStatement("UPDATE Message SET (version, sender, message, date) = (?,?,?,?) WHERE id= ?");
         stmt.setString(1,updated.version.toString());
         stmt.setString(2, new_message.sender);
         stmt.setString(3,new_message.message);
         stmt.setString(4,new_message.date.toString());
         stmt.setString(5, updated.identity.toString());

        stmt.executeUpdate();
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<Message> message) throws UpdatedException,DeletedException,SQLException {
        final Stored<Message> current = renew(message.identity);
     if(current.version.equals(message.version)) {
         PreparedStatement stmt = connection.prepareStatement("DELETE FROM Message WHERE id =?");
         stmt.setString(1,message.identity.toString());
         stmt.executeUpdate();

     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized Stored<Message> renew(UUID id) throws DeletedException,SQLException{


       PreparedStatement stmt = connection.prepareStatement("SELECT version,sender,message,date FROM Message WHERE id = ?");

       stmt.setString(1,id.toString());
       final ResultSet rs = stmt.executeQuery();

      if(rs.next()) {
          final UUID version = UUID.fromString(rs.getString("version"));
          final String sender = rs.getString("sender");
          final String message = rs.getString("message");
          final Instant date = Instant.parse(rs.getString("date"));
          return (new Stored<Message>(new Message(sender,message,date),id,version));
      } else {
          throw new DeletedException();
      }
   }

}
