package inf226.inforum;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public class Thread {
   public final String topic;
   public final ImmutableList<Message> messages;

   public Thread(String topic, ImmutableList<Message> messages) {
      this.topic = topic;
      this.messages = messages;
   }

   public Thread(String topic) {
      this.topic = topic;
      this.messages = ImmutableList.empty();
   }

   public Thread addMessage(Message message) {
      return new Thread(topic, messages.add(message));
   }
}

