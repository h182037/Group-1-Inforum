package inf226.inforum.storage;

import inf226.inforum.*;
import inf226.inforum.storage.*;
import java.io.IOException;
import java.util.Map;
import java.util.Collections;
import java.util.TreeMap;
import java.util.UUID;


public class TransientStorage<T>
      implements Storage<T, String, IOException> {

   private final Map<UUID,Stored<T>> objects 
      = Collections.synchronizedMap
              (new TreeMap<UUID, Stored<T> >());


   public synchronized Stored<T> save(T value) throws IOException {
      final Stored<T> stored = new Stored<T>(value);
      objects.put(stored.identity,stored);
      return stored;
   }

   public synchronized Stored<T> update(Stored<T> object, T new_object)
      throws UpdatedException,DeletedException, IOException {
      final Stored<T> stored = objects.get(object.identity);
      if (stored == null) {
         throw new DeletedException();
      }
      if (!stored.version.equals(object.version)) {
         throw new UpdatedException(stored);
      }
      final Stored<T> new_stored = stored.newVersion();
      objects.put(new_stored.identity, new_stored);
      return new_stored;
   }

   public synchronized  void delete(Stored<T> object)
      throws UpdatedException,DeletedException,IOException {

      final Stored<T> stored = objects.get(object.identity);
      if (stored == null) {
         throw new DeletedException();
      }
      if (!stored.version.equals(object.version)) {
         throw new UpdatedException(stored);
      }
      objects.remove(stored.identity);
   }

   public ImmutableList< Stored<T> > lookup(String query)
      throws IOException {
      return ImmutableList.empty();
   }
}

