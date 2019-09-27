package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class UserContext {
   public final ImmutableList<Stored<Thread>> threads;
   public UserContext(ImmutableList<Stored<Thread>> threads) {
      this.threads = threads;
   }
}

