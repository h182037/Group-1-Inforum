package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class UserContext {
   public final ImmutableList<Stored<Forum>> forums;
   public final String username;
   public UserContext(ImmutableList<Stored<Forum>> forums, String username) {
      this.forums = forums;
      this.username = username;
   }
   public UserContext(String username) {
      this.forums = ImmutableList.empty();
      this.username = username;
   }
}

