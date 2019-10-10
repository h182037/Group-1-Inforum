package inf226.inforum.storage;

import inf226.inforum.Message;
import inf226.inforum.ImmutableList;
import inf226.inforum.storage.*;

import java.io.Closeable;
import java.io.IOException;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

public class MessageStorage implements Storage<Message,String,SQLException>, Closeable {
   final Connection connection;

    public MessageStorage(String filename) throws SQLException {
        final String url = "jdbc:sqlite:" + filename;
      connection = DriverManager.getConnection(url);
    }

   @Override
   public void close() throws IOException {
      try {
         connection.close();
      } catch (SQLException e) {
         throw new IOException("SQL exception:" + e.getMessage());
      }
   }


   @Override
   public Stored<Message> save(Message value) throws SQLException {
     return null;
   }

   @Override
   public Stored<Message> update(Stored<Message> object, Message new_object) throws UpdatedException,DeletedException,SQLException {
     return null;
   }

   @Override
   public void delete(Stored<Message> object) throws UpdatedException,DeletedException,SQLException {
     return;
   }

   @Override
   public Stored<Message> renew(Stored<Message> object) throws DeletedException,SQLException{
     return null;
   }

   @Override
   public ImmutableList< Stored<Message> > lookup(String query) throws SQLException {
     return null;
   }
}