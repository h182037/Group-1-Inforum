package inf226.inforum;

import com.lambdaworks.crypto.SCryptUtil;
import inf226.inforum.storage.DeletedException;
import inf226.inforum.storage.UserStorage;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public class User {
   public final String name;
   public final String imageURL;
   public final Instant joined;
   public final String password;


   public User(String name, String password, String imageURL, Instant joined) {
     this.name = name;
     this.imageURL = imageURL;
     this.joined = joined;
     this.password = password;
   }



    public boolean checkPassword(String password) throws DeletedException {
        String hashed = SCryptUtil.scrypt(password,16384,8,1);
        boolean check = true;
        try {
            UserStorage us;
            us = new UserStorage();

            check = us.checkPasswordWithDB(hashed);
            return check;

        } catch (SQLException e) {
            System.err.println("Check password" + e);
        }

        return check;


    }
    public boolean checkName(String name) throws SQLException, DeletedException {
        UserStorage us;
        us = new UserStorage();
        boolean check = us.getUserAuth(name);

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
       if(password == null) {
           equal = equal && user_other.password == null;
       } else {
           equal = equal && password.equals(user_other.password);
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