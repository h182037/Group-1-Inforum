package inf226.inforum.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

import inf226.inforum.ImmutableList;
import inf226.inforum.Maybe;
import inf226.inforum.User;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class UserStorage implements Storage<User,SQLException> {
   final Connection connection;

    public UserStorage(Connection connection) throws SQLException {
      this.connection = connection;
    }


   public synchronized  void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS User (id TEXT PRIMARY KEY, version TEXT, name TEXT, imageURL TEXT, joined TEXT, UNIQUE(name))");
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
        String sql =  "UPDATE User SET (version,name,imageURL,joined) =('" 
                                                     + updated.version  + "','"
                                                     + new_user.name  + "','"
                                                     + new_user.imageURL + "','"
                                                     + new_user.joined.toString() + "') WHERE id='"+ updated.identity + "'";
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

   public synchronized Maybe<Stored<User>> getUser(String name) {
      try {
         final String sql = "SELECT id FROM User WHERE name = '" + name + "'";
         final Statement statement = connection.createStatement();
         final ResultSet rs = statement.executeQuery(sql);
         if(rs.next()) {
          final UUID id = UUID.fromString(rs.getString("id"));
          return Maybe.just(renew(id));
         }
      } catch (SQLException e) {
         System.out.println(e);
         // Intensionally left blank
      } catch (DeletedException e) {
         System.out.println(e);
         // Intensionally left blank
      }
      return Maybe.nothing();

   }
}
