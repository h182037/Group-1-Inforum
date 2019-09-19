package inf226.inforum.storage;

import inf226.inforum.*;
import inf226.inforum.storage.*;

public interface Storage<T,Q,E extends Exception> {

   public Stored<T> save(T value) throws E;
   public Stored<T> update(Stored<T> object) throws UpdatedException,DeletedException,E;
   public void delete(Stored<T> object) throws UpdatedException,DeletedException,E;

   public ImmutableList< Stored<T> > lookup(Q query) throws E;
}
