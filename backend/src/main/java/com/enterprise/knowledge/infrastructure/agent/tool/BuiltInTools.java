package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BuiltInTools {

    public String calculate(String expression) {
        try {
            expression = expression.replace("^", "**");
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager().getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return "计算结果: " + result;
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    public String getWeather(String city) {
        if (city == null || city.isEmpty()) {
            city = "北京";
        }
        return "【" + city + "天气预报】\n温度: 25°C\n天气: 晴\n风力: 微风\n空气质量: 优";
    }

    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "当前时间: " + now.format(formatter);
    }

    public String getCompanyPolicy(String topic) {
        if (topic == null || topic.isEmpty()) {
            return "请指定查询的制度主题，例如：年假、考勤、报销等";
        }

        return switch (topic.toLowerCase()) {
            case "年假", "年休" -> "【年假制度】\n- 工作满1年可享受5天年假\n- 工作满3年可享受10天年假\n- 工作满5年可享受15天年假\n- 年假需提前1周申请";
            case "考勤", "打卡" -> "【考勤制度】\n- 上班时间: 9:00-18:00\n- 午休时间: 12:00-13:30\n- 迟到15分钟内不计迟到\n- 每月允许3次迟到机会";
            case "报销", "费用" -> "【报销流程】\n1. 填写报销申请单\n2. 附上相关发票\n3. 部门主管审批\n4. 财务审核付款\n- 报销周期: 每周二、周五处理";
            case "加班" -> "【加班政策】\n- 工作日加班按1.5倍计算\n- 周末加班按2倍计算\n- 法定节假日按3倍计算\n- 加班需提前申请审批";
            case "请假" -> "【请假流程】\n1. 通过OA系统提交申请\n2. 直属上级审批\n3. 超过3天需部门经理审批\n4. 请假期间做好工作交接";
            default -> "暂无关于「" + topic + "」的具体制度信息，请联系人力资源部咨询";
        };
    }

    public String getAvailableTools() {
        return """
            可用工具列表:
            - calculator: 数学计算器，参数: expression
            - weather: 天气查询，参数: city
            - datetime: 获取当前时间
            - company_policy: 公司制度查询，参数: topic
            """;
    }
}