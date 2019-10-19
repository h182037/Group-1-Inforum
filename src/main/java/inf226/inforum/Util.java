package inf226.inforum;
import inf226.inforum.Maybe;
import java.lang.Throwable;


public class Util {
   public static<E extends Throwable> void throwMaybe(Maybe<E> exception) throws E {
       try { throw exception.get(); }
       catch (Maybe.NothingException e) { /* Intensionally left blank */ }
   }
}

