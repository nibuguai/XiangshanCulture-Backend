package com.cavin.culture.controller.listener;

import com.cavin.culture.utils.TDBUtil;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.tdb.TDBFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.*;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;

@WebListener
public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        File file = new File(TDBUtil.tdbDirectory);
        boolean needInit = false;

        // 检查是否需要初始化
        if (!file.isDirectory()) {
            System.out.println("[ContextListener] TDB目录不存在，需要初始化: " + TDBUtil.tdbDirectory);
            needInit = true;
        } else {
            // 检查TDB目录是否有数据
            String[] files = file.list();
            if (files == null || files.length == 0) {
                System.out.println("[ContextListener] TDB目录存在但为空，需要重新初始化: " + TDBUtil.tdbDirectory);
                needInit = true;
            } else {
                System.out.println("[ContextListener] TDB目录已存在且有数据，文件数: " + files.length);
            }
        }

        if (needInit) {
            Dataset dataset = null;
            OntModel ontModel = null;
            try {
                System.out.println("[ContextListener] 开始初始化TDB...");
                
                ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
                InputStream in = getClass().getClassLoader().getResourceAsStream("static/owl/culture.owl");
                if (in == null) {
                    System.err.println("[ContextListener] 错误: 未找到culture.owl文件！");
                    return;
                }
                ontModel.read(in, null);
                System.out.println("[ContextListener] OWL文件加载完成，模型大小: " + ontModel.size());

                dataset = TDBUtil.getDataSet();
                dataset.begin(ReadWrite.WRITE);

                if (dataset.containsNamedModel(TDBUtil.defaultNamedModel)) {
                    System.out.println("[ContextListener] TDB中已有culture NamedGraph");
                } else {
                    System.out.println("[ContextListener] 添加culture NamedGraph，模型大小: " + ontModel.size());
                    dataset.addNamedModel(TDBUtil.defaultNamedModel, ontModel);
                }

                if (dataset.containsNamedModel(TDBUtil.inferredNamedModel)) {
                    System.out.println("[ContextListener] TDB中已有cultureInferred NamedGraph");
                } else {
                    List<Rule> rules = null;
                    try {
                        InputStream rulesStream = getClass().getClassLoader().getResourceAsStream("static/rules/xiangshan.rules");
                        if (rulesStream != null) {
                            Reader reader = new InputStreamReader(rulesStream, "UTF-8");
                            BufferedReader br = new BufferedReader(reader);
                            rules = Rule.parseRules(Rule.rulesParserFromReader(br));
                            System.out.println("[ContextListener] 推理规则加载完成");
                        } else {
                            System.err.println("[ContextListener] 警告: 未找到xiangshan.rules规则文件");
                        }
                    } catch (IOException e) {
                        System.err.println("[ContextListener] 读取规则文件异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    if (rules != null) {
                        Reasoner reasoner = new GenericRuleReasoner(rules);
                        InfModel inf = ModelFactory.createInfModel(reasoner, ontModel);
                        Model infModel = inf.getDeductionsModel();
                        Iterator<Statement> deduct = infModel.listStatements();
                        while (deduct.hasNext()) {
                            ontModel.add(deduct.next());
                        }
                        System.out.println("[ContextListener] 推理完成，模型大小: " + ontModel.size());
                        dataset.addNamedModel(TDBUtil.inferredNamedModel, ontModel);
                    } else {
                        System.out.println("[ContextListener] 跳过推理，直接使用原始模型");
                        dataset.addNamedModel(TDBUtil.inferredNamedModel, ontModel);
                    }
                }

                dataset.commit();
                dataset.end();
                System.out.println("[ContextListener] TDB初始化完成！");
            } catch (Exception e) {
                System.err.println("[ContextListener] TDB初始化异常: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (ontModel != null) {
                    ontModel.close();
                }
                if (dataset != null) {
                    dataset.close();
                }
            }
        } else {
            System.out.println("[ContextListener] 使用已创建的TDB");
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        TDBUtil.close(TDBUtil.getDataSet());
    }

}
