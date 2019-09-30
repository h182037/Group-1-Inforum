package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class UserContext {
   public final ImmutableList<Stored<Forum>> forums;
   public UserContext(ImmutableList<Stored<Forum>> forums) {
      this.forums = forums;
   }
}

