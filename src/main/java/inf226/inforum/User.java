package inf226.inforum;

public class User {
   public final String name;
   public final String password;

   public User(String name, String password) {
     this.name = name;
     this.password = password;
   }

   public boolean checkPassword(String password) {
      // TODO: Implement proper authentication.
      return this.password.equals(password);
   }
}