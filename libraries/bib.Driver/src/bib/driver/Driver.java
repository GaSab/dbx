/*
 * Automatic Create Materialized Views
 * Authors: rpoliveira@inf.puc-rio.br, sergio@inf.puc-rio.br  *
 */
package bib.driver;

import bib.base.Base;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Driver extends Base {

    public Driver() {
        super();
        connect();
    }

    protected static Connection connection = null;

    private void connect() {
        try {
            if (connection == null) {
                switch (prop.getProperty("sgbd")) {
                    case "sqlserver":
                        connection = DriverManager.getConnection(Driver.prop.getProperty("urlSQLServer") + "databaseName=" + Driver.prop.getProperty("databaseName") + ";", Driver.prop.getProperty("userSQLServer"), Driver.prop.getProperty("pwdSQLServer"));
                        log.msg("Conectado ao bd " + Driver.prop.getProperty("urlSQLServer") + ":" + Driver.prop.getProperty("databaseName"));
                        break;
                    case "postgresql":
                        connection = DriverManager.getConnection(Driver.prop.getProperty("urlPostgres") + Driver.prop.getProperty("databaseName"), Driver.prop.getProperty("userPostgres"), Driver.prop.getProperty("pwdPostgres"));
                        log.msg("Conectado ao bd " + Driver.prop.getProperty("urlPostgres") + ":" + Driver.prop.getProperty("databaseName"));
                        break;
                    case "oracle":
                        connection = DriverManager.getConnection(Driver.prop.getProperty("urlOracle") + Driver.prop.getProperty("databaseName"), Driver.prop.getProperty("userOracle"), Driver.prop.getProperty("pwdOracle"));
                        log.msg("Conectado ao bd " + Driver.prop.getProperty("urlOracle") + Driver.prop.getProperty("databaseName"));
                        break;
                    default:
                        throw new UnsupportedOperationException("Atributo SGBD do arquivo de parâmetros (.properties) nao foi atribuido corretamente.");
                }
                log.msg("Aplicação versão: " + Driver.prop.getProperty("versao"));
            }
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public void closeConnection() throws SQLException {
        Driver.connection.close();
    }

    public PreparedStatement prepareStatement(String query) {
        try {
            return Driver.connection.prepareStatement(prop.getProperty("signature") + " " + query);
        } catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    public ResultSet executeQuery(String query) {
        try {
            Statement statement = Driver.connection.createStatement();
            statement.closeOnCompletion();
            return statement.executeQuery(prop.getProperty("signature") + " " + query);
        } catch (SQLException e) {
            log.msg(query);
            log.error(e);
            return null;
        }
    }

    public void executeUpdate(PreparedStatement prepared) {
        try {
            prepared.executeUpdate();
        } catch (SQLException e) {
            log.error(e);
        } finally {
            try {
                prepared.close();
            } catch (SQLException ex) {
                log.error(ex);
            }
        }
    }

    public void executeUpdate(String query) {
        PreparedStatement prepared = this.prepareStatement(query);
        try {
            prepared.executeUpdate();
        } catch (SQLException e) {
            log.msg("Query com erro:" + query);
            log.error(e);
        } finally {
            try {
                prepared.close();
            } catch (SQLException ex) {
                log.error(ex);
            }
        }
    }

    public ResultSet executeQuery(PreparedStatement prepared) {
        try {
            return prepared.executeQuery();
        } catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    public Statement getStatement() {
        try {
            Statement statement = Driver.connection.createStatement();
            statement.closeOnCompletion();
            return statement;
        } catch (SQLException ex) {
            log.error(ex);
            return null;
        }
    }

}