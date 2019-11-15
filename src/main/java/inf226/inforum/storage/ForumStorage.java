package inf226.inforum.storage;

import inf226.inforum.Thread;
import inf226.inforum.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * TODO: Secure the following for SQL injection vulnerabilities.
 */

public class ForumStorage implements Storage<Forum,SQLException> {
   final Connection connection;
   final Storage<Thread,SQLException> threadStore;

    public ForumStorage(Storage<Thread,SQLException> threadStore, Connection connection) throws SQLException {
      this.threadStore = threadStore;

      this.connection = connection;
    }


   public synchronized void initialise() throws SQLException {
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS Forum (id TEXT PRIMARY KEY, version TEXT, handle TEXT, name TEXT, UNIQUE (handle))");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS ForumThread (forum TEXT, thread TEXT, ordinal INTEGER, PRIMARY KEY(forum, thread), FOREIGN KEY(thread) REFERENCES Thread(id) ON DELETE CASCADE, FOREIGN KEY(forum) REFERENCES Forum(id) ON DELETE CASCADE)");
       connection.createStatement()
                 .executeUpdate("CREATE TABLE IF NOT EXISTS SubForum (forum TEXT, subforum TEXT, ordinal INTEGER, PRIMARY KEY(forum, subforum), FOREIGN KEY(subforum) REFERENCES Forum(id) ON DELETE CASCADE, FOREIGN KEY(forum) REFERENCES Forum(id) ON DELETE CASCADE)");
   }

   @Override
   public synchronized Stored<Forum> renew(UUID id) throws DeletedException,SQLException {

      //final String threadsql = "SELECT thread,ordinal FROM ForumThread WHERE forum = '" + id.toString() + "' ORDER BY ordinal DESC";
      //final String subforumsql = "SELECT subforum,ordinal FROM SubForum WHERE forum = '" + id.toString() + "' ORDER BY ordinal DESC";




       PreparedStatement stmt = connection.prepareStatement("SELECT version,handle,name FROM Forum WHERE id = ? ");
       stmt.setString(1,id.toString());

       PreparedStatement stmt2 = connection.prepareStatement("SELECT thread,ordinal FROM ForumThread WHERE forum = ? ORDER BY ordinal DESC");
       stmt2.setString(1,id.toString());

       PreparedStatement stmt3 = connection.prepareStatement("SELECT subforum,ordinal FROM SubForum WHERE forum = ? ORDER BY ordinal DESC");
       stmt3.setString(1,id.toString());



        final ResultSet forumResult = stmt.executeQuery();

      if(forumResult.next()) {
          final UUID version = UUID.fromString(forumResult.getString("version"));
          final String handle = forumResult.getString("handle");
          final String name = forumResult.getString("name");

          final ResultSet threadResult = stmt2.executeQuery();
          // Get all the threads in this forum
          final ImmutableList.Builder<Stored<Thread>> threads = ImmutableList.builder();
          while(threadResult.next()) {
              final UUID threadId = UUID.fromString(threadResult.getString("thread"));
              threads.accept(threadStore.renew(threadId));
          }
          final ResultSet subforumResult = stmt3.executeQuery();
          // Get all the subforums in this forum
          final ImmutableList.Builder<Stored<Forum>> subforums = ImmutableList.builder();
          while(subforumResult.next()) {
              final UUID subforumId = UUID.fromString(subforumResult.getString("subforum"));
              subforums.accept(this.renew(subforumId));
          }
          return (new Stored<Forum>(new Forum(handle,name,threads.getList(), subforums.getList()),id,version));
      } else {
          throw new DeletedException();
      }
   }

   @Override
   public synchronized Stored<Forum> save(Forum forum) throws SQLException {
     final Stored<Forum> stored = new Stored<Forum>(forum);

       PreparedStatement stmt = connection.prepareStatement("INSERT INTO Forum VALUES(?,?,?,?)");
       stmt.setString(1, stored.identity.toString());
       stmt.setString(2,stored.version.toString());
       stmt.setString(3,forum.handle);
       stmt.setString(4,forum.name);
       stmt.executeUpdate();

     final Maybe.Builder<SQLException> exception = Maybe.builder();

     // Store the threads in the forum
     final Mutable<Integer> ordinal = new Mutable<Integer>(0);
     forum.threads.forEach(thread -> {

        try {
            PreparedStatement stmt2 = connection.prepareStatement("INSERT INTO ForumThread VALUES(?,?,?)");
            stmt2.setString(1,stored.identity.toString());
            stmt2.setString(2,thread.identity.toString());
            stmt2.setString(3,ordinal.get().toString());
            stmt2.executeUpdate();
        }
        catch (SQLException e) { exception.accept(e) ; }
        ordinal.accept(ordinal.get() + 1);
      });


     // Store the subforum in the forum
     ordinal.accept(0);
     forum.subforums.forEach(subforum -> {
        final String msql = "INSERT INTO Subforum VALUES('" + stored.identity + "','"
                                                            + subforum.identity + "','"
                                                            + ordinal.get().toString() + "')";
        try { connection.createStatement().executeUpdate(msql);
            PreparedStatement stmt3 = connection.prepareStatement("INSERT INTO Subforum VALUES(?,?,?)");
            stmt3.setString(1,stored.identity.toString());
            stmt3.setString(2,subforum.identity.toString());
            stmt3.setString(3,ordinal.get().toString());
            stmt3.executeUpdate();
        }
        catch (SQLException e) { exception.accept(e) ; }
        ordinal.accept(ordinal.get() + 1);
      });

     Util.throwMaybe(exception.getMaybe());

     return stored;
   }

   @Override
   public synchronized Stored<Forum> update(Stored<Forum> forum, Forum new_forum) throws UpdatedException,DeletedException,SQLException {
     final Stored<Forum> current = renew(forum.identity);
     final Stored<Forum> updated = current.newVersion(new_forum);
     if(current.version.equals(forum.version)) {

        PreparedStatement stmt = connection.prepareStatement("UPDATE Forum SET (version,handle,name) = (?,?,?) WHERE id=?");
        stmt.setString(1,updated.version.toString());
        stmt.setString(2,new_forum.handle);
        stmt.setString(3,new_forum.name);
        stmt.setString(4,updated.identity.toString());
        stmt.executeUpdate();

        PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM ForumThread WHERE forum=?");
        stmt2.setString(1,forum.identity.toString());
        stmt2.executeUpdate();

        final Maybe.Builder<SQLException> exception = Maybe.builder();
        final Mutable<Integer> ordinal = new Mutable<Integer>(0);
        new_forum.threads.forEach(thread -> {

           try {
               PreparedStatement stmt3 = connection.prepareStatement("INSERT INTO ForumThread VALUES(?,?,?)");
               stmt3.setString(1,updated.identity.toString());
               stmt3.setString(2,thread.identity.toString());
               stmt3.setString(3,ordinal.get().toString());
               stmt3.executeUpdate();
           }
           catch (SQLException e) { exception.accept(e);}
           ordinal.accept(ordinal.get() + 1);
         });

        ordinal.accept(0);
        new_forum.subforums.forEach(subforum -> {

           try {
               PreparedStatement stmt4 = connection.prepareStatement("INSERT INTO Subforum VALUES(?,?,?)");
               stmt4.setString(1,updated.identity.toString());
               stmt4.setString(2,subforum.identity.toString());
               stmt4.setString(3,ordinal.get().toString());
               stmt4.executeUpdate();
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
   public synchronized void delete(Stored<Forum> forum) throws UpdatedException,DeletedException,SQLException {
     final Stored<Forum> current = renew(forum.identity);
     if(current.version.equals(forum.version)) {

         PreparedStatement stmt = connection.prepareStatement("DELETE FROM ForumThread WHERE forum=?");
         stmt.setString(1,forum.identity.toString());
         stmt.executeUpdate();


         PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM Subforum WHERE forum=?");
         stmt2.setString(1,forum.identity.toString());
         stmt2.executeUpdate();


         PreparedStatement stmt3 = connection.prepareStatement("DELETE FROM Forum WHERE id =?");
         stmt3.setString(1,forum.identity.toString());
         stmt3.executeUpdate();
     } else {
        throw new UpdatedException(current);
     }
   }

}
