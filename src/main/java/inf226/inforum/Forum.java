package inf226.inforum;

import inf226.inforum.storage.Stored;

public class Forum {
   public final String handle;
   public final String name;
   public final ImmutableList<Thread> threads;
   public final ImmutableList<Forum> subforums;

   public Forum(String handle, String name, ImmutableList<Thread> threads, ImmutableList<Forum> subforums) {
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

   Pair<Forum,String> resolveSubforum(String path) {
     Mutable<Pair<Forum,String>> result
        = new Mutable<Pair<Forum,String>>(Pair.pair(this,path));
     subforums.forEach( forum -> {
         if (path.startsWith(forum.handle + "/")) {
            result.accept(forum.resolveSubforum(path.substring(forum.handle.length() + 1)));
         } });
     return result.get();
   }
}