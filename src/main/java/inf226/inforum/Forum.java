package inf226.inforum;

import inf226.inforum.storage.Stored;

public class Forum {
   public final String handle;
   public final String name;
   public final ImmutableList<Stored<Thread>> threads;
   public final ImmutableList<Stored<Forum>> subforums;

   public Forum(String handle, String name, ImmutableList<Stored<Thread>> threads, ImmutableList<Stored<Forum>> subforums) {
      // TODO: Verify that handle is URL safe.
      this.handle = handle;
      this.name = name;
      this.threads = threads;
      this.subforums = subforums;
   }

   public Forum(String handle, String name) {
      this.handle = handle;
      this.name = name;
      this.threads = ImmutableList.empty();
      this.subforums = ImmutableList.empty();
   }

   public static Pair<Stored<Forum>,String> resolveSubforum(Stored<Forum> that , String path) {
     Mutable<Pair<Stored<Forum>,String>> result
        = new Mutable<Pair<Stored<Forum>,String>>(Pair.pair(that,path));
     that.value.subforums.forEach( forum -> {
         if (path.startsWith(forum.value.handle + "/")) {
            result.accept(resolveSubforum(forum,path.substring(forum.value.handle.length() + 1)));
         } });
     return result.get();
   }
}