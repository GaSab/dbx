/*
 * Automatic Creation Materialized Views
 * Authors: rpoliveira@inf.puc-rio.br, sergio@inf.puc-rio.br  *
 */
package agents;

import algorithms.mv.Agrawal;
import algorithms.mv.DefineView;
import static bib.base.Base.log;
import static bib.base.Base.prop;
import bib.sgbd.Captor;
import bib.sgbd.SQL;
import static java.lang.Thread.sleep;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import mv.MaterializedView;

/**
 *
 * @author Rafael
 */
public class AgentObserverMV extends Agent implements Runnable {

    private final Captor captor;

    public AgentObserverMV() {
        this.captor = new Captor();
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.getLastExecutedSQL();
                this.analyzeQueriesCaptured();
                sleep(200);
            } catch (InterruptedException e) {
                log.errorPrint(e);
            }
        }
    }

    private void analyzeQueriesCaptured() {
        DefineView defineView = new DefineView();
        Agrawal agrawal = new Agrawal();
        ArrayList<MaterializedView> MVCandiates = this.getQueriesNotAnalized();
        for (MaterializedView MVCandiate : MVCandiates) {
            System.out.println(MVCandiate.getSql());
        }

        MVCandiates = agrawal.getWorkloadSelected(MVCandiates);
        MVCandiates = defineView.getWorkloadSelected(MVCandiates);
        MVCandiates = this.getPlanDDLViews(MVCandiates);
        this.persistDDLCreateMV(MVCandiates);
    }

    private ArrayList<MaterializedView> getQueriesNotAnalized() {
        ArrayList<MaterializedView> MVCandiates = new ArrayList<>();
        try {
            driver.createStatement();
            ResultSet resultset = driver.executeQuery(prop.getProperty("getSqlQueriesNotAnalized"));
            if (resultset != null) {
                while (resultset.next()) {
                    MaterializedView currentQuery = new MaterializedView();
                    currentQuery.setResultSet(resultset);
                    currentQuery.setSchemaDataBase(captor.getSchemaDataBase());
                    System.out.println(currentQuery.getSql());
                    System.exit(0);
                    MVCandiates.add(currentQuery);
                }
                if (!MVCandiates.isEmpty()) {
                    this.updateQueryAnalizedCount();
                }
            }
            driver.closeStatement();
        } catch (SQLException e) {
            log.errorPrint(e);
        }
        return MVCandiates;
    }

    protected void updateQueryAnalizedCount() {
        PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToUpdateWldAnalyzeCount"));
        log.dmlPrint(prop.getProperty("getSqlClauseToUpdateWldAnalyzeCount"));
        driver.executeUpdate(preparedStatement);
    }

    private void getLastExecutedSQL() {
        ArrayList<SQL> sqlCaptured = captor.getLastExecutedSQL();
        this.insertWorkload(sqlCaptured);
    }

    public void persistDDLCreateMV(ArrayList<MaterializedView> MVCandiates) {
        try {
            if (!MVCandiates.isEmpty()) {
                log.title("Persist ddl create MV");
                this.updateQueryAnalizedCount();
                for (MaterializedView query : MVCandiates) {
                    log.msgPrint(query.getComents());
                    if (!query.getHypoMaterializedView().isEmpty()) {
                        if (query.getAnalyzeCount() == 0) {
                            String ddlCreateMV = this.getSqlClauseToCreateMV(query.getHypoMaterializedView(), query.getNameMaterizedView());
                            PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToInsertDDLCreateMV"));
                            log.dmlPrint(prop.getProperty("getSqlClauseToInsertDDLCreateMV"));
                            preparedStatement.setInt(1, query.getId());
                            preparedStatement.setString(2, ddlCreateMV);
                            preparedStatement.setLong(3, query.getHypoCreationCost());
                            preparedStatement.setDouble(4, query.getHypoGainAC());
                            preparedStatement.setString(5, "H");
                            preparedStatement.setInt(6, query.getId());
                            query.setAnalyzeCount(1);
                            driver.executeUpdate(preparedStatement);
                        } else {
                            PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToIncrementBenefictDDLCreateMV"));
                            log.dmlPrint(prop.getProperty("getSqlClauseToIncrementBenefictDDLCreateMV"));
                            preparedStatement.setLong(1, query.getHypoCreationCost());
                            preparedStatement.setDouble(2, query.getHypoGainAC());
                            preparedStatement.setInt(3, query.getId());
                            driver.executeUpdate(preparedStatement);
                        }
                    }
                }
                log.endTitle();
            }
        } catch (SQLException e) {
            log.errorPrint(e);
        }
    }

    private String getSqlClauseToCreateMV(String hypoMaterializedView, String nameMaterizedView) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void insertWorkload(ArrayList<SQL> sqlCaptured) {
        if (!sqlCaptured.isEmpty()) {
            log.title("Insert / update TB_WORKLOAD");
            for (int i = 0; i < sqlCaptured.size(); ++i) {
                SQL query = sqlCaptured.get(i);
                sqlCaptured.remove(i);
                int queryId = this.getIdWorkload(query);
                log.dmlPrint(query.getSql());
                if (queryId == 0) {
                    this.insertQueryTbWorkload(query);
                } else {
                    query.setId(queryId);
                    this.updateQueryData(query);
                }
            }
            log.endTitle();
        }
    }

    public int getIdWorkload(SQL query) {
        try {
            String queryTemp = prop.getProperty("getSqlClauseToCheckIfQueryIsAlreadyCaptured");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            preparedStatement.setString(1, query.getSql());
            ResultSet result = driver.executeQuery(preparedStatement);
            if (result.next()) {
                return result.getInt("wld_id");
            } else {
                return 0;
            }
        } catch (SQLException e) {
            log.errorPrint(e.getMessage());
            return 1;
        }
    }

    public void insertQueryTbWorkload(SQL query) {
        try {
            try (PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToInsertQueryTbWorkload"))) {
                log.dmlPrint(prop.getProperty("getSqlClauseToInsertQueryTbWorkload") + " value of " + query);
                preparedStatement.setString(1, query.getSql());
                preparedStatement.setString(2, query.getPlan());
                preparedStatement.setInt(3, 1);
                preparedStatement.setInt(4, 0);
                preparedStatement.setString(5, query.getType());
                driver.executeUpdate(preparedStatement);
            }
        } catch (SQLException e) {
            log.errorPrint(e);
        }
    }

    public void updateQueryData(SQL query) {
        try {
            try (PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToUpdateQueryTbWorkload"))) {
                log.dmlPrint(prop.getProperty("getSqlClauseToUpdateQueryTbWorkload") + " value of " + query);
                preparedStatement.setString(1, query.getPlan());
                preparedStatement.setInt(2, query.getId());
                driver.executeUpdate(preparedStatement);
            }
        } catch (SQLException e) {
            log.errorPrint(e);
        }
    }

    private ArrayList<MaterializedView> getPlanDDLViews(ArrayList<MaterializedView> MVCandiates) {
        for (MaterializedView MVCandiate : MVCandiates) {
            MVCandiate.setHypoPlan(captor.getPlanExecution(MVCandiate));
        }
        return MVCandiates;
    }
}
