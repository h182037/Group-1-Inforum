package inf226.inforum;
import inf226.inforum.Maybe;
import java.lang.Throwable;

import inf226.inforum.storage.*;

import java.sql.Connection;
import java.util.function.Function;


public class Util{

   public static<E extends Throwable> void throwMaybe(Maybe<E> exception) throws E {
       try { throw exception.get(); }
       catch (Maybe.NothingException e) { /* Intensionally left blank */ }
   }

    public static boolean checkString(String s) {
        boolean valid = true;

        return valid;
    }


    public static boolean checkPassword(String password) {


        return true;
    }



    public static String escapeString(String s){
       String escaped = s;
       escaped = escaped.replaceAll("&","&amp");
       escaped = escaped.replaceAll("<","&lt");
       escaped = escaped.replaceAll(">","&gt");
       escaped = escaped.replaceAll("\"","&quot");
       escaped = escaped.replaceAll("'","&#x27");
       escaped = escaped.replaceAll("/","&#x2F");
       return escaped;
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

