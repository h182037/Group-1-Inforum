package inf226.inforum;
import java.util.function.Consumer;

public class Mutable<A> implements Consumer<A>{
   private A value;

   public Mutable(A value) {
     this.value = value;
   }

   @Override
   public void accept(A value) {
      this.value = value;
   }

   public A get() {
      return value;
   }
}

