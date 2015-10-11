/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package algorithms.mv;

import static bib.base.Base.log;
import static bib.base.Base.prop;
import bib.driver.Driver;
import bib.sgbd.Column;
import bib.sgbd.Filter;
import bib.sgbd.Index;
import bib.sgbd.SQL;
import bib.sgbd.SeqScan;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Administrador
 */
public class IHSTIS_CI {

    private ArrayList<SeqScan> sso = null;
    private SeqScan ss = null;
    private ArrayList<Column> colsSelect = null;
    private ArrayList<Column> colsGroup = null;
    private ArrayList<Column> colsOrder = null;
    private ArrayList<Index> lCandidates = new ArrayList();
    private ArrayList<Filter> filterColumns = null;
    private Filter filterAux = null;
    private Index indexAuxP = null;
    private Index indexAuxS = null;
    private long indexScanCost = 0;
    private long seqScanCost = 0;
    private long wldId = 0;
    private long profit = 0;

    public void runAlg(SQL sql) {

        //Recupera o codigo da Task (wld_id)
        wldId = sql.getId();

        if (sql.getPlan() != null) {
            sso = sql.plan().getSeqScanOperations();
        }

        //Percorre as operacoes de SeqScan
        for (int i = 0; i < sso.size(); i++)
        {
            ss = sso.get(i);

            //Percorre cada coluna (filter) do SeqScan
            filterColumns = ss.getFilterColumns();
            for (int j = 0; j < filterColumns.size(); j++) {
                filterAux = filterColumns.get(j);

                //Adiciona indice primario
                //Cria um indice candidato primario para cada coluna do SeqScan
                indexAuxP = new Index();
                indexAuxP.setTableName(ss.getTableName());
                indexAuxP.columns = new ArrayList();
                indexAuxP.columns.add(filterAux);
                indexAuxP.setIndexType("Primary");
                indexAuxP.setCreationCost(2 * getSeqScanCost(ss.getTableName()));
                indexAuxP.setHasFilter(true);
                indexAuxP.setFilterType(filterAux.getFilterType());
                indexAuxP.setNumberOfRows(ss.getNumberOfRows());
                lCandidates.add(indexAuxP);

                //Adicionando Indice Secundario
                //Cria um indice candidato secundario para cada coluna do SeqScan
                indexAuxS = new Index();
                indexAuxS.setTableName(ss.getTableName());
                indexAuxS.columns = new ArrayList();
                indexAuxS.columns.add(filterAux);
                indexAuxS.setIndexType("Secundary");
                indexAuxS.setCreationCost(2 * getSeqScanCost(ss.getTableName()));
                indexAuxS.setHasFilter(true);
                indexAuxS.setFilterType(filterAux.getFilterType());
                indexAuxS.setNumberOfRows(ss.getNumberOfRows());
                lCandidates.add(indexAuxS);
            }
        }

        //TODO: Para cada tabela presente em uma cláusula SELECT faça:
        //Criar um indice composto secundário para todos os atributos desta tabela que aparecem na Consulta SQL
        //Usa a comando SQL para pegar os atributos envolvidos nas clausulas SELECT, GROUP e ORDER
        //colsSelect = sql.getFieldsSelect();
        //colsGroup = sql.getFieldsGroup();
        //colsOrder = sql.getFieldsOrder();
        if (colsSelect.size() > 0) {

        }
        //PS. Um comando SQL pode ter várias cláusulas SELECT
        //É importante saber qual a tabela de uma determinada coluna. 
        //Logo, é necessário preencher o campo table da classe Column
        //SELECT nome FROM empregado WHERE salario >= ALL (SELECT salario_base FROM cargo)
        //Devolver um ArrayList de Tabela, onde cada objeto Tabela tem um conjunto de atributos usados em um ou mais cláusulas SELECT
        //Agrupar por tabela
        //SELECT e.enome, d.dnome FROM empregado e, departamento d WHERE e.lotacao=d.codigo
        //Talvez, o mais fácil seja utilizar o Parser ZQL [Thuraisingham et al. 2010].
       
        //TODO: Juntar os atributos do Case 1 (Filtro) e do Case 2 (Select, Group e Order) para uma mesma Tabela
        //Criar um novo índice secundário com os atributos do índice anterior (Select, Group e Order) c
        //com os atributos de cada um dos índices criados anteriormente (envolvidos em filter)
        //Basta criar um novo índice secundário com todos os atributos presentes nos índices que já estão em lCandidates (removendo os atributos repetidos)
        
        
        
        //Percorrer os Indices Candidatos
        for (Index lCandidate : lCandidates) {
            //Testa se o indice ja existe na metabase local
            if (!inLM(lCandidate)) {
                //Insere um novo indice candidato na LM
                insertIndexLM(lCandidate);
            } else {
                //verificar se o indice ja estah associado ah tarefa corrente
                if (!inTaskIndexes(wldId, lCandidate)) {
                    //Inserir tupla na tabela tb_task_indexes
                    insertTaskIndexes(wldId, lCandidate);
                }
            }
            //Verifica se o indice eh hipotetico (ou seja, nao eh um indice real)
            if (isHypotheticalIndex(lCandidate)) {
                //Estimar Custo de Index Scan
                indexScanCost = getIndexScanCost(lCandidate);
                //Estima custo do SeqScan
                seqScanCost = getSeqScanCost(lCandidate.getTableName());
                //Verifica se o custo de utilizar o indice (Index Scan) eh menor que o custo do SeqScan
                if (indexScanCost < seqScanCost) {
                    profit = seqScanCost - indexScanCost;
                }
                updateProfit(lCandidate, profit);
            }
            //Se o indice eh real e nao foi utilizado deveria ter um beneficio descontado???
        }
    }

