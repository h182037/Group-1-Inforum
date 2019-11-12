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
  
        return true;
    }


   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final User user_other = (User) other;
    boolean equal = true;
    if(name == null) {
       equal = equal && user_other.name == null;
    } else {
       equal = equal && name.equals(user_other.name);
    }
    if(imageURL == null) {
       equal = equal && user_other.imageURL == null;
    } else {
       equal = equal && imageURL.equals(user_other.imageURL);
    }
    if(joined == null) {
       equal = equal && user_other.joined == null;
    } else {
       equal = equal && joined.equals(user_other.joined);
    }
    return equal;
   }
}