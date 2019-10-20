package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class UserContext {
   public final Stored<User> user;
   public final ImmutableList<Stored<Forum>> forums;

   public UserContext(Stored<User> user, ImmutableList<Stored<Forum>> forums) {
      this.forums = forums;
      this.user = user;
   }
   public UserContext(Stored<User> user) {
      this.forums = ImmutableList.empty();
      this.user = user;
   }
}

