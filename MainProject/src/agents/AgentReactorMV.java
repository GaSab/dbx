/*
 * Automatic Creation Materialized Views
 *    *
 */
package agents;

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
public class AgentReactorMV extends Agent {

    ArrayList<MaterializedView> MVCandiates;
    protected ArrayList<MaterializedView> capturedQueriesForAnalyses;

    @Override
    public void run() {
        while (true) {
            try {
                this.getLastExecutedDDL();
                this.CreateMV();
                this.updateDDLForMaterialization();
                sleep(4000);
            } catch (InterruptedException e) {
                log.errorPrint(e);
            }
        }
    }

    public AgentReactorMV() {
        this.capturedQueriesForAnalyses = new ArrayList<>();
        this.MVCandiates = new ArrayList<>();
    }

    public void getLastExecutedDDL() {
        this.getDDLNotAnalized();
    }

    public void getDDLNotAnalized() {
        try {
            this.capturedQueriesForAnalyses.clear();
            ResultSet resultset = driver.executeQuery(prop.getProperty("getSqlDDLNotAnalizedReactor"));
            if (resultset != null) {
                while (resultset.next()) {
                    MaterializedView currentQuery = new MaterializedView();
                    currentQuery.setResultSet(resultset);
                    MVCandiates.add(currentQuery);
                    currentQuery.setResultSet(resultset);
                    this.capturedQueriesForAnalyses.add(currentQuery);
                }
            }
            log.msgPrint("Quantidade de DDLs encontradas para materialização: " + this.capturedQueriesForAnalyses.size());
            resultset.close();
        } catch (SQLException e) {
            log.errorPrint(e);
        }
    }

    public void CreateMV() {
        PreparedStatement preparedStatement;
        for (MaterializedView workload : this.MVCandiates) {
            if (!workload.getHypoMaterializedView().isEmpty()) {
                try {
                    log.ddlPrint("Materializando: " + workload.getHypoMaterializedView());
                    preparedStatement = driver.prepareStatement(workload.getHypoMaterializedView());
                    driver.executeUpdate(preparedStatement);
                    preparedStatement.close();
                } catch (SQLException ex) {
                    log.errorPrint(ex);
                }
            }
        }
    }

    public void updateDDLForMaterialization() {
        try {
            log.title("Persist update ddl create MV");
            for (MaterializedView currentQuery : this.MVCandiates) {
                PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToUpdateDDLCreateMVToMaterialization"));
                preparedStatement.setString(1, "R");
                preparedStatement.setInt(2, currentQuery.getId());
                driver.executeUpdate(preparedStatement);
                preparedStatement.close();
            }
            log.endTitle();
        } catch (SQLException e) {
            log.errorPrint(e);
        }
    }

}
