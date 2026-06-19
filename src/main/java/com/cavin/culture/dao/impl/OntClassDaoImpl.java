package com.cavin.culture.dao.impl;

import com.cavin.culture.dao.OntClassDao;
import com.cavin.culture.utils.TDBUtil;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Dataset;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Repository
public class OntClassDaoImpl implements OntClassDao {

    @Override
    public List<OntClass> getClasses() {
        List<OntClass> list = new ArrayList<>();
        Iterator<OntClass> classIterator = TDBUtil.getInferredOntModel().listClasses();
        while ( classIterator.hasNext() ) {
            OntClass ontClass = classIterator.next();
            if ( !ontClass.isAnon() ) {
                list.add(ontClass);
            }
        }
        return list;
    }

    @Override
    public List<OntClass> getRootClasses() {
        System.out.println("[OntClassDaoImpl] 开始获取根类...");
        List<OntClass> list = new ArrayList<>();
        try {
            OntModel ontModel = TDBUtil.getInferredOntModel();
            System.out.println("[OntClassDaoImpl] OntModel获取成功: " + (ontModel != null));
            if (ontModel == null) {
                System.err.println("[OntClassDaoImpl] OntModel为null，无法获取根类");
                return list;
            }
            Iterator<OntClass> classIterator = ontModel.listHierarchyRootClasses();
            System.out.println("[OntClassDaoImpl] 根类迭代器获取成功: " + (classIterator != null));
            while ( classIterator.hasNext() ) {
                OntClass ontClass = classIterator.next();
                if ( !ontClass.isAnon() ) {
                    System.out.println("[OntClassDaoImpl] 找到根类: " + ontClass.getLocalName() + " URI: " + ontClass.getURI());
                    // 过滤掉顶层的"革命文物"类，只保留其子类作为根类
                    if (!ontClass.getLocalName().equals("革命文物")) {
                        list.add(ontClass);
                    } else {
                        System.out.println("[OntClassDaoImpl] 跳过顶层类: 革命文物");
                    }
                }
            }
            System.out.println("[OntClassDaoImpl] 根类总数: " + list.size());
        } catch (Exception e) {
            System.err.println("[OntClassDaoImpl] 获取根类异常: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<OntClass> getSuperClasses(OntClass inClass) {
        List<OntClass> list = new ArrayList<>();
        Iterator<OntClass> classIterator = inClass.listSuperClasses();
        while ( classIterator.hasNext() ) {
            OntClass ontClass = classIterator.next();
            if ( !ontClass.isAnon() ) {
                list.add(ontClass);
            }
        }
        return list;
    }

    @Override
    public List<OntClass> getSubClasses(OntClass inClass) {
        List<OntClass> list = new ArrayList<>();
        Iterator<OntClass> classIterator = inClass.listSubClasses();
        while ( classIterator.hasNext() ) {
            OntClass ontClass = classIterator.next();
            if ( !ontClass.isAnon() ) {
                list.add(ontClass);
            }
        }
        return list;
    }

}
