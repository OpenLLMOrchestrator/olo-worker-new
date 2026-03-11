package com.olo.db.repository;

import com.olo.db.DbClient;

/**
 * Base for repository layer: holds {@link DbClient} and runs SQL via {@link DbClient#execute}.
 * Repositories contain queries; the client abstracts the database implementation.
 */
public abstract class DbRepository {

  private final DbClient dbClient;

  protected DbRepository(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  protected DbClient getDbClient() {
    return dbClient;
  }
}
