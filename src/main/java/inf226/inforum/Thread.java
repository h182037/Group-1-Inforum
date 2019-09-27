package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class Thread {
   public final String topic;
   public final ImmutableList<Stored<Message>> messages;

   public Thread(String topic, ImmutableList<Stored<Message>> messages) {
      this.topic = topic;
      this.messages = messages;
   }
      
}

