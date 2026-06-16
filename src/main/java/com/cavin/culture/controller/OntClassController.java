package com.cavin.culture.controller;

import com.cavin.culture.bean.JsonMessage;
import com.cavin.culture.service.OntClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Controller
public class OntClassController {

    @Autowired
    private OntClassService ontClassService;

    @RequestMapping(value = "/getRootClasses", method = RequestMethod.GET)
    @ResponseBody
    public JsonMessage getRootClasses() {
        System.out.println("[OntClassController] 收到getRootClasses请求");
        try {
            List<Map<String, Object>> list = ontClassService.getRootClasses();
            System.out.println("[OntClassController] 获取根类数量: " + (list != null ? list.size() : "null"));
            return JsonMessage.success().addData("rootClasses", list);
        } catch (Exception e) {
            System.err.println("[OntClassController] 获取根类异常: " + e.getMessage());
            e.printStackTrace();
            return JsonMessage.error(500, "获取根类失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/getSubClasses", method = RequestMethod.GET)
    @ResponseBody
    public JsonMessage getSubClasses(@RequestParam(value = "className")String className) {
        List<Map<String, Object>> list = ontClassService.getSubClasses(className);
        return JsonMessage.success().addData("subClasses", list);
    }

    @RequestMapping(value = "/getSuperClasses", method = RequestMethod.GET)
    @ResponseBody
    public JsonMessage getSuperClasses(@RequestParam(value = "className")String className) {
        List<String> list = ontClassService.getSuperClasses(className);
        return JsonMessage.success().addData("superClasses", list);
    }

}
