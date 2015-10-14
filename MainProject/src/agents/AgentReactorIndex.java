/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agents;

import static bib.base.Base.log;
import static bib.base.Base.prop;
import bib.sgbd.Column;
import bib.sgbd.Index;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author josemariamonteiro
 */
public class AgentReactorIndex extends AgentReactor {

    ArrayList<Index> candidateIndexes;

    public AgentReactorIndex() {
        this.candidateIndexes = new ArrayList<>();
    }

    @Override
    public void getLastTuningActionsNotAnalyzed() {
        this.getDDLNotAnalized();
    }

    public void getDDLNotAnalized() {
        try {
            candidateIndexes.clear();
            ResultSet resultset = driver.executeQuery(prop.getProperty("getSqlIndexNotAnalizedReactor"));
            PreparedStatement preparedStatement = null;
            ResultSet resultsetColumn = null;

            if (resultset != null) {
                while (resultset.next()) {
                    Index ind = new Index();
                    ind.setCidId(resultset.getInt("cid_id"));
                    ind.setTableName(resultset.getString("cid_table_name"));
                    ind.setIndexType(resultset.getString("cid_type"));
                    String indexName = ind.getTableName() + "_" + ind.getIndexType();

                    preparedStatement = driver.prepareStatement(prop.getProperty("getSqlIndexColumns"));
                    preparedStatement.setInt(1, ind.getCidId());

                    resultsetColumn = driver.executeQuery(preparedStatement);

                    if (resultsetColumn != null) {
                        while (resultsetColumn.next()) {
                            indexName = indexName + "_" + resultsetColumn.getString("cic_column_name");
                            Column c = new Column();
                            c.setName(resultsetColumn.getString("cic_column_name"));
                            ind.columns.add(c);
                        }
                    }
                    ind.setIndexName(indexName);
                    candidateIndexes.add(ind);
                    preparedStatement.close();
                    resultsetColumn.close();
                }
            }
            resultset.close();
        } catch (SQLException e) {
            log.error(e);
        }
    }

    @Override
    public void executeTuningActions() {
        PreparedStatement preparedStatement;
        //Percorre os índices a serem criados fisicamente (como índices reais)
        for (Index ind : this.candidateIndexes) {
            //Monta o comando CREATE INDEX
            String tableName = ind.getTableName();
            ArrayList<Column> columns = ind.columns;
            String columnName = null;
            String indexName = ind.getIndexName();
            String ddl = null;

            ddl = "CREATE INDEX "
                    + indexName
                    + " ON "
                    + tableName
                    + " USING btree (";

            for (int i = 0; i < columns.size(); i++) {
                columnName = columns.get(i).getName();
                if (i == 0) {
                    ddl = ddl + columnName + " ASC NULLS LAST";
                } else {
                    ddl = ddl + "," + columnName + " ASC NULLS LAST";
                }
            }
            ddl = ddl + ")";

            //Usado como Debug
            System.out.println(ddl);
            //Executa a Criação
            try {
                log.msg("Creating index " + ind.getName() + " with script: " + ddl);
                preparedStatement = driver.prepareStatement(ddl);
                driver.executeUpdate(preparedStatement);
                preparedStatement.close();
                log.msg("Finish create index " + ind.getName());
            } catch (SQLException ex) {
                log.error(ex);
            }

            //Se o índice for primário clusteriza
            if (ind.getIndexType().equals("P")) {
                try {
                    log.msg("Clustering Index " + indexName);
                    String ddlCluster = "Cluster " + tableName + " Using " + indexName;
                    preparedStatement = driver.prepareStatement(ddlCluster);
                    driver.executeUpdate(preparedStatement);
                    preparedStatement.close();
                    log.msg("Finish cluster index " + indexName);
                } catch (SQLException e) {
                    log.error(e);
                }

            }
        }
    }

    @Override
    public void updateStatusTuningActions() {
        try {
            for (Index ind : this.candidateIndexes) {
                PreparedStatement preparedStatement = driver.prepareStatement(prop.getProperty("getSqlClauseToUpdateDDLCreateIndexToMaterializationReactor"));
                preparedStatement.setString(1, "R");
                preparedStatement.setInt(2, ind.getCidId());
                driver.executeUpdate(preparedStatement);
                preparedStatement.close();
            }
            candidateIndexes.clear();
        } catch (SQLException e) {
            log.error(e);
        }
    }

}
