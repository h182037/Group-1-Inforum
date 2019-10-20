package inf226.inforum.storage;

import inf226.inforum.User;
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

public class UserStorage implements Storage<User,String,SQLException> {
   final Connection connection;

    public UserStorage(Connection connection) throws SQLException {
      this.connection = connection;
    }


   public synchronized  void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS User (id TEXT PRIMARY KEY, version TEXT, name TEXT, user TEXT, joined TEXT)");
   }

   @Override
   public Stored<User> save(User user) throws SQLException {
     final Stored<User> stored = new Stored<User>(user);
     String sql =  "INSERT INTO User VALUES('" + stored.identity + "','"
                                                 + stored.version  + "','"
                                                 + user.name  + "','"
                                                 + user.imageURL + "','"
                                                 + user.joined.toString() + "')";
     connection.createStatement().executeUpdate(sql);
     return stored;
   }

   @Override
   public synchronized Stored<User> update(Stored<User> user, User new_user) throws UpdatedException,DeletedException,SQLException {
     final Stored<User> current = renew(user.identity);
     final Stored<User> updated = current.newVersion(new_user);
     if(current.version.equals(user.version)) {
        String sql =  "REPLACE INTO User VALUES('" + updated.identity + "','"
                                                     + updated.version  + "','"
                                                     + new_user.name  + "','"
                                                     + new_user.imageURL + "','"
                                                     + new_user.joined.toString() + "')";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
     return updated;
   }

   @Override
   public synchronized void delete(Stored<User> user) throws UpdatedException,DeletedException,SQLException {
     final Stored<User> current = renew(user.identity);
     if(current.version.equals(user.version)) {
        String sql =  "DELETE FROM User WHERE id ='" + user.identity + "'";
        connection.createStatement().executeUpdate(sql);
     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized Stored<User> renew(UUID id) throws DeletedException,SQLException{
      final String sql = "SELECT version,name,imageURL,joined FROM User WHERE id = '" + id.toString() + "'";
      final Statement statement = connection.createStatement();
      final ResultSet rs = statement.executeQuery(sql);

      if(rs.next()) {
          final UUID version = UUID.fromString(rs.getString("version"));
          final String name = rs.getString("name");
          final String imageURL = rs.getString("imageURL");
          final Instant joined = Instant.parse(rs.getString("joined"));
          return (new Stored<User>(new User(name,imageURL,joined),id,version));
      } else {
          throw new DeletedException();
      }
   }

   @Override
   public synchronized ImmutableList< Stored<User> > lookup(String query) throws SQLException {
     return null;
   }
}
