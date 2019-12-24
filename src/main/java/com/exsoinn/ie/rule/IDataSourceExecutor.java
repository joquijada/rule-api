package com.exsoinn.ie.rule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public interface IDataSourceExecutor {

    Connection getConnection() throws SQLException;

    DataSource getDataSource() throws SQLException;

    void closeConnection() throws SQLException;



}
