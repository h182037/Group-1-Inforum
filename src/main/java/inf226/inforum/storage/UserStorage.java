package inf226.inforum.storage;

import com.lambdaworks.crypto.SCryptUtil;
import inf226.inforum.Maybe;
import inf226.inforum.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static inf226.inforum.Maybe.just;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 *
 * Every SQL  injection has been secured, with preferred statements
 */

public class UserStorage implements Storage<User,SQLException> {
    Connection connection;

    public UserStorage(Connection connection) throws SQLException {
        this.connection = connection;
    }

    public UserStorage() {

    }


    public synchronized void initialise() throws SQLException {
        connection.createStatement()
                .executeUpdate("CREATE TABLE IF NOT EXISTS User (id TEXT PRIMARY KEY, version TEXT, name TEXT, password TEXT, imageURL TEXT, joined TEXT, UNIQUE(name))");


    }


    // Task 1, Preferred statement made
    @Override
    public Stored<User> save(User user) throws SQLException {
        final Stored<User> stored = new Stored<User>(user);

        PreparedStatement stmt = connection.prepareStatement("INSERT INTO User VALUES(?,?,?,?,?,?)");
        stmt.setString(1, stored.identity.toString());
        stmt.setString(2, stored.version.toString());
        stmt.setString(3, user.name);
        stmt.setString(4, user.imageURL);
        stmt.setString(5, user.joined.toString());
        stmt.setString(6, user.password);
        stmt.executeUpdate();


        return stored;
    }

    // Task 1, Preferred statement made
    @Override
    public synchronized Stored<User> update(Stored<User> user, User new_user) throws UpdatedException, DeletedException, SQLException {
        final Stored<User> current = renew(user.identity);

        final Stored<User> updated = current.newVersion(new_user);
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE User SET(version,name,password, imageURL,joined) =(?,?,?,?,?) WHERE id=?");
            stmt.setString(1, updated.version.toString());
            stmt.setString(2, new_user.name);
            stmt.setString(3, new_user.password);
            stmt.setString(4, new_user.imageURL);
            stmt.setString(5, new_user.joined.toString());
            stmt.setString(6, updated.identity.toString());

            stmt.executeUpdate();


        } catch (SQLException e) {
            throw new UpdatedException(current);
        }

        return updated;
    }

    // Task 1, Preferred statement made
    @Override
    public synchronized void delete(Stored<User> user) throws UpdatedException, DeletedException, SQLException {
        final Stored<User> current = renew(user.identity);
        if (current.version.equals(user.version)) {

            PreparedStatement stmt = connection.prepareStatement("DELETE FROM User WHERE id = ?");
            stmt.setString(1, user.identity.toString());
            stmt.executeUpdate();
        } else {
            throw new UpdatedException(current);
        }
    }

    // Task 1, Preferred statement made
    @Override
    public synchronized Stored<User> renew(UUID id) throws DeletedException, SQLException {

        PreparedStatement stmt = connection.prepareStatement("SELECT version,name,imageURL,joined FROM User WHERE id = ?");
        stmt.setString(1, id.toString());
        final ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            final UUID version = UUID.fromString(rs.getString("version"));
            final String name = rs.getString("name");
            final String imageURL = rs.getString("imageURL");
            final Instant joined = Instant.parse(rs.getString("joined"));
            User u = new User();
            u.setImageURL(imageURL);
            u.setJoined(joined);
            u.setName(name);
            return (new Stored<User>(u, id, version));
        } else {
            throw new DeletedException();
        }
    }

    // Task 1, Preferred statement made
    public synchronized Maybe<Stored<User>> getUser(String name) {
        try {


            PreparedStatement stmt = connection.prepareStatement("SELECT id FROM User WHERE name = ?");
            stmt.setString(1, name);
            final ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                final UUID id = UUID.fromString(rs.getString("id"));
                return just(renew(id));
            }
        } catch (SQLException e) {
            System.out.println(e);
            // Intensionally left blank
        } catch (DeletedException e) {
            System.out.println("userstorage" + e);
            // Intensionally left blank
        }
        return Maybe.nothing();

    }


    //TASK 0, here we check the password input in login with the hashed password in the database with the correct user

    public synchronized boolean checkPasswordWithDB(String passwordInput, String username) throws SQLException, DeletedException {

        PreparedStatement stmt = connection.prepareStatement("SELECT password FROM User WHERE name = ?");
        stmt.setString(1, username);
        final ResultSet rs = stmt.executeQuery();
        //final String sql = "SELECT name, password FROM User WHERE password = ?";

        //final Statement statement = connection.createStatement();
        // final ResultSet rs = statement.executeQuery(sql);
        String password = null;
        String name = null;
        String user_id = null;

        if (rs.next()) {
            // final UUID id = UUID.fromString(rs.getString("password"));
            password = rs.getString("password");
            // name = rs.getString("name");
            if (SCryptUtil.check(passwordInput, password)) {
                connection.close();
                return true;

            }


        }
        connection.close();
        return false;
    }

}
