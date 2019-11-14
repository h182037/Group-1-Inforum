package inf226.inforum.storage;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;

import inf226.inforum.Maybe;
import inf226.inforum.User;

import static inf226.inforum.Maybe.just;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class UserStorage implements Storage<User,SQLException> {
   Connection connection;

    public UserStorage(Connection connection) throws SQLException {
      this.connection = connection;
    }

    public UserStorage() {

    }


    public synchronized  void initialise() throws SQLException {
      connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS User (id TEXT PRIMARY KEY, version TEXT, name TEXT, password TEXT, imageURL TEXT, joined TEXT, UNIQUE(name))");
         //  connection.createStatement().executeUpdate("ALTER TABLE User ALTER COLUMN password TEXT");


    }

   @Override
   public Stored<User> save(User user) throws SQLException {
     final Stored<User> stored = new Stored<User>(user);
      // connection.createStatement().executeUpdate("ALTER TABLE User ALTER COLUMN password TEXT");

    PreparedStatement stmt = connection.prepareStatement("INSERT INTO User VALUES(?,?,?,?,?,?)");
    stmt.setString(1, stored.identity.toString());
    stmt.setString(2, stored.version.toString());
    stmt.setString(3, user.name);
    stmt.setString(4, user.password);
    stmt.setString(5, user.imageURL);
    stmt.setString(6, user.joined.toString());

    stmt.executeUpdate();


     return stored;
   }

   @Override
   public synchronized Stored<User> update(Stored<User> user, User new_user) throws UpdatedException,DeletedException,SQLException {
     final Stored<User> current = renew(user.identity);

     final Stored<User> updated = current.newVersion(new_user);
     try {
         PreparedStatement stmt = connection.prepareStatement("UPDATE User SET(version,name,password, imageURL,joined) =(?,?,?,?,?) WHERE id=?");
         stmt.setString(1, updated.version.toString());
         stmt.setString(2, new_user.name);
         stmt.setString(3,new_user.password);
         stmt.setString(4,new_user.imageURL);
         stmt.setString(5, new_user.joined.toString());
         stmt.setString(6, updated.identity.toString());

         stmt.executeUpdate();


     } catch(SQLException e) {
         throw new UpdatedException(current);
     }

     return updated;
   }

   @Override
   public synchronized void delete(Stored<User> user) throws UpdatedException,DeletedException,SQLException {
     final Stored<User> current = renew(user.identity);
     if(current.version.equals(user.version)) {

         PreparedStatement stmt = connection.prepareStatement("DELETE FROM User WHERE id = ?");
         stmt.setString(1, user.identity.toString());
        stmt.executeUpdate();
     } else {
        throw new UpdatedException(current);
     }
   }

   @Override
   public synchronized Stored<User> renew(UUID id) throws DeletedException,SQLException{

      PreparedStatement stmt = connection.prepareStatement("SELECT version,name,imageURL,joined FROM User WHERE id = ?");
      stmt.setString(1, id.toString());
      final ResultSet rs = stmt.executeQuery();

      if(rs.next()) {
          final UUID version = UUID.fromString(rs.getString("version"));
          final String name = rs.getString("name");
          final String password = rs.getString("password");
          final String imageURL = rs.getString("imageURL");
          final Instant joined = Instant.parse(rs.getString("joined"));
          return (new Stored<User>(new User(name, password, imageURL, joined),id,version));
      } else {
          throw new DeletedException();
      }
   }

   public synchronized Maybe<Stored<User>> getUser(String name) {
      try {


         PreparedStatement stmt = connection.prepareStatement("SELECT id FROM User WHERE name = ?");
         stmt.setString(1,name);
         final ResultSet rs = stmt.executeQuery();

         if(rs.next()) {
          final UUID id = UUID.fromString(rs.getString("id"));
          return just(renew(id));
         }
      } catch (SQLException e) {
         System.out.println(e);
         // Intensionally left blank
      } catch (DeletedException e) {
         System.out.println("userstorage" +e);
         // Intensionally left blank
      }
      return Maybe.nothing();

   }

   public synchronized boolean checkPasswordWithDB(String hash) throws SQLException, DeletedException {
        final String sql = "SELECT password FROM User WHERE password ='" + hash + "'";
        final Statement statement = connection.createStatement();
        final ResultSet rs = statement.executeQuery(sql);
        String password = null;
        String name = null;
        if(rs.next()) {
            final UUID id = UUID.fromString(rs.getString("password"));
            name = rs.getString("name");

          password = renewPassword((id));
          if(hash.equals(password) && checkName(name)) {
                return true;
        }

       }
       return false;
   }

    public synchronized String renewPassword(UUID id) throws DeletedException,SQLException{

        PreparedStatement stmt = connection.prepareStatement("SELECT version,name,imageURL,joined, password FROM User WHERE id = ?");
        stmt.setString(1, id.toString());
        final ResultSet rs = stmt.executeQuery();

        if(rs.next()) {

            final String password = rs.getString("password");

            return password;
        } else {
            throw new DeletedException();
        }
    }

    public boolean getUserAuth(String name) {
        try {

            PreparedStatement stmt = connection.prepareStatement("SELECT id FROM User WHERE name = ?");
            stmt.setString(1,name);
            final ResultSet rs = stmt.executeQuery();

            if(rs.next()) {
                final String dbName = rs.getString("id");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("getuserAUTH" + e);
            // Intensionally left blank
        }
        return false;

    }
    public synchronized boolean checkName(String name) throws DeletedException,SQLException{

        PreparedStatement stmt = connection.prepareStatement("SELECT version,name,imageURL,joined, password FROM User WHERE name = ?");
        stmt.setString(1, name.toString());
        final ResultSet rs = stmt.executeQuery();
        String nameFound = null;
        if(rs.next()) {

            nameFound = rs.getString("name");
            if(nameFound != null) {
                return true;
            }

        } else {
            return false;
        }
    return false;
    }
}
