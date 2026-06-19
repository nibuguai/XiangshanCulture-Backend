package com.cavin.culture.utils;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TDBUtil {

    public static String tdbDirectory;
    public static final String defaultNamedModel = "http://www.owl-ontologies.com/BeijingRevolution";
    public static final String inferredNamedModel = "http://www.owl-ontologies.com/BeijingRevolutionInferred";
    public static String owlPath;
    public static Set<String> urls;

    static {
        owlPath = "static/owl/";   // classpath 相对路径，读资源用

        tdbDirectory = System.getenv().getOrDefault("TDB_PATH", "./tdb");
    }

    public static Dataset getDataSet() {
        return TDBFactory.createDataset(tdbDirectory);
    }

    public static Model getDefaultModel() {
        Dataset ds = null;
        Model model = null;
        try {
            ds = TDBFactory.createDataset(tdbDirectory);
            ds.begin(ReadWrite.READ);
            model = ds.getNamedModel(TDBUtil.defaultNamedModel);
            ds.end();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return model;
    }

    public static Model getInferredModel() {
        Dataset ds = null;
        Model model = null;
        try {
            ds = TDBFactory.createDataset(tdbDirectory);
            ds.begin(ReadWrite.READ);
            model = ds.getNamedModel(TDBUtil.inferredNamedModel);
            ds.end();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return model;
    }

    public static OntModel getDefaultOntModel() {
        Dataset ds = null;
        OntModel ontModel = null;
        try {
            ds = TDBFactory.createDataset(tdbDirectory);
            ds.begin(ReadWrite.READ);
            ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, ds.getNamedModel(TDBUtil.defaultNamedModel));
            ds.end();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return ontModel;
    }

    public static OntModel getInferredOntModel() {
        Dataset ds = null;
        OntModel ontModel = null;
        try {
            System.out.println("[TDBUtil] TDB目录: " + tdbDirectory);
            System.out.println("[TDBUtil] 推理模型URI: " + TDBUtil.inferredNamedModel);
            ds = TDBFactory.createDataset(tdbDirectory);
            System.out.println("[TDBUtil] Dataset创建成功: " + (ds != null));
            ds.begin(ReadWrite.READ);
            Model model = ds.getNamedModel(TDBUtil.inferredNamedModel);
            System.out.println("[TDBUtil] Model获取成功: " + (model != null));
            if (model != null) {
                System.out.println("[TDBUtil] Model大小: " + model.size());
            }
            ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, model);
            System.out.println("[TDBUtil] OntModel创建成功: " + (ontModel != null));
            if (ontModel != null) {
                System.out.println("[TDBUtil] OntModel大小: " + ontModel.size());
            }
            ds.end();
        } catch (Exception e) {
            System.err.println("[TDBUtil] 获取OntModel异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return ontModel;
    }

    public static Set<String> getURIs() {
        Set<String> set = new HashSet<>();
        set.add("http://www.owl-ontologies.com/RevolutionaryRelics.owl");
        return set;
    }

    // org.apache.jena.sparql.ARQException: ResultSet no longer valid (QueryExecution has been closed)
    public static ResultSet query(String queryString) {
        Dataset ds = null;
        Model model = null;
        ResultSet results = null;
        try {
            ds = TDBFactory.createDataset(tdbDirectory);
            ds.begin(ReadWrite.READ);
            model = ds.getNamedModel(inferredNamedModel);
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, model);
            results = qe.execSelect();
            qe.close();
            ds.end();
        } finally {
            if (model != null) {
                model.close();
            }
            if (ds != null) {
                ds.close();
            }
        }
        return results;
    }

    public static void close(Dataset dataset) {
        if (dataset != null) {
            dataset.close();
        }
    }

}
