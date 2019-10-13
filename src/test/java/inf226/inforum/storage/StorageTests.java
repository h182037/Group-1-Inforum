package inf226.inforum.storage;

import inf226.inforum.*;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

public class StorageTests{
    @Test
    void storedNewVerion() {
        Stored<String> stored = new Stored<String>("foobar");
        Stored<String> updated = stored.newVersion("barbar");
        assertTrue(updated.identity.equals(stored.identity));
        assertFalse(updated.version.equals(stored.version));
        assertEquals(updated.value,"barbar");
    }
    private<U,Q,E extends Exception> void testStorageSave(Storage<U,Q,E> storage, U value) {
        try {
            Stored<U> stored = storage.save(value);
            assertTrue(stored.value.equals(value));
        } catch (Exception exception) {
            fail("Could not save to storage.");
        }
    }
    private<U,Q,E extends Exception> void testStorageUpdate(Storage<U,Q,E> storage, U value0, U value1) {
        assertFalse(value0.equals(value1));
        try {
            Stored<U> stored = storage.save(value0);
            assertTrue(stored.value.equals(value0));
            boolean updated = true;
            while(updated) {
                try {
                    assertTrue(storage.update(stored,value1).value.equals(value1));
                    updated = false;
                } catch (UpdatedException e) {
                    stored = (Stored<U>)e.newObject;
                }
            }
        } catch (Exception exception) {
            fail("Could not save to storage.");
        }
    }

    @Test
    void testMessageStorageSave() {
        try{
            MessageStorage storage = new MessageStorage("test-message-store.db");
            Message message = new Message("Alice","Hello world!",Instant.now());
            testStorageSave(storage,message);
        } catch (Exception e) {
            fail(e);
        }
    }
}

