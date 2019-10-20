package inf226.inforum;

import java.time.Instant;

public class User {
   public final String name;
   public final String imageURL;
   public final Instant joined;

   public User(String name, String imageURL, Instant joined) {
     this.name = name;
     this.imageURL = imageURL;
     this.joined = joined;
   }

   public boolean checkPassword(String password) {
      // TODO: Implement proper authentication.
      return true;
   }
}