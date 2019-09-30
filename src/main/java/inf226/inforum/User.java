package inf226.inforum;

public class User {
   public final String name;

   public User(String name) {
     this.name = name;
   }

   public boolean checkPassword(String password) {
      // TODO: Implement proper authentication.
      return true;
   }
}