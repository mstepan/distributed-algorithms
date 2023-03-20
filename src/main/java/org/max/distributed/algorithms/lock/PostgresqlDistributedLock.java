package org.max.distributed.algorithms.lock;

import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

public class PostgresqlDistributedLock {

    private static final String DUPLICATE_KEY_ERROR_CODE = "23505";

    private static final String KEY_CONSTRAINT_NAME = "lock_pkey";

    private final String lockName;

    private final PGPoolingDataSource dataSource;

    private volatile Connection lastConnection;

    public PostgresqlDistributedLock() {
        this.lockName = "my_lock_" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        this.dataSource = initDataSource();
    }

    public void lock() {

        Connection con;
        try {
            con = dataSource.getConnection();
            con.setAutoCommit(false);

            boolean rowLocked = tryLockRow(con, lockName);

            if (rowLocked) {
                lastConnection = con;
                return;
            }

            tryInsertRowForLock(con, lockName);

            // try lock for the 2nd time after row inserted into LOCK table
            rowLocked = tryLockRow(con, lockName);

            if (!rowLocked) {
                throw new IllegalStateException("Row wasn't locked after INSERT, really strange");
            }
            lastConnection = con;
        }
        catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    /**
     * Try INSERT row into LOCK table that can be used for lock.
     * Multiple threads may insert the same row at the same time, so at least one will success all other will fail
     * with SQLException.
     */
    private void tryInsertRowForLock(Connection conn, String lockName) throws SQLException {
        try {
            PreparedStatement prepSt = conn.prepareStatement("INSERT into lock(lock_name) VALUES (?)");
            prepSt.setString(1, lockName);
            prepSt.executeUpdate();
        }
        catch (SQLException sqlEx) {

            if (sqlEx instanceof PSQLException psqlException) {
                ServerErrorMessage errorMsg = psqlException.getServerErrorMessage();

                if (errorMsg == null) {
                    System.err.println("PSQLException exception without 'ServerErrorMessage' detected");
                }
                else {
                    // for postgresql error codes check https://www.postgresql.org/docs/current/errcodes-appendix.html
                    if (DUPLICATE_KEY_ERROR_CODE.equals(errorMsg.getSQLState()) &&
                        KEY_CONSTRAINT_NAME.equals(errorMsg.getConstraint())) {
                        System.out.printf("Expected duplicate key constraint violation for '%s'\n",
                            errorMsg.getConstraint());
                    }
                }
            }
            else {
                System.err.printf("Strange sql exception detected, not instanceof 'PSQLException': '%s'\n",
                    sqlEx.getClass().getCanonicalName());
            }
            conn.rollback();
        }
        finally {
            conn.commit();
        }
    }

    /**
     * Try select row from LOCK table for UPDATE.
     */
    private boolean tryLockRow(Connection conn, String lockName) throws SQLException {

        PreparedStatement prepSt =
            conn.prepareStatement("SELECT * FROM lock WHERE lock_name = ? FOR UPDATE");
        prepSt.setString(1, lockName);

        try (ResultSet res = prepSt.executeQuery()) {
            return res.next();
        }
        catch (SQLException sqlEx) {
            System.err.println("lock acquisition failed by timeout");
            return false;
        }
    }

    public void unlock() {
        try {
            try (Connection curConn = lastConnection) {
                curConn.commit();
            }
        }
        catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
    }


    private PGPoolingDataSource initDataSource() {

        PGPoolingDataSource dataSource = new PGPoolingDataSource();
        dataSource.setDataSourceName("Distributed Lock DataSource");
        dataSource.setServerNames(new String[] {
            "localhost"
        });
        dataSource.setDatabaseName("postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");
        dataSource.setMaxConnections(10);
        return dataSource;
    }

}
