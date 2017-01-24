package com.google.cloud.cache.apps.loadtest;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;

/**
 * Static utilities for stress test operations.
 *
 * <p><b>Note:</b> These utility functions typically ignore errors and allow exceptions to bubble up
 * and fail the test, as metrics obtained from partial scenarios provide little insight on
 * performance.
 */
final class DatastoreStressOperations {

  // Entity kind to use where actual kind name is irrelevant.
  private static final String ENTITY_KIND_DUMMY_NAME = "DummyKindName";

  // Number of retry attempts after contention errors.
  // A value of '0' will fail the test on any contention error.
  private static final int CONTENTION_RETRIES = 0;

  /**
   * Reads entities from datastore based on the given parameters.
   *
   * @param datastore datastore instance to use for the get operation.
   */
  static Entity read(DatastoreService datastore, String key) {
    try {
      return datastore.get(KeyFactory.createKey(ENTITY_KIND_DUMMY_NAME, key));
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  /**
   * Writes entities to datastore based on the given parameters.
   *
   * @param datastore datastore instance to use for the write operation.
   * @param kind entity kind to write.
   * @param key.
   * @param useTxn whether or not to use a transaction for the write.
   */
  static void write(DatastoreService datastore, String key, boolean useTxn) {
    // Standard transaction retries pattern. If first attempt fails due to a contention error, try
    // again at the expense of the overall test time.
    int retries = CONTENTION_RETRIES;
    while (true) {
      Transaction txn = null;
      if (useTxn) {
        TransactionOptions options = TransactionOptions.Builder.withXG(false);
        txn = datastore.beginTransaction(options);
      }

      try {
        write(datastore, key);

        if (useTxn) {
          txn.commit();
        }
        break;
      } catch (ConcurrentModificationException ex) {
        if (retries == 0) {
          throw ex;
        }
        retries--;
      } finally {
        if (useTxn && txn.isActive()) {
          txn.rollback();
        }
      }
    }
  }

  /** Helper function to handle datastore writes based on the given parameters. */
  private static void write(DatastoreService datastore, String key) {
    // First, write all entity groups into the appropriate tablets.
    List<Entity> entityGroups = new ArrayList<Entity>(1);
    for (int i = 0; i < 1; i++) {
      entityGroups.add(makeEntity(ENTITY_KIND_DUMMY_NAME, key, null, 1, 100, 1, 100, true));
    }
    datastore.put(entityGroups);

    // Write one entities in each entity group as needed.
    List<Entity> entities = new ArrayList<Entity>(1);
    for (Entity entityGroup : entityGroups) {
      Key parent = entityGroup.getKey();
      for (int i = 0; i < 1; i++) {
        entities.add(makeEntity(ENTITY_KIND_DUMMY_NAME, null, parent, 1, 100, 1, 100, true));
      }
    }
    datastore.put(entities);
  }

  /**
   * Deletes entities from datastore based on the given parameters.
   *
   * @param datastore datastore instance to use for the delete operation.
   * @param kind entity kind to delete.
   * @param count number of entities to delete.
   */
  static void delete(DatastoreService datastore, String key, int count) {
    List<Key> keys = new ArrayList<Key>(count);
    for (int i = 0; i < count; i++) {
      keys.add(KeyFactory.createKey(ENTITY_KIND_DUMMY_NAME, key));
    }
    datastore.delete(keys);
  }

  /**
   * Makes an entity with an optional key name and parent, and configurable number and size of
   * indexed and unindexed properties. The properties' contents are all 0-bytes.
   */
  private static Entity makeEntity(
      String kind,
      String keyName,
      Key parent,
      int indexedCount,
      int indexedSize,
      int unindexedCount,
      int unindexedSize,
      boolean randomContent) {
    Entity entity;
    if (keyName == null) {
      entity = new Entity(kind, parent);
    } else {
      entity = new Entity(kind, keyName, parent);
    }

    for (int i = 0; i < indexedCount; i++) {
      entity.setProperty("indexed" + i, makeShortBlob(indexedSize, randomContent));
    }
    for (int i = 0; i < unindexedCount; i++) {
      entity.setUnindexedProperty("unindexed" + i, makeBlob(unindexedSize, randomContent));
    }
    return entity;
  }

  /**
   * Makes a blob of size {@code size} whose contents are random if {@code randomize} is true and
   * all 0-bytes otherwise.
   */
  private static Blob makeBlob(int size, boolean randomize) {
    byte[] silly = new byte[size];
    if (randomize) {
      Random random = new Random();
      for (int i = 0; i < size; i++) {
        silly[i] = (byte) random.nextInt();
      }
    }
    return new Blob(silly);
  }

  /**
   * Makes a short blob of size {@code size} whose contents are random if {@code randomize} is true
   * and all 0-bytes otherwise.
   */
  private static ShortBlob makeShortBlob(int size, boolean randomize) {
    byte[] silly = new byte[size];
    if (randomize) {
      Random random = new Random();
      for (int i = 0; i < size; i++) {
        silly[i] = (byte) random.nextInt();
      }
    }
    return new ShortBlob(silly);
  }
}
