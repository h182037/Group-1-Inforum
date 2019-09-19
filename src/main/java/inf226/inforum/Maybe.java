package inf226.inforum;

public class Maybe<T> {
   private final T value;

   public Maybe(T value) {
      this.value = value;
   }

   public T get() throws NothingException {
      if(value == null)
         throw new NothingException();
      else
         return value;
   }

   public static<U> Maybe<U> nothing() {
      return new Maybe<U>(null);
   }

   public static class NothingException extends Exception {
	private static final long serialVersionUID = 8141663032597379968L;

     public NothingException() {
        super("Unexpected Maybe.nothing()");
     }
     @Override
     public Throwable fillInStackTrace() {
         return this;
     }
   }
}
