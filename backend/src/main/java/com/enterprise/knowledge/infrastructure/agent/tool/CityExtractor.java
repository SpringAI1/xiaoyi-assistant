package com.enterprise.knowledge.infrastructure.agent.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CityExtractor {

    // 主要城市列表 - 从天气查询使用
    private static final Map<String, String> CHINESE_CITIES = new HashMap<>();
    
    static {
        // 直辖市
        CHINESE_CITIES.put("北京", "北京");
        CHINESE_CITIES.put("上海", "上海");
        CHINESE_CITIES.put("天津", "天津");
        CHINESE_CITIES.put("重庆", "重庆");
        
        // 广东省
        CHINESE_CITIES.put("广州", "广州");
        CHINESE_CITIES.put("深圳", "深圳");
        CHINESE_CITIES.put("东莞", "东莞");
        CHINESE_CITIES.put("佛山", "佛山");
        CHINESE_CITIES.put("惠州", "惠州");
        CHINESE_CITIES.put("中山", "中山");
        CHINESE_CITIES.put("江门", "江门");
        CHINESE_CITIES.put("珠海", "珠海");
        CHINESE_CITIES.put("汕头", "汕头");
        CHINESE_CITIES.put("湛江", "湛江");
        CHINESE_CITIES.put("肇庆", "肇庆");
        CHINESE_CITIES.put("茂名", "茂名");
        CHINESE_CITIES.put("揭阳", "揭阳");
        CHINESE_CITIES.put("梅州", "梅州");
        CHINESE_CITIES.put("汕尾", "汕尾");
        CHINESE_CITIES.put("河源", "河源");
        CHINESE_CITIES.put("韶关", "韶关");
        CHINESE_CITIES.put("清远", "清远");
        CHINESE_CITIES.put("云浮", "云浮");
        CHINESE_CITIES.put("阳江", "阳江");
        CHINESE_CITIES.put("潮州", "潮州");
        
        // 湖南省
        CHINESE_CITIES.put("长沙", "长沙");
        CHINESE_CITIES.put("株洲", "株洲");
        CHINESE_CITIES.put("湘潭", "湘潭");
        CHINESE_CITIES.put("衡阳", "衡阳");
        CHINESE_CITIES.put("邵阳", "邵阳");
        CHINESE_CITIES.put("武冈", "武冈");
        
        // 其他主要城市
        CHINESE_CITIES.put("杭州", "杭州");
        CHINESE_CITIES.put("南京", "南京");
        CHINESE_CITIES.put("苏州", "苏州");
        CHINESE_CITIES.put("武汉", "武汉");
        CHINESE_CITIES.put("成都", "成都");
        CHINESE_CITIES.put("西安", "西安");
        CHINESE_CITIES.put("郑州", "郑州");
        CHINESE_CITIES.put("青岛", "青岛");
        CHINESE_CITIES.put("大连", "大连");
        CHINESE_CITIES.put("沈阳", "沈阳");
        CHINESE_CITIES.put("哈尔滨", "哈尔滨");
        CHINESE_CITIES.put("长春", "长春");
        CHINESE_CITIES.put("济南", "济南");
        CHINESE_CITIES.put("石家庄", "石家庄");
        CHINESE_CITIES.put("太原", "太原");
        CHINESE_CITIES.put("合肥", "合肥");
        CHINESE_CITIES.put("福州", "福州");
        CHINESE_CITIES.put("厦门", "厦门");
        CHINESE_CITIES.put("南宁", "南宁");
        CHINESE_CITIES.put("海口", "海口");
        CHINESE_CITIES.put("三亚", "三亚");
        CHINESE_CITIES.put("贵阳", "贵阳");
        CHINESE_CITIES.put("昆明", "昆明");
        CHINESE_CITIES.put("南昌", "南昌");
        CHINESE_CITIES.put("赣州", "赣州");
    }

    public String extractCity(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        
        String cleanedQuery = query.trim();
        
        // 首先尝试完整匹配
        for (String city : CHINESE_CITIES.keySet()) {
            if (cleanedQuery.contains(city)) {
                return city;
            }
        }
        
        // 尝试更模糊匹配 - 检查是否包含城市名的一部分
        for (String city : CHINESE_CITIES.keySet()) {
            if (city.length() >= 2 && cleanedQuery.contains(city.substring(0, 2))) {
                return city;
            }
        }
        
        return null;
    }
}
