package com.querycraft.exception;

public class InvalidSqlException extends RuntimeException {
    private final String sql;

    public InvalidSqlException(String message, String sql) {
        super(message);
        this.sql = sql;
    }

    public InvalidSqlException(String message, String sql, Throwable cause) {
        super(message, cause);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
