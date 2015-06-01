--
-- PostgreSQL database dump
--

-- Dumped from database version 9.3.2
-- Dumped by pg_dump version 9.3.2
-- Started on 2015-06-01 20:15:41

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- TOC entry 7 (class 2615 OID 257337)
-- Name: agent; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA agent;


ALTER SCHEMA agent OWNER TO postgres;

SET search_path = agent, pg_catalog;

--
-- TOC entry 209 (class 1255 OID 257338)
-- Name: limpa_estatisticas(); Type: FUNCTION; Schema: agent; Owner: postgres
--

CREATE FUNCTION limpa_estatisticas() RETURNS boolean
    LANGUAGE sql
    AS $$
delete from agent.tb_access_plan;
delete from agent.tb_candidate_view;
delete from agent.tb_workload;
delete from agent.tb_task_indexes;
delete from agent.tb_candidate_index_column;
delete from agent.tb_candidate_index;
delete from agent.tb_epoque;
delete from agent.tb_profits;
delete from agent.tb_task_indexes;
select true;
$$;


ALTER FUNCTION agent.limpa_estatisticas() OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 185 (class 1259 OID 257339)
-- Name: tb_access_plan; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_access_plan (
    wld_id integer NOT NULL,
    apl_id_seq integer NOT NULL,
    apl_text_line character varying(10000)
);


ALTER TABLE agent.tb_access_plan OWNER TO postgres;

--
-- TOC entry 186 (class 1259 OID 257345)
-- Name: tb_candidate_index; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_candidate_index (
    cid_id integer NOT NULL,
    cid_table_name character varying(100) NOT NULL,
    cid_index_profit integer DEFAULT 0 NOT NULL,
    cid_creation_cost integer DEFAULT 0 NOT NULL,
    cid_status character(1),
    cid_type character(1),
    cid_initial_profit integer,
    cid_fragmentation_level integer
);


ALTER TABLE agent.tb_candidate_index OWNER TO postgres;

--
-- TOC entry 187 (class 1259 OID 257350)
-- Name: tb_candidate_index_column; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_candidate_index_column (
    cid_id integer NOT NULL,
    cic_column_name character(100) NOT NULL,
    cic_type character(100)
);


ALTER TABLE agent.tb_candidate_index_column OWNER TO postgres;

--
-- TOC entry 188 (class 1259 OID 257353)
-- Name: tb_candidate_view; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_candidate_view (
    cmv_id integer NOT NULL,
    cmv_ddl_create text NOT NULL,
    cmv_cost bigint,
    cmv_profit bigint NOT NULL,
    cmv_status character(1) DEFAULT 'H'::bpchar
);
ALTER TABLE ONLY tb_candidate_view ALTER COLUMN cmv_id SET STATISTICS 0;
ALTER TABLE ONLY tb_candidate_view ALTER COLUMN cmv_profit SET STATISTICS 0;


ALTER TABLE agent.tb_candidate_view OWNER TO postgres;

--
-- TOC entry 2046 (class 0 OID 0)
-- Dependencies: 188
-- Name: TABLE tb_candidate_view; Type: COMMENT; Schema: agent; Owner: postgres
--

COMMENT ON TABLE tb_candidate_view IS 'Possiveis valores:
H: Hipotetico
R: Real';


--
-- TOC entry 2047 (class 0 OID 0)
-- Dependencies: 188
-- Name: COLUMN tb_candidate_view.cmv_status; Type: COMMENT; Schema: agent; Owner: postgres
--

COMMENT ON COLUMN tb_candidate_view.cmv_status IS 'Possiveis valores:
H: Hipotetico
R: Real
M: Materializar';


--
-- TOC entry 189 (class 1259 OID 257360)
-- Name: tb_epoque; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_epoque (
    epq_id integer NOT NULL,
    epq_start integer NOT NULL,
    epq_end integer NOT NULL
);


ALTER TABLE agent.tb_epoque OWNER TO postgres;

--
-- TOC entry 190 (class 1259 OID 257363)
-- Name: tb_profits; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_profits (
    cid_id integer NOT NULL,
    pro_timestamp integer NOT NULL,
    pro_profit integer NOT NULL,
    wld_id integer NOT NULL,
    pro_type character(1)
);


ALTER TABLE agent.tb_profits OWNER TO postgres;

--
-- TOC entry 191 (class 1259 OID 257366)
-- Name: tb_task_indexes; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_task_indexes (
    wld_id integer NOT NULL,
    cid_id integer NOT NULL
);


ALTER TABLE agent.tb_task_indexes OWNER TO postgres;

--
-- TOC entry 192 (class 1259 OID 257369)
-- Name: tb_workload; Type: TABLE; Schema: agent; Owner: postgres; Tablespace: 
--

CREATE TABLE tb_workload (
    wld_id integer NOT NULL,
    wld_sql character varying(10000) NOT NULL,
    wld_plan character varying(10000) NOT NULL,
    wld_capture_count integer DEFAULT 0 NOT NULL,
    wld_analyze_count integer DEFAULT 0 NOT NULL,
    wld_type character(1),
    wld_relevance integer
);


