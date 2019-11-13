package inf226.inforum;

import com.lambdaworks.crypto.SCryptUtil;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

public class User {
   public final String name;
    public final String password;
   public final String imageURL;
   public final Instant joined;



   public User(String name, String password, String imageURL, Instant joined) {
     this.name = name;
       this.password = password;
     this.imageURL = imageURL;
     this.joined = joined;

   }

    public static boolean checkPassword(String password) {
        boolean valid = true;
        if(password.length() < 8 || password.length() > 64) {
            valid = false;
        }else{
            for (int i = 0; i <password.length(); i++){
                char c = password.charAt(i);

                if(       ('a'  <= c && c <= 'z')
                        || ('A' <= c && c <= 'Z')
                        || ('0' <= c && c <= '9')){
                    valid=true;
                }else{
                    valid=false;
                }
            }
        }

        return valid;
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