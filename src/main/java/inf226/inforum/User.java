package inf226.inforum;

import inf226.inforum.storage.DeletedException;
import inf226.inforum.storage.UserStorage;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;

public class User {


    public void setName(String name) {
        this.name = name;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public void setJoined(Instant joined) {
        this.joined = joined;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String name;
     public String imageURL;
   public Instant joined;
    public String password;


   public User(String name, String imageURL,Instant joined, String password ) {
     this.name = name;
     this.imageURL = imageURL;
     this.joined = joined;
       this.password = password;
   }
    public User() {
       this.password ="";
       this.name = "";
       this.joined = Instant.now();
       this.imageURL = "";
    }

    public String getName() {
        return name;
    }

    public String getImageURL() {
        return imageURL;
    }

    public Instant getJoined() {
        return joined;
    }

    public String getPassword() {
        return password;
    }

    public boolean checkPassword(String password) throws DeletedException, UnsupportedEncodingException, GeneralSecurityException {
        boolean check = false;


        try {
            final String dburl = "jdbc:sqlite:" + "production.db";
           Connection connection = DriverManager.getConnection(dburl);
            connection.createStatement().executeUpdate("PRAGMA foreign_keys = ON");

            UserStorage userStore = new UserStorage(connection);
            userStore.initialise();


            check = userStore.checkPasswordWithDB(password, getName());


        } catch (SQLException e) {
            System.err.println("Check password"+ e);


   }  return check;
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