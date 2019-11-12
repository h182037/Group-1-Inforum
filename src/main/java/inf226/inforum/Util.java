package inf226.inforum;
import inf226.inforum.Maybe;
import java.lang.Throwable;
import inf226.inforum.storage.Stored;
import inf226.inforum.storage.Storage;
import inf226.inforum.storage.DeletedException;
import inf226.inforum.storage.UpdatedException;

import java.util.function.Function;


public class Util {
   public static<E extends Throwable> void throwMaybe(Maybe<E> exception) throws E {
       try { throw exception.get(); }
       catch (Maybe.NothingException e) { /* Intensionally left blank */ }
   }

    public static boolean checkString(String s) {
        boolean valid = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (('a' <= c && c <= 'z')
                    || ('A' <= c && c <= 'Z')
                    || ('0' <= c && c <= '9')) {
                valid = true;
            } else {
                valid = false;
            }
        }
        return valid;
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



    public static<A,Q, E extends Exception> Stored<A> updateSingle(Stored<A> stored, Storage<A,E> storage, Function<Stored<A>,A> update) throws E, DeletedException{
      boolean updated = true;
      while(true) {
        try {
          return storage.update(stored,update.apply(stored));
        } catch (UpdatedException e) {
          stored = (Stored<A>)e.newObject;
        }
      }
   }
}

