package inf226.inforum;

import inf226.inforum.Maybe;

public final class ImmutableList<T> {
   private final Maybe<ListItem<T> > items;
   public final Maybe<T> last;
   
   private ImmutableList() {
      this.items = Maybe.nothing();
      this.last = Maybe.nothing();
   }
   private ImmutableList(T head, ImmutableList<T> tail) {
      this.items = new Maybe<ListItem<T>>(new ListItem<T>(head, tail));

      /* Construct a reference to the last element of
         the list. */
      T new_last;
      try {
         new_last = tail.last.get();
      } catch (Maybe.NothingException e) {
         new_last = head;
      }
      this.last = new Maybe<T>(new_last);
   }

   public static<U> ImmutableList<U> empty() {
      return new ImmutableList<U>();
   }
   public static<U> ImmutableList<U> cons(U head, ImmutableList<U> tail) {
      return new ImmutableList<U>(head,tail);
   }
   public Maybe<T> head() {
      try {
         return new Maybe<T>(items.get().head);
      } catch (Maybe.NothingException e) {
         return Maybe.nothing();
      }
   }

   public Maybe< ImmutableList<T> > tail() {
      try {
         return new Maybe<ImmutableList<T> >(items.get().tail);
      } catch (Maybe.NothingException e) {
         return Maybe.nothing();
      }
   }

   private static class ListItem<T> {
      public final T head;
      public final ImmutableList<T> tail;
      ListItem(T head, ImmutableList<T> tail) {
         this.head = head;
         this.tail = tail;
      }
   }
}
