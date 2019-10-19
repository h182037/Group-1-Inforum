package inf226.inforum.storage;

import inf226.inforum.Message;
import inf226.inforum.ImmutableList;
import inf226.inforum.storage.*;

import java.io.IOException;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import java.util.UUID;
import java.time.Instant;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class MessageStorage implements Storage<Message,String,SQLException> {
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
     String sql =  "INSERT INTO Message VALUES('" + stored.identity + "','"
                                                 + stored.version  + "','"
                                                 + message.sender  + "','"
                                                 + message.message + "','"
                                                 + message.date.toString() + "')";
     connection.createStatement().executeUpdate(sql);
     return stored;
   }

   @Override
   public synchronized Stored<Message> update(Stored<Message> message, Message new_message) throws UpdatedException,DeletedException,SQLException {
     final Stored<Message> current = renew(message.identity);
     final Stored<Message> updated = current.newVersion(new_message);
     if(current.version.equals(message.version)) {
        String sql =  "REPLACE INTO Message VALUES('" + updated.identity + "','"
                                                     + updated.version  + "','"
                                                     + new_message.sender  + "','"
                                                     + new_message.message + "','"
                                                     + new_message.date.toString() + "')";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<Message> message) throws UpdatedException,DeletedException,SQLException {
     final Stored<Message> current = renew(message.identity);
     if(current.version.equals(message.version)) {
        String sql =  "DELETE FROM Message WHERE id ='" + message.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized Stored<Message> renew(UUID id) throws DeletedException,SQLException{
      final String sql = "SELECT version,sender,message,date FROM Message WHERE id = '" + id.toString() + "'";
      final Statement statement = connection.createStatement();
      final ResultSet rs = statement.executeQuery(sql);

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

   @Override
   public synchronized ImmutableList< Stored<Message> > lookup(String query) throws SQLException {
     return null;
   }
}
