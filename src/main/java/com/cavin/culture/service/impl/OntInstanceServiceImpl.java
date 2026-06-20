package com.cavin.culture.service.impl;

import com.cavin.culture.bean.Triple;
import com.cavin.culture.dao.OntClassDao;
import com.cavin.culture.dao.OntInstanceDao;
import com.cavin.culture.service.OntInstanceService;
import com.cavin.culture.utils.TDBUtil;
import org.apache.jena.ontology.*;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OntInstanceServiceImpl implements OntInstanceService {

    @Autowired
    private OntInstanceDao ontInstanceDao;

    @Override
    public List<String> getInstancesByClass(String className) {
        OntClass inClass = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            inClass = TDBUtil.getInferredOntModel().getOntClass(url + "#" + className);
            if (inClass != null) {
                break;
            }
        }
        if (inClass != null){
            List<String> list = new ArrayList<>();
            ExtendedIterator<? extends OntResource> propertyIterator = inClass.listInstances();
            while ( propertyIterator.hasNext() ) {
                OntResource resource = propertyIterator.next();
                if ( !resource.isAnon() ) {
                    list.add(resource.getLocalName());
                }
            }
            return list;
        } else {
            // error
            System.out.println("未找到OntClass");
            return null;
        }
    }

    @Override
    public List<String> getAllObjectProperties() {
        List<String> list = new ArrayList<>();
        ExtendedIterator<ObjectProperty> properties = TDBUtil.getInferredOntModel().listObjectProperties();
        while (properties.hasNext()) {
            OntProperty ontProperty = properties.next();
            if ( !ontProperty.isAnon() ) {
                list.add(ontProperty.asResource().getLocalName());
            }
        }
        return list;
    }

    @Override
    public List<String> getInputSuggestion(String queryString) {
        List<String> list = new ArrayList<>();
        ExtendedIterator<Individual> individualsIt = TDBUtil.getInferredOntModel().listIndividuals();
        while (individualsIt.hasNext()) {
            Individual individual = individualsIt.next();
            if (individual.asResource().getLocalName().contains(queryString)) {
                list.add(individual.asResource().getLocalName());
            }
        }
        return list;
    }

    @Override
    public Map<String, String> getIndividualInfo(String individualName) {
        Individual individual = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName);
            if (individual != null) {
                break;
            }
        }
        if (individual != null){
            Map<String, String> map = new HashMap<>();
            StmtIterator stmtIterator = ontInstanceDao.getStatementsByInstance(individual);
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.nextStatement();
                if (statement.getObject().isLiteral()) {
                    map.put(statement.getPredicate().getLocalName(), statement.getObject().asLiteral().getString());
                }
            }
            return map;
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

    @Override
    public Map<String, Object> getIndividualRlatOfType(String individualName) {
        Individual individual = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName);
            if (individual != null) {
                break;
            }
        }
        if (individual != null){
            Map<String, Object> map = new HashMap<>();
            String classType = null;
            List<Object> rlatIndiv = new ArrayList<>();
            List<Object> rlatEvent = new ArrayList<>();
            List<Object> rlatRelic = new ArrayList<>();
            StmtIterator stmtIterator = ontInstanceDao.getStatementsByInstance(individual);
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.nextStatement();
                if (statement.getPredicate().getLocalName().equals("type")) {
                    String typeName = statement.getObject().asResource().getLocalName();
                    if (!typeName.equals("NamedIndividual")) {
                        classType = typeName;
                    }
                    continue;
                }
                if (statement.getObject().isResource()) {
                    if ( statement.getObject().asResource().getURI().contains("http://www.owl-ontologies.com/Individual.owl") ) {
                        Map<String, String> indivMap = new HashMap<>();
                        indivMap.put("name", statement.getObject().asResource().getLocalName());
                        indivMap.put("relation", statement.getPredicate().asResource().getLocalName());
                        rlatIndiv.add(indivMap);
                    }
                    if ( statement.getObject().asResource().getURI().contains("http://www.owl-ontologies.com/Event.owl") ) {
                        rlatEvent.add(statement.getObject().asResource().getLocalName());
                    }
                    if ( statement.getObject().asResource().getURI().contains("http://www.owl-ontologies.com/Relics.owl") ) {
                        rlatRelic.add(statement.getObject().asResource().getLocalName());
                    }
                }
            }
            map.put("class", classType);
            map.put("rlatIndiv", rlatIndiv);
            map.put("rlatEvent", rlatEvent);
            map.put("rlatRelic", rlatRelic);
            return map;
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

    @Override
    public List<Triple> getIndividualRlat(String individualName) {
        Individual individual = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName);
            if (individual != null) {
                break;
            }
        }
        if (individual != null){
            List<Triple> list = new ArrayList<>();
            List<String> processedPairs = new ArrayList<>();

            // 获取当前实例作为主语的关系
            StmtIterator stmtIterator = ontInstanceDao.getStatementsByInstance(individual);
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.nextStatement();
                if (statement.getPredicate().getLocalName().equals("type")) {
                    continue;
                }
                if (statement.getObject().isResource()) {
                    String subject = statement.getSubject().getLocalName();
                    String predicate = statement.getPredicate().getLocalName();
                    String object = statement.getObject().asResource().getLocalName();
                    String pairKey = subject + "-" + predicate + "-" + object;

                    if (!processedPairs.contains(pairKey)) {
                        Triple triple = new Triple(subject, predicate, object);
                        list.add(triple);
                        processedPairs.add(pairKey);
                    }
                }
            }

            // 获取当前实例作为宾语的关系（反向关系）
            StmtIterator allStmts = TDBUtil.getInferredOntModel().listStatements();
            while (allStmts.hasNext()) {
                Statement statement = allStmts.nextStatement();
                if (statement.getObject().isResource() &&
                    statement.getObject().asResource().getLocalName().equals(individualName) &&
                    !statement.getPredicate().getLocalName().equals("type")) {

                    String subject = statement.getSubject().getLocalName();
                    String predicate = statement.getPredicate().getLocalName();
                    String object = individualName;
                    String pairKey = subject + "-" + predicate + "-" + object;

                    if (!processedPairs.contains(pairKey)) {
                        Triple triple = new Triple(subject, predicate, object);
                        list.add(triple);
                        processedPairs.add(pairKey);
                    }
                }
            }

            return list;
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

    @Override
    public List<Map<String, String>> getResourceObjectWithCate(String individualName) {
        Individual individual = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName);
            if (individual != null) {
                break;
            }
        }
        if (individual != null){
            List<Map<String, String>> list = new ArrayList<>();
            List<String> duplicateList = new ArrayList<>();

            // 添加当前实例自身
            Map<String, String> selfMap = new HashMap<>();
            selfMap.put("name", individualName);
            // 从rdf:type语句获取类型
            String selfCategory = "未知类型";
            StmtIterator typeStmts = individual.listProperties(RDF.type);
            while (typeStmts.hasNext()) {
                Statement typeStmt = typeStmts.next();
                if (typeStmt.getObject().isResource()) {
                    String typeName = typeStmt.getObject().asResource().getLocalName();
                    if (!typeName.equals("NamedIndividual")) {
                        selfCategory = typeName;
                        break;
                    }
                }
            }
            selfMap.put("category", selfCategory);
            list.add(selfMap);
            duplicateList.add(individualName);

            // 获取当前实例作为主语的关系
            StmtIterator stmtIterator = ontInstanceDao.getStatementsByInstance(individual);
            while (stmtIterator.hasNext()) {
                Statement statement = stmtIterator.nextStatement();
                if (statement.getObject().isResource()) {
                    if (!duplicateList.contains(statement.getObject().asResource().getLocalName())) {
                        Map<String, String> map = new HashMap<>();
                        if (!statement.getPredicate().getLocalName().equals("type")) {
                            // 宾语的name及category
                            Individual objIndividual = TDBUtil.getInferredOntModel().getIndividual(statement.getObject().asResource().getURI());
                            map.put("name", statement.getObject().asResource().getLocalName());
                            if (objIndividual != null) {
                                // 从rdf:type语句获取类型
                                String objCategory = "未知类型";
                                StmtIterator objTypeStmts = objIndividual.listProperties(RDF.type);
                                while (objTypeStmts.hasNext()) {
                                    Statement typeStmt = objTypeStmts.next();
                                    if (typeStmt.getObject().isResource()) {
                                        String typeName = typeStmt.getObject().asResource().getLocalName();
                                        if (!typeName.equals("NamedIndividual")) {
                                            objCategory = typeName;
                                            break;
                                        }
                                    }
                                }
                                map.put("category", objCategory);
                            } else {
                                map.put("category", "未知类型");
                            }
                            duplicateList.add(statement.getObject().asResource().getLocalName());
                            list.add(map);
                        }
                    }
                }
            }

            // 获取当前实例作为宾语的关系（反向关系）
            StmtIterator allStmts = TDBUtil.getInferredOntModel().listStatements();
            while (allStmts.hasNext()) {
                Statement statement = allStmts.nextStatement();
                if (statement.getObject().isResource() &&
                    statement.getObject().asResource().getLocalName().equals(individualName) &&
                    !statement.getPredicate().getLocalName().equals("type")) {

                    String subjectName = statement.getSubject().getLocalName();
                    if (!duplicateList.contains(subjectName)) {
                        Map<String, String> map = new HashMap<>();
                        Individual subjIndividual = TDBUtil.getInferredOntModel().getIndividual(statement.getSubject().getURI());
                        map.put("name", subjectName);
                        if (subjIndividual != null) {
                            // 从rdf:type语句获取类型
                            String subjCategory = "未知类型";
                            StmtIterator subjTypeStmts = subjIndividual.listProperties(RDF.type);
                            while (subjTypeStmts.hasNext()) {
                                Statement typeStmt = subjTypeStmts.next();
                                if (typeStmt.getObject().isResource()) {
                                    String typeName = typeStmt.getObject().asResource().getLocalName();
                                    if (!typeName.equals("NamedIndividual")) {
                                        subjCategory = typeName;
                                        break;
                                    }
                                }
                            }
                            map.put("category", subjCategory);
                        } else {
                            map.put("category", "未知类型");
                        }
                        duplicateList.add(subjectName);
                        list.add(map);
                    }
                }
            }

            return list;
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

    @Override
    public List<Map<String, String>> queryForKnowledge(String individualName, String scope) {
        Individual individual = null;
        String individualURI = null;
        String scopeURI = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName);
            individualURI = url + "#" + individualName;
            if (individual != null) {
                break;
            }
        }
        if (individual != null){
            if (scope.equals("所有类")) {
                scopeURI = scope;
            } else {
                Iterator<OntClass> classIterator = TDBUtil.getInferredOntModel().listHierarchyRootClasses();
                while ( classIterator.hasNext() ) {
                    OntClass ontClass = classIterator.next();
                    if ( !ontClass.isAnon() && ontClass.getLocalName().equals(scope) ) {
                        scopeURI = ontClass.getURI().substring(0, ontClass.getURI().indexOf("#"));
                    }
                }
            }
            return ontInstanceDao.queryForKnowledge(individualURI, scopeURI);
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

    @Override
    public Map<String, Object> queryForRelation(String individualName1, String individualName2) {
        Individual individual1 = null;
        Individual individual2 = null;
        OntClass ontClass1;
        OntClass ontClass2;
        String individualURI1 = null;
        String individualURI2 = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual1 = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName1);
            individualURI1 = url + "#" + individualName1;
            if (individual1 != null) {
                break;
            }
        }
        for (String url : urls) {
            individual2 = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName2);
            individualURI2 = url + "#" + individualName2;
            if (individual2 != null) {
                break;
            }
        }
        if ( individual1 != null && individual2 != null ) {
            ontClass1 = individual1.getOntClass();
            while (ontClass1.hasSuperClass()){
                ontClass1 = ontClass1.getSuperClass();
            }
            ontClass2 = individual2.getOntClass();
            while (ontClass2.hasSuperClass()){
                ontClass2 = ontClass2.getSuperClass();
            }

            Map<String, Object> map = new HashMap<>();

            List<Map<String, String>> nodes = new ArrayList<>();
            Map<String, String> nodeMap1 = new HashMap<>();
            nodeMap1.put("name", individualName1);
            nodeMap1.put("category", ontClass1.getLocalName());
            Map<String, String> nodeMap2 = new HashMap<>();
            nodeMap2.put("name", individualName2);
            nodeMap2.put("category", ontClass2.getLocalName());
            nodes.add(nodeMap1);
            nodes.add(nodeMap2);

            List<Triple> links = new ArrayList<>();
            List<String> relations = ontInstanceDao.queryForRelation(individualURI1, individualURI2);
            for (String relation : relations) {
                Triple triple = new Triple(individualName1, relation, individualName2);
                links.add(triple);
            }

            map.put("nodes", nodes);
            map.put("links", links);

            return map;
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

    @Override
    public List<Triple> queryForProperty(String individualName, String predicateName) {
        Individual individual = null;
        String individualURI = null;
        String predicateURI = null;
        Set<String> urls = TDBUtil.getURIs();
        for (String url : urls) {
            individual = TDBUtil.getInferredOntModel().getIndividual(url + "#" + individualName);
            individualURI = url + "#" + individualName;
            if (individual != null) {
                break;
            }
        }
        if (individual != null) {
            ExtendedIterator<ObjectProperty> properties = TDBUtil.getInferredOntModel().listObjectProperties();
            while ( properties.hasNext() ) {
                ObjectProperty ontProperty = properties.next();
                if ( !ontProperty.isAnon() && ontProperty.asResource().getLocalName().equals(predicateName) ) {
                    predicateURI = ontProperty.asResource().getURI();
                }
            }
            List<Triple> list = new ArrayList<>();
            List<String> queryList = ontInstanceDao.queryForProperty(individualURI, predicateURI);
            for (String object : queryList) {
                Triple triple = new Triple(individualName, predicateName, object);
                list.add(triple);
            }
            return list;
        } else {
            // error
            System.out.println("未找到Individual");
            return null;
        }
    }

}
