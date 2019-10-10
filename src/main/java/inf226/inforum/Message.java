package inf226.inforum;

import java.time.Instant;


public class Message {
   public final String sender; // User name of sender
   public final String message;
   public final Instant date; // Date of posting.


   public Message(String sender, String message, Instant date) {
      this.sender = sender;
      this.message = message;
      this.date = date;
   }

   // Copy constructor
   public Message(Message m) {
      this.sender = m.sender;
      this.message = m.message;
      this.date = m.date;
   }

   public String toHTML() {
      // TODO: Prevent XSS through message body.
      return message;
   }
}

