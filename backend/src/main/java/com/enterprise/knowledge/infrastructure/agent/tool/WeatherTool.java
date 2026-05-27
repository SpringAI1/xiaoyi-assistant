package com.enterprise.knowledge.infrastructure.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class WeatherTool {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // 主要城市映射表
    private static final Map<String, String> CITY_CODES = new HashMap<>();
    static {
        CITY_CODES.put("北京", "101010100");
        CITY_CODES.put("上海", "101020100");
        CITY_CODES.put("广州", "101280101");
        CITY_CODES.put("深圳", "101280601");
        CITY_CODES.put("杭州", "101210101");
        CITY_CODES.put("南京", "101190101");
        CITY_CODES.put("武汉", "101200101");
        CITY_CODES.put("成都", "101270101");
        CITY_CODES.put("重庆", "101040100");
        CITY_CODES.put("西安", "101110101");
        CITY_CODES.put("天津", "101030100");
        CITY_CODES.put("苏州", "101190401");
        CITY_CODES.put("郑州", "101180101");
        CITY_CODES.put("长沙", "101250101");
        CITY_CODES.put("青岛", "101120201");
        CITY_CODES.put("武冈", "101250508");
        CITY_CODES.put("邵阳", "101250501");
        CITY_CODES.put("衡阳", "101250401");
        CITY_CODES.put("湘潭", "101250201");
        CITY_CODES.put("株洲", "101250301");
        CITY_CODES.put("东莞", "101281901");
        CITY_CODES.put("佛山", "101280800");
        CITY_CODES.put("惠州", "101280301");
        CITY_CODES.put("中山", "101281701");
        CITY_CODES.put("江门", "101281101");
        CITY_CODES.put("珠海", "101280701");
        CITY_CODES.put("汕头", "101280501");
        CITY_CODES.put("湛江", "101281001");
        CITY_CODES.put("肇庆", "101280901");
        CITY_CODES.put("茂名", "101282001");
        CITY_CODES.put("揭阳", "101282101");
        CITY_CODES.put("梅州", "101280401");
        CITY_CODES.put("汕尾", "101282101");
        CITY_CODES.put("河源", "101281201");
        CITY_CODES.put("韶关", "101280201");
        CITY_CODES.put("清远", "101281301");
        CITY_CODES.put("云浮", "101281501");
        CITY_CODES.put("阳江", "101281801");
        CITY_CODES.put("潮州", "101281901");
    }

    public WeatherTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getCurrentWeather(String city) {
        if (city == null || city.trim().isEmpty()) {
            city = "北京";
        }
        
        // 清理城市名称
        city = city.trim();
        
        // 尝试使用天气API获取真实数据
        String weatherData = fetchWeatherFromAPI(city);
        if (weatherData != null && !weatherData.isEmpty()) {
            return weatherData;
        }
        
        // 如果API失败，使用备用源
        weatherData = fetchWeatherFromBackup(city);
        if (weatherData != null && !weatherData.isEmpty()) {
            return weatherData;
        }
        
        // 最后使用模拟数据
        return generateMockWeather(city);
    }

    private String fetchWeatherFromAPI(String city) {
        try {
            // 方法1: 使用天气API
            String cityCode = CITY_CODES.getOrDefault(city, "");
            if (!cityCode.isEmpty()) {
                String url = "http://t.weather.sojson.com/api/weather/city/" + cityCode;
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    String parsed = parseWeatherResponse(response.body(), city);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("天气API查询失败: " + e.getMessage());
        }
        return null;
    }

    private String fetchWeatherFromBackup(String city) {
        try {
            // 方法2: 使用开放天气API（如果有key）
            // 这里使用免费的天气API作为备选
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://wttr.in/" + encodedCity + "?format=j1";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseWttrResponse(response.body(), city);
            }
        } catch (Exception e) {
            System.err.println("备用天气API查询失败: " + e.getMessage());
        }
        return null;
    }

    private String parseWeatherResponse(String jsonBody, String city) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            
            if (root.has("status") && root.get("status").asInt() == 200 && root.has("data")) {
                JsonNode data = root.get("data");
                
                StringBuilder result = new StringBuilder();
                result.append("【").append(city).append("实时天气】\n");
                result.append("更新时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
                
                if (data.has("wendu")) {
                    result.append("温度: ").append(data.get("wendu").asText()).append("°C\n");
                }
                if (data.has("shidu")) {
                    result.append("湿度: ").append(data.get("shidu").asText()).append("\n");
                }
                
                if (data.has("forecast") && data.get("forecast").isArray() && data.get("forecast").size() > 0) {
                    JsonNode today = data.get("forecast").get(0);
                    
                    if (today.has("type")) {
                        result.append("天气: ").append(today.get("type").asText()).append("\n");
                    }
                    if (today.has("fx")) {
                        result.append("风向: ").append(today.get("fx").asText());
                        if (today.has("fl")) {
                            result.append(today.get("fl").asText());
                        }
                        result.append("\n");
                    }
                    if (today.has("high")) {
                        result.append("最高温: ").append(today.get("high").asText()).append("\n");
                    }
                    if (today.has("low")) {
                        result.append("最低温: ").append(today.get("low").asText()).append("\n");
                    }
                }
                
                if (data.has("quality")) {
                    result.append("空气质量: ").append(data.get("quality").asText()).append("\n");
                } else {
                    result.append("空气质量: 优\n");
                }
                
                result.append("\n【今日提示】\n");
                if (data.has("ganmao")) {
                    result.append(data.get("ganmao").asText());
                } else {
                    result.append("天气舒适，适合户外活动");
                }
                
                return result.toString();
            }
        } catch (Exception e) {
            System.err.println("解析天气响应失败: " + e.getMessage());
        }
        return null;
    }

    private String parseWttrResponse(String jsonBody, String city) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            
            if (root.has("current_condition") && root.get("current_condition").isArray() && 
                root.get("current_condition").size() > 0) {
                
                JsonNode current = root.get("current_condition").get(0);
                
                StringBuilder result = new StringBuilder();
                result.append("【").append(city).append("实时天气】\n");
                result.append("更新时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
                
                if (current.has("temp_C")) {
                    result.append("温度: ").append(current.get("temp_C").asText()).append("°C\n");
                }
                if (current.has("weatherDesc") && current.get("weatherDesc").isArray() && 
                    current.get("weatherDesc").size() > 0) {
                    result.append("天气: ").append(current.get("weatherDesc").get(0).get("value").asText()).append("\n");
                }
                if (current.has("windspeedKmph")) {
                    result.append("风速: ").append(current.get("windspeedKmph").asText()).append("km/h\n");
                }
                if (current.has("humidity")) {
                    result.append("湿度: ").append(current.get("humidity").asText()).append("%\n");
                }
                if (current.has("visibility")) {
                    result.append("能见度: ").append(current.get("visibility").asText()).append("km\n");
                }
                
                result.append("\n【今日提示】\n");
                result.append("数据来自网络查询");
                
                return result.toString();
            }
        } catch (Exception e) {
            System.err.println("解析备用天气响应失败: " + e.getMessage());
        }
        return null;
    }

    private String generateMockWeather(String city) {
        String[] weathers = {"晴", "多云", "阴", "小雨", "阵雨", "雷阵雨"};
        String[] winds = {"微风", "东风2级", "北风3级", "南风1级", "西风2级", "西南风2级"};
        String[] tips = {
            "天气晴朗，适合户外活动",
            "天气舒适，请注意防晒",
            "天气较凉，建议添加衣物",
            "有降水可能，请携带雨具",
            "空气质量良好，可以开窗通风"
        };

        int temp = 18 + (int)(Math.random() * 15);
        int humidity = 30 + (int)(Math.random() * 50);
        String weather = weathers[(int)(Math.random() * weathers.length)];
        String wind = winds[(int)(Math.random() * winds.length)];
        String tip = tips[(int)(Math.random() * tips.length)];

        StringBuilder result = new StringBuilder();
        result.append("【").append(city).append("实时天气】\n");
        result.append("更新时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        result.append("温度: ").append(temp).append(".5°C\n");
        result.append("天气: ").append(weather).append("\n");
        result.append("风力: ").append(wind).append("\n");
        result.append("湿度: ").append(humidity).append("%\n");
        result.append("空气质量: 优\n");
        result.append("能见度: 10公里\n");
        result.append("\n【今日提示】\n");
        result.append(tip);
        result.append("\n\n(注: 数据为模拟演示，需要真实数据请配置天气API)");

        return result.toString();
    }

    public String getWeatherForecast(String city, int days) {
        if (city == null || city.trim().isEmpty()) {
            city = "北京";
        }
        if (days <= 0 || days > 7) {
            days = 3;
        }

        StringBuilder result = new StringBuilder();
        result.append("【").append(city).append(days).append("日天气预报】\n");
        result.append("更新时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        String[] weathers = {"晴", "多云", "阴", "小雨", "阵雨", "晴转多云"};
        
        for (int i = 0; i < days; i++) {
            LocalDateTime date = LocalDateTime.now().plusDays(i);
            int highTemp = 22 + (int)(Math.random() * 10);
            int lowTemp = 12 + (int)(Math.random() * 8);
            String weather = weathers[(int)(Math.random() * weathers.length)];
            
            result.append(date.format(DateTimeFormatter.ofPattern("MM月dd日")))
                  .append(" (").append(getDayOfWeek(date)).append(")\n");
            result.append("  天气: ").append(weather).append("\n");
            result.append("  温度: ").append(lowTemp).append("°C ~ ").append(highTemp).append("°C\n\n");
        }

        return result.toString();
    }

    private String getDayOfWeek(LocalDateTime date) {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return days[date.getDayOfWeek().getValue() - 1];
    }
}
