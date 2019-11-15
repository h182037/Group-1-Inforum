package inf226.inforum.storage;

import inf226.inforum.*;

import java.sql.*;
import java.util.UUID;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 *
 * Every SQL  injection has been secured, with preferred statements
 */

public class UserContextStorage implements Storage<UserContext,SQLException> {
   final Connection connection;
   final Storage<Forum,SQLException> forumStore;
   final Storage<User,SQLException> userStore;


    public UserContextStorage(Storage<Forum,SQLException> forumStore, 
                              Storage<User,SQLException> userStore,
                              Connection connection) throws SQLException {
      this.forumStore = forumStore;
      this.userStore = userStore;
      this.connection = connection;
    }


   public synchronized void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS UserContext (id TEXT PRIMARY KEY, version TEXT, user TEXT, FOREIGN KEY(user) REFERENCES User(id) ON DELETE CASCADE)");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS UserContextForum (context TEXT, forum TEXT, ordinal INTEGER, PRIMARY KEY(context, forum), FOREIGN KEY(forum) REFERENCES Forum(id) ON DELETE CASCADE, FOREIGN KEY(context) REFERENCES UserContext(id) ON DELETE CASCADE)");
   }
    // Task 1, Preferred statement made
    @Override
    public synchronized Stored<UserContext> renew(UUID id) throws DeletedException,SQLException {


        PreparedStatement contextStmt = connection.prepareStatement("SELECT version, user FROM UserContext WHERE id = ?");
        contextStmt.setString(1,id.toString());

        PreparedStatement forumStmt = connection.prepareStatement("SELECT forum,ordinal FROM UserContextForum WHERE context = ? ORDER BY ordinal DESC");
        forumStmt.setString(1, id.toString());

        final ResultSet contextResult = contextStmt.executeQuery();
        final ResultSet forumResult = forumStmt.executeQuery();


        if(contextResult.next()) {
            final UUID version = UUID.fromString(contextResult.getString("version"));
            final Stored<User> user = userStore.renew(UUID.fromString(contextResult.getString("user")));
            // Get all the forums in this context
            final ImmutableList.Builder<Stored<Forum>> forums = ImmutableList.builder();
            while(forumResult.next()) {
                final UUID forumId = UUID.fromString(forumResult.getString("forum"));
                forums.accept(forumStore.renew(forumId));
            }
            return (new Stored<UserContext>(new UserContext(user,forums.getList()),id,version));
        } else {
            throw new DeletedException();
        }
    }

    // Task 1, Preferred statement made
   @Override
   public synchronized Stored<UserContext> save(UserContext context) throws SQLException {
     final Stored<UserContext> stored = new Stored<UserContext>(context);
     final String sql =  "INSERT INTO UserContext VALUES('" + stored.identity + "','"
                                                       + stored.version  + "','"
                                                       + context.user.identity  + "')";

     PreparedStatement stmt = connection.prepareStatement("INSERT INTO UserContext VALUES(?,?,?)");
     stmt.setString(1, stored.identity.toString());
     stmt.setString(2,stored.version.toString());
     stmt.setString(3,context.user.identity.toString());

    stmt.executeUpdate();


     final Maybe.Builder<SQLException> exception = Maybe.builder();
     final Mutable<Integer> ordinal = new Mutable<Integer>(0);
     context.forums.forEach(forum -> {
        try {

            PreparedStatement msql = connection.prepareStatement("INSERT INTO UserContextForum VALUES(?,?,?)");
            msql.setString(1, stored.identity.toString());
            msql.setString(2,forum.identity.toString());
            msql.setString(3,ordinal.get().toString());

            msql.executeUpdate();
        }
        catch (SQLException e) { exception.accept(e) ; }
        ordinal.accept(ordinal.get() + 1);
      });

     Util.throwMaybe(exception.getMaybe());
         
     return stored;
   }
    // Task 1, Preferred statement made
   @Override
   public synchronized Stored<UserContext> update(Stored<UserContext> context, UserContext new_context) throws UpdatedException,DeletedException,SQLException {
     final Stored<UserContext> current = renew(context.identity);
     final Stored<UserContext> updated = current.newVersion(new_context);
     if(current.version.equals(context.version)) {

         PreparedStatement stmt = connection.prepareStatement("UPDATE UserContext SET (version,user) = (?,?) WHERE id=?");
         stmt.setString(1,updated.version.toString());
         stmt.setString(2,new_context.user.identity.toString());
         stmt.setString(3,updated.identity.toString());
         stmt.executeUpdate();

         PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM UserContextForum WHERE context=?");
         stmt2.setString(1,context.identity.toString());

         stmt2.executeUpdate();
        
        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<Integer>(0);
        new_context.forums.forEach(forum -> {

           try {
               PreparedStatement msql = connection.prepareStatement("INSERT INTO UserContextForum VALUES(?,?,?)");
               msql.setString(1,updated.identity.toString());
               msql.setString(2,forum.identity.toString());
               msql.setString(3,ordinal.get().toString());
               msql.executeUpdate();
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
    // Task 1, Preferred statement made
   @Override
   public synchronized void delete(Stored<UserContext> context) throws UpdatedException,DeletedException,SQLException {
     final Stored<UserContext> current = renew(context.identity);
     if(current.version.equals(context.version)) {


         PreparedStatement stmt = connection.prepareStatement("DELETE FROM UserContextForum WHERE context=?");
         stmt.setString(1,context.identity.toString());
         stmt.executeUpdate();

         PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM UserContext WHERE id =?");
         stmt2.setString(1,context.identity.toString());
         stmt2.executeUpdate();

     } else {
        throw new UpdatedException(current);
     }
   }
    // Task 1, Preferred statement made
   public synchronized Maybe<Stored<UserContext>> getUserContext(Stored<User> user, String password) {
      try {
      if (user.value.checkPassword(password)) {


          PreparedStatement stmt = connection.prepareStatement("SELECT id FROM UserContext WHERE user = ?");
          stmt.setString(1,user.identity.toString());
          final ResultSet contextResult = stmt.executeQuery();

         if(contextResult.next()) {
            final UUID id = UUID.fromString(contextResult.getString("id"));
            return Maybe.just(renew(id));
            
         }
      }
      } catch (Exception e) {
         // Intensionally left blank to avoid info leakage from login.
      }
      return Maybe.nothing();
   }

   /**
    *  Invite another user to the forum.
    *  // Task 1, Preferred statement made
    */
   public synchronized boolean invite(Stored<Forum> forum, Stored<User> user) {
     try {


      PreparedStatement stmt = connection.prepareStatement("SELECT id FROM UserContext WHERE user =?");
      stmt.setString(1,user.identity.toString());


      ResultSet rs = stmt.executeQuery();
      if(rs.next()) {

          PreparedStatement stmt2 = connection.prepareStatement("INSERT INTO UserContextForum VALUES(?,?,?)");
          stmt2.setString(1, rs.getString("id"));
          stmt2.setString(2, forum.identity.toString());
          stmt2.setString(3, "(-1");
          stmt2.executeUpdate();

        return true;

      }
      System.out.println("No user in DB:" + "SQL");
    } catch (SQLException e) {System.err.println("UsercntextStorage" + e);}
    return false;
   }
}
