/*
 * Automatic Creation Materialized Views
 * Authors: rpoliveira@inf.puc-rio.br, sergio@inf.puc-rio.br  *
 */
package mv;

import static bib.base.Base.log;
import static bib.base.Base.prop;
import bib.sgbd.Plan;
import bib.sgbd.SQL;
import bib.sgbd.oracle.PlanOracle;
import bib.sgbd.postgresql.PlanPostgreSQL;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Rafael
 */
public class MaterializedView extends SQL {

    private Plan hypoPlan;
    private String hypoMaterializedView;
    private long hypoGain;
    private long hypoNumPages;
    private long hypoCreationCost;
    private double hypoGainAC;

    public MaterializedView() {
    }

    public String getHypoPlan() {
        if (this.hypoPlan != null) {
            return hypoPlan.getPlan();
        } else {
            return "";
        }
    }

    public void setHypoPlan(String hypoPlan) {
        switch (prop.getProperty("sgbd")) {
            case "postgresql":
                this.hypoPlan = new PlanPostgreSQL(hypoPlan);
                break;
            case "oracle":
                this.hypoPlan = new PlanOracle(hypoPlan);
                break;
            default:
                erro();
        }
        this.setHypoNumPages();
        this.setHypoGain();
        this.setHypoGainAC();
        this.setHypoCreationCost();
    }

    public void setResultSet(ResultSet resultset) {
        try {
            this.setId(resultset.getInt("wld_id"));
            this.setSql(resultset.getString("wld_sql").toLowerCase());
            if (resultset.getObject("cmv_ddl_create") != null) {
                this.setHypoMaterializedView(resultset.getString("cmv_ddl_create").toLowerCase());
            }
            this.setCaptureCount(resultset.getInt("wld_capture_count"));
            this.setAnalyzeCount(resultset.getInt("wld_analyze_count"));
            this.setRelevance(resultset.getInt("wld_relevance"));
            this.setPlan(resultset.getString("wld_plan").toLowerCase(), prop.getProperty("sgbd"));
        } catch (SQLException e) {
            log.errorPrint(e);
        }
    }

    public long getHypoGain() {
        return hypoGain;
    }

    public void setHypoGain() {
        this.hypoGain = (this.getCost() - this.getHypoCost());
    }

    public double getHypoGainAC() {
        return hypoGainAC;
    }

    public void setHypoGainAC() {
        this.hypoGainAC = this.getHypoGain() * this.getCaptureCount();
    }

    public void setHypoNumPages() {
        double fillfactory = Double.valueOf(prop.getProperty("fillfactory" + prop.getProperty("sgbd")));
        int pagesize = Integer.valueOf(prop.getProperty("pagesize" + prop.getProperty("sgbd")));
        this.hypoNumPages = (long) ((this.hypoPlan.getNumRow() * this.hypoPlan.getSizeRow() * fillfactory) / pagesize);
        if (this.hypoNumPages < 1) {
            this.hypoNumPages = 1;
        }
    }

    public long getHypoCreationCost() {
        return hypoCreationCost;
    }

    public void setHypoCreationCost() {
        this.hypoCreationCost = (this.getHypoNumPages() * 2) + this.getCost();
    }

    public long getHypoNumPages() {
        return hypoNumPages;
    }

    public String getHypoMaterializedView() {
        return hypoMaterializedView;
    }

    public void setHypoMaterializedView(String hypoMaterializedView) {
        this.hypoMaterializedView = hypoMaterializedView;
    }

    @Override
    public void print() {
        super.print();
        log.title("custo hypotético visão " + this.getComents());
        log.msgPrint("HypoGain: " + this.getHypoGain());
        log.msgPrint("HypoGainAC: " + this.getHypoGainAC());
        log.msgPrint("HypoNumPages: " + this.getHypoNumPages());
        log.msgPrint("hypoNumRow: " + this.hypoPlan.getNumRow());
        log.msgPrint("hypoSizeRow: " + this.hypoPlan.getSizeRow());
        log.msgPrint("hypoCost: " + this.getHypoCost());
        log.msgPrint("Cost - hypoCost: " + (this.getCost() - this.getHypoCost()));
        log.msgPrint("HypoCreationCost: " + this.getHypoCreationCost());
        log.msgPrint("Hypo Plan: " + this.hypoPlan.getPlan());
        log.msgPrint("Hypo Query MV: " + this.hypoMaterializedView);
        log.endTitle();
    }

    public String getDDLCreateMV() {
        String ddl = prop.getProperty("getDDLCreateMV" + prop.getProperty("sgbd"));
        ddl = ddl.replace("$nameMV$", this.getNameMaterizedView());
        ddl = ddl.replace("$sqlMV$", this.getHypoMaterializedView());
        return ddl;
    }

    private Object erro() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNameMaterizedView() {
        return "v_ot_workload_" + String.valueOf(this.getId());
    }

    public boolean containsField(String clause, String field) {
        return clause.contains(" " + field + " ")
                || clause.contains(" " + field + ",")
                || clause.contains(" " + field + ";")
                || clause.contains(" " + field + "=")
                || clause.contains(" " + field + ">")
                || clause.contains(" " + field + "<")
                || clause.contains("," + field + ",")
                || clause.contains("," + field + ";")
                || clause.contains("," + field + "=")
                || clause.contains("," + field + ">")
                || clause.contains("," + field + "<")
                || clause.contains("." + field + ",")
                || clause.contains("." + field + ";")
                || clause.contains("." + field + "=")
                || clause.contains("." + field + ">")
                || clause.contains("." + field + "<");
    }

    private long getHypoCost() {
        return this.getHypoNumPages();
    }

}
