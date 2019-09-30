package inf226.inforum;

import java.util.function.Consumer;

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
   public static<U> Maybe<U> just(U value) {
      return new Maybe<U>(value);
   }


   @Override
   public final boolean equals(Object other) {
    if (other == null)
        return false;
    if (getClass() != other.getClass())
        return false;
    @SuppressWarnings("unchecked")
    final Maybe maybe_other = (Maybe) other;
    if(maybe_other.value == null && value == null)
       return true;
    if(value == null)
       return false;
    return value.equals(maybe_other.value);
   }

   public void forEach(Consumer<T> c) {
      if (value == null)
         return ;
      else
         c.accept(value);
   }

   public Maybe<T> supremum(Maybe<T> other) {
     if (value == null)
       return other;
     else
       return this;
   }

   public static<U> Builder<U> builder() { return new Builder<U>() ; }

   public static class Builder<U> implements Consumer<U> {
      private Maybe<U> value;
      public Builder() { value = nothing(); }
      @Override
      public void accept(U value) { (new Maybe<U>(value)).forEach(v -> this.value = new Maybe<U>(v)); }
      public Maybe<U> getMaybe(){ return value ;};
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