ALTER TABLE agent.tb_workload OWNER TO postgres;

--
-- TOC entry 193 (class 1259 OID 257377)
-- Name: tb_workload_wld_id_seq; Type: SEQUENCE; Schema: agent; Owner: postgres
--

CREATE SEQUENCE tb_workload_wld_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE agent.tb_workload_wld_id_seq OWNER TO postgres;

--
-- TOC entry 2048 (class 0 OID 0)
-- Dependencies: 193
-- Name: tb_workload_wld_id_seq; Type: SEQUENCE OWNED BY; Schema: agent; Owner: postgres
--

ALTER SEQUENCE tb_workload_wld_id_seq OWNED BY tb_workload.wld_id;


--
-- TOC entry 1910 (class 2604 OID 257379)
-- Name: wld_id; Type: DEFAULT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_workload ALTER COLUMN wld_id SET DEFAULT nextval('tb_workload_wld_id_seq'::regclass);


--
-- TOC entry 1912 (class 2606 OID 257381)
-- Name: pk_tb_access_plan; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_access_plan
    ADD CONSTRAINT pk_tb_access_plan PRIMARY KEY (wld_id, apl_id_seq);


--
-- TOC entry 1914 (class 2606 OID 257383)
-- Name: pk_tb_candidate_index; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_candidate_index
    ADD CONSTRAINT pk_tb_candidate_index PRIMARY KEY (cid_id);


--
-- TOC entry 1916 (class 2606 OID 257385)
-- Name: pk_tb_candidate_index_column; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_candidate_index_column
    ADD CONSTRAINT pk_tb_candidate_index_column PRIMARY KEY (cid_id, cic_column_name);


--
-- TOC entry 1920 (class 2606 OID 257387)
-- Name: pk_tb_epoque; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_epoque
    ADD CONSTRAINT pk_tb_epoque PRIMARY KEY (epq_id);


--
-- TOC entry 1922 (class 2606 OID 257389)
-- Name: pk_tb_profits; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_profits
    ADD CONSTRAINT pk_tb_profits PRIMARY KEY (cid_id, pro_timestamp);


--
-- TOC entry 1926 (class 2606 OID 257391)
-- Name: pk_tb_workload; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_workload
    ADD CONSTRAINT pk_tb_workload PRIMARY KEY (wld_id);


--
-- TOC entry 1918 (class 2606 OID 257393)
-- Name: tb_cadidate_view_pkey; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_candidate_view
    ADD CONSTRAINT tb_cadidate_view_pkey PRIMARY KEY (cmv_id);


--
-- TOC entry 1924 (class 2606 OID 257395)
-- Name: tb_task_indexes_pkey; Type: CONSTRAINT; Schema: agent; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY tb_task_indexes
    ADD CONSTRAINT tb_task_indexes_pkey PRIMARY KEY (wld_id, cid_id);


--
-- TOC entry 1928 (class 2606 OID 257396)
-- Name: fk_cid_id; Type: FK CONSTRAINT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_candidate_index_column
    ADD CONSTRAINT fk_cid_id FOREIGN KEY (cid_id) REFERENCES tb_candidate_index(cid_id);


--
-- TOC entry 1927 (class 2606 OID 257401)
-- Name: fk_wld_id; Type: FK CONSTRAINT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_access_plan
    ADD CONSTRAINT fk_wld_id FOREIGN KEY (wld_id) REFERENCES tb_workload(wld_id);


--
-- TOC entry 1930 (class 2606 OID 257406)
-- Name: fk_wld_id; Type: FK CONSTRAINT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_profits
    ADD CONSTRAINT fk_wld_id FOREIGN KEY (wld_id) REFERENCES tb_workload(wld_id);


--
-- TOC entry 1929 (class 2606 OID 257411)
-- Name: tb_cadidate_view_fk; Type: FK CONSTRAINT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_candidate_view
    ADD CONSTRAINT tb_cadidate_view_fk FOREIGN KEY (cmv_id) REFERENCES tb_workload(wld_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 1931 (class 2606 OID 257416)
-- Name: tb_task_indexes_cid_id_fkey; Type: FK CONSTRAINT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_task_indexes
    ADD CONSTRAINT tb_task_indexes_cid_id_fkey FOREIGN KEY (cid_id) REFERENCES tb_candidate_index(cid_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- TOC entry 1932 (class 2606 OID 257421)
-- Name: tb_task_indexes_wld_id_fkey; Type: FK CONSTRAINT; Schema: agent; Owner: postgres
--

ALTER TABLE ONLY tb_task_indexes
    ADD CONSTRAINT tb_task_indexes_wld_id_fkey FOREIGN KEY (wld_id) REFERENCES tb_workload(wld_id) ON UPDATE CASCADE ON DELETE CASCADE;


-- Completed on 2015-06-01 20:15:41

--
-- PostgreSQL database dump complete
--