    private void updateProfit(Index ind, long profit) {
        //Atualiza o beneficio acumulado do indice
        Driver driver = new Driver();
        try {
            String queryTemp = prop.getProperty("setDMLUpdateCandidateIndexProfitonpostgresql");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            //cid_index_profit
            preparedStatement.setLong(1, profit);
            //cid_id
            preparedStatement.setInt(2, getIndexId(ind));
            
            //Executa a inserção
            driver.executeUpdate(preparedStatement);
            
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
    
    private int getIndexId(Index ind){
        //Verificar se existe indice na ML definido sobre a mesma tabela e memos atributos
        //Sera necessario utilizar as tabelas tb_candidate_index e tb_candidate_index_column
        //Retorna zero se não encontrar o índice
        ArrayList<Integer> ids = new ArrayList();
        int cid;
        Driver driver = new Driver();
        int cidId = 0;
        //Recupera os índices existentes com o mesmo número de colunas do índice recebido como parâmetro
        try {
            String queryTemp = prop.getProperty("getDMLIndexNamesWithConditionpostgresql");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            preparedStatement.setString(1, ind.getTableName());
            preparedStatement.setString(2, ind.getIndexType());
            preparedStatement.setInt(3, ind.columns.size());
            ResultSet result = driver.executeQuery(preparedStatement);
            while (result.next()) {
                cid = result.getInt("cid_id");
                ids.add(cid);
            }
            result.close();  
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        
        //Percorre os índices identificados, verificando as colunas
        Column column; 
        String columnName;
        
        //Percorre os índices possíveis, com o memso número de colunas
        for (int i = 0; i < ids.size(); i++){
            //Guarda o id do índice que está sendo verificado
            cidId=ids.get(i);
            //Percorre as colunas
            for (int j = 0; j < ind.columns.size(); i++)
            {
                column = ind.columns.get(j);
                columnName = column.getName();

                try {
                    String queryTemp = prop.getProperty("getDMLIndexColumnWithConditionpostgresql");
                    PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
                    preparedStatement.setInt(1, ids.get(i));
                    preparedStatement.setString(2, columnName);
                    ResultSet result = driver.executeQuery(preparedStatement);
                    if (result.next()) {
                        result.close();
                    }
                    else{
                        cidId=0;
                        result.close();  
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }

            }
            
            if (cidId>0){
                return cidId;
            }
        
        }
        
        return cidId;
    }

    private boolean isHypotheticalIndex(Index ind) {
        //Verificar na tabela tb_candidate_index a coluna cid_status: H -> Hypothetical; R-> Real
        
        int indId = getIndexId(ind);
        Driver driver = new Driver();
        String status=null;
        
        if (indId > 0){
            //Recupera o status do índice
            try {
                String queryTemp = prop.getProperty("setDMSelectIndexStatusonpostgresql");
                PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
                preparedStatement.setInt(1, indId);
                ResultSet result = driver.executeQuery(preparedStatement);
                if (result.next()) {
                    status = result.getString("cid_status");
                    result.close();
                } else {
                    result.close();
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }
        
        return status.equals("H");
    }

    private void insertTaskIndexes(long wldId, Index ind) {
        //verificar se o indice ja estah associado ah tarefa na tabela tb_task_indexes
        
        Driver driver = new Driver();
        
        int indId = getIndexId(ind);
        
        if(indId>0){
            try {
                String queryTemp = prop.getProperty("setDMLInsertTaskIndexesonpostgresql");
                PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
                //cid_index_profit
                preparedStatement.setLong(1, wldId);
                //cid_id
                preparedStatement.setInt(2, getIndexId(ind));

                //Executa a inserção
                driver.executeUpdate(preparedStatement);

            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }

    }

    private boolean inTaskIndexes(long wldId, Index ind) {
        //verificar se o indice ja estah associado ah tarefa na tabela tb_task_indexes
        
        //Recupera o id do índice
        int indId = getIndexId(ind);
        
        Driver driver = new Driver();
        boolean isInTaskIndexes = false;
        
        if (indId > 0){
            //Verifica se a tupla existe
            try {
                String queryTemp = prop.getProperty("setDMSelectTaskIndexesonpostgresql");
                PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
                preparedStatement.setInt(1, indId);
                preparedStatement.setLong(2, wldId);
                ResultSet result = driver.executeQuery(preparedStatement);
                
                if (result.next()) {
                    isInTaskIndexes = true;
                    result.close();
                } else {
                    result.close();
                }
                
            } catch (SQLException e) {
                log.error(e.getMessage());
            }
        }

        return isInTaskIndexes;
    }

    private boolean inLM(Index ind) {
        //Verificar se existe indice na ML definido sobre a mesma tabela e memos atributos
        //Sera necessario utilizar as tabelas tb_candidate_index e tb_candidate_index_column
        ArrayList<Integer> ids = new ArrayList();
        int cid;
        Driver driver = new Driver();
        boolean isInLM = false;
        //Recupera os índices existentes com o mesmo número de colunas do índeice recebido como parâmetro
        try {
            String queryTemp = prop.getProperty("getDMLIndexNamesWithConditionpostgresql");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            preparedStatement.setString(1, ind.getTableName());
            preparedStatement.setString(2, ind.getIndexType());
            preparedStatement.setInt(3, ind.columns.size());
            ResultSet result = driver.executeQuery(preparedStatement);
            while (result.next()) {
                cid = result.getInt("cid_id");
                ids.add(cid);
            }
            result.close();  
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        
        //Percorre os índices identificados, verificando as colunas
        Column column; 
        String columnName;
        
        //Percorre os índices possíveis, com o memso número de colunas
        for (int i = 0; i < ids.size(); i++){
            //Avalia um determinado índice possível
            isInLM = true;
            //Percorre as colunas
            for (int j = 0; j < ind.columns.size(); i++)
            {
                column = ind.columns.get(j);
                columnName = column.getName();

                try {
                    String queryTemp = prop.getProperty("getDMLIndexColumnWithConditionpostgresql");
                    PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
                    preparedStatement.setInt(1, ids.get(i));
                    preparedStatement.setString(2, columnName);
                    ResultSet result = driver.executeQuery(preparedStatement);
                    if (result.next()) {
                        result.close();
                    }
                    else{
                        isInLM = false;
                        result.close();  
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }
            }
            
            if(isInLM){
                return isInLM;
            }
            
        }
        return isInLM;
    }

    //TODO: Implementar
    private void insertIndexLM(Index ind) {
        //Inserir indice na LM
        //Beneficio acumulado = 0
        //NQ (numero de consultas que usa o indice) = 0
        //Inserir linha na tabela tb_candidate_index
        //Inserir uma ou mais linhas na tabela tb_candidate_index_column. Uma linha para cada coluna
        //Inserir linha na tabela tb_task_indexes
        
        Driver driver = new Driver();
        int maxId=0;
        
        //Recupera o maior valor para cid_id
        try {
            String queryTemp = prop.getProperty("getDMLMaxIndexIDonpostgresql");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            ResultSet result = driver.executeQuery(preparedStatement);
            if (result.next()) {
                maxId = result.getInt("maxId");
                result.close();
            } else {
                result.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        
        //Gera cid_id do novo índice candidato
        maxId++;
        
        //Insere o novo índice candidato
        try {
            String queryTemp = prop.getProperty("setDMLInsertCandidateIndexonpostgresql");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            //cid_id
            preparedStatement.setInt(1, maxId);
            //cid_table_name
            preparedStatement.setString(2, ind.getTableName());
            //cid_index_profit
            preparedStatement.setInt(3, 0);
            //cid_creation_cost
            preparedStatement.setLong(4, ind.getCreationCost());
            //cid_status
            preparedStatement.setString(5, "H");
            //cid_type
            preparedStatement.setString(6, ind.getIndexType());
            //cid_initial_profit
            preparedStatement.setInt(7, 0);
            //cid_fragmentation_level
            preparedStatement.setInt(8, 0);
            //cid_initial_ratio
            preparedStatement.setInt(9, 0);
            //cid_index_name
            preparedStatement.setString(10, ind.getName());
            //cid_creation_time (as a real index)
            preparedStatement.setInt(11, 0);
            
            //Executa a inserção
            driver.executeUpdate(preparedStatement);
            
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        
        
    }

    private long getIndexScanCost(Index ind) {
        if (ind.getIndexType().equals("Primary")) {

        } else {

        }
        return 0;
    }


    private long getSeqScanCost(String tableName) {
        Driver driver = new Driver();
        int numberOfTablePages = 0;
        try {
            String queryTemp = prop.getProperty("getSeqScanCostpostgresql");
            PreparedStatement preparedStatement = driver.prepareStatement(queryTemp);
            preparedStatement.setString(1, tableName);
            ResultSet result = driver.executeQuery(preparedStatement);
            if (result.next()) {
                numberOfTablePages = result.getInt("rel_pages");
                result.close();
            } else {
                result.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        return numberOfTablePages;
    }
}

    