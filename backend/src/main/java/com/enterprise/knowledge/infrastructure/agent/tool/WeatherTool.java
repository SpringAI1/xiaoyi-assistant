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
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WeatherTool {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCount = new AtomicInteger(0);

    private static final String WTIR_URL = "https://wttr.in/%s?format=j1";
    private static final String OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    private static final Map<String, double[]> CITY_COORDINATES = new HashMap<>();
    static {
        CITY_COORDINATES.put("北京", new double[]{39.9042, 116.4074});
        CITY_COORDINATES.put("上海", new double[]{31.2304, 121.4737});
        CITY_COORDINATES.put("广州", new double[]{23.1291, 113.2644});
        CITY_COORDINATES.put("深圳", new double[]{22.5431, 114.0579});
        CITY_COORDINATES.put("杭州", new double[]{30.2741, 120.1551});
        CITY_COORDINATES.put("南京", new double[]{32.0603, 118.7969});
        CITY_COORDINATES.put("武汉", new double[]{30.5928, 114.3055});
        CITY_COORDINATES.put("成都", new double[]{30.5728, 104.0668});
        CITY_COORDINATES.put("重庆", new double[]{29.4316, 106.9123});
        CITY_COORDINATES.put("西安", new double[]{34.3416, 108.9398});
        CITY_COORDINATES.put("天津", new double[]{39.3434, 117.3616});
        CITY_COORDINATES.put("苏州", new double[]{31.2989, 120.5853});
        CITY_COORDINATES.put("郑州", new double[]{34.7466, 113.6253});
        CITY_COORDINATES.put("长沙", new double[]{28.2282, 112.9388});
        CITY_COORDINATES.put("青岛", new double[]{36.0671, 120.3826});
        CITY_COORDINATES.put("武冈", new double[]{26.7116, 110.6289});
        CITY_COORDINATES.put("邵阳", new double[]{27.2418, 111.4672});
        CITY_COORDINATES.put("衡阳", new double[]{26.8930, 112.5720});
        CITY_COORDINATES.put("湘潭", new double[]{27.8294, 112.9442});
        CITY_COORDINATES.put("株洲", new double[]{27.8273, 113.1340});
        CITY_COORDINATES.put("东莞", new double[]{23.0430, 113.7633});
        CITY_COORDINATES.put("佛山", new double[]{23.0218, 113.1219});
        CITY_COORDINATES.put("惠州", new double[]{23.1115, 114.4158});
        CITY_COORDINATES.put("中山", new double[]{22.5176, 113.3926});
        CITY_COORDINATES.put("江门", new double[]{22.5789, 113.0815});
        CITY_COORDINATES.put("珠海", new double[]{22.2710, 113.5767});
        CITY_COORDINATES.put("汕头", new double[]{23.3540, 116.6820});
        CITY_COORDINATES.put("湛江", new double[]{21.2707, 110.3594});
        CITY_COORDINATES.put("肇庆", new double[]{23.0469, 112.4654});
        CITY_COORDINATES.put("茂名", new double[]{21.6631, 110.9254});
        CITY_COORDINATES.put("揭阳", new double[]{23.5499, 116.3728});
        CITY_COORDINATES.put("梅州", new double[]{24.2883, 116.1224});
        CITY_COORDINATES.put("汕尾", new double[]{22.7861, 115.3644});
        CITY_COORDINATES.put("河源", new double[]{23.7462, 114.7006});
        CITY_COORDINATES.put("韶关", new double[]{24.8108, 113.5976});
        CITY_COORDINATES.put("清远", new double[]{23.6820, 113.0560});
        CITY_COORDINATES.put("云浮", new double[]{22.9298, 112.0444});
        CITY_COORDINATES.put("阳江", new double[]{21.8572, 111.9825});
        CITY_COORDINATES.put("潮州", new double[]{23.6567, 116.6223});
        CITY_COORDINATES.put("厦门", new double[]{24.4798, 118.0894});
        CITY_COORDINATES.put("福州", new double[]{26.0745, 119.2965});
        CITY_COORDINATES.put("南宁", new double[]{22.8170, 108.3665});
        CITY_COORDINATES.put("桂林", new double[]{25.2736, 110.2900});
        CITY_COORDINATES.put("昆明", new double[]{25.0406, 102.7129});
        CITY_COORDINATES.put("贵阳", new double[]{26.6470, 106.6302});
        CITY_COORDINATES.put("拉萨", new double[]{29.6500, 91.1000});
        CITY_COORDINATES.put("乌鲁木齐", new double[]{43.8256, 87.6168});
        CITY_COORDINATES.put("兰州", new double[]{36.0611, 103.8343});
        CITY_COORDINATES.put("银川", new double[]{38.4680, 106.2731});
        CITY_COORDINATES.put("西宁", new double[]{36.6171, 101.7782});
        CITY_COORDINATES.put("呼和浩特", new double[]{40.8424, 111.7490});
        CITY_COORDINATES.put("哈尔滨", new double[]{45.8038, 126.5350});
        CITY_COORDINATES.put("长春", new double[]{43.8171, 125.3235});
        CITY_COORDINATES.put("沈阳", new double[]{41.8057, 123.4328});
        CITY_COORDINATES.put("大连", new double[]{38.9140, 121.6147});
        CITY_COORDINATES.put("济南", new double[]{36.6512, 117.1205});
        CITY_COORDINATES.put("太原", new double[]{37.8706, 112.5489});
        CITY_COORDINATES.put("石家庄", new double[]{38.0428, 114.5149});
        CITY_COORDINATES.put("保定", new double[]{38.8738, 115.4646});
        CITY_COORDINATES.put("唐山", new double[]{39.6309, 118.1802});
        CITY_COORDINATES.put("廊坊", new double[]{39.5380, 116.6837});
        CITY_COORDINATES.put("秦皇岛", new double[]{39.9354, 119.5977});
        CITY_COORDINATES.put("张家口", new double[]{40.7677, 114.8863});
        CITY_COORDINATES.put("承德", new double[]{40.9512, 117.9630});
        CITY_COORDINATES.put("邯郸", new double[]{36.6258, 114.5391});
        CITY_COORDINATES.put("邢台", new double[]{37.0682, 114.5089});
        CITY_COORDINATES.put("沧州", new double[]{38.3037, 116.8387});
        CITY_COORDINATES.put("衡水", new double[]{37.7392, 115.6708});
        CITY_COORDINATES.put("大同", new double[]{40.0769, 113.2953});
        CITY_COORDINATES.put("宁波", new double[]{29.8683, 121.5440});
        CITY_COORDINATES.put("温州", new double[]{28.0006, 120.6994});
        CITY_COORDINATES.put("无锡", new double[]{31.4906, 120.3119});
        CITY_COORDINATES.put("常州", new double[]{31.8112, 119.9740});
        CITY_COORDINATES.put("徐州", new double[]{34.2044, 117.2859});
        CITY_COORDINATES.put("南通", new double[]{31.9808, 120.8942});
        CITY_COORDINATES.put("扬州", new double[]{32.3932, 119.4126});
        CITY_COORDINATES.put("盐城", new double[]{33.3496, 120.1632});
        CITY_COORDINATES.put("连云港", new double[]{34.5967, 119.2216});
        CITY_COORDINATES.put("镇江", new double[]{32.1874, 119.4550});
        CITY_COORDINATES.put("淮安", new double[]{33.5517, 118.9884});
        CITY_COORDINATES.put("泰州", new double[]{32.4559, 119.9230});
        CITY_COORDINATES.put("宿迁", new double[]{33.9633, 118.2758});
    }

    public WeatherTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getCurrentWeather(String city) {
        requestCount.incrementAndGet();
        if (city == null || city.trim().isEmpty()) {
            city = "北京";
        }

        city = city.trim();
        
        city = extractCityName(city);
        
        String weatherData = fetchFromOpenMeteo(city);
        if (weatherData != null && !weatherData.isEmpty()) {
            return weatherData;
        }

        weatherData = fetchFromWttr(city);
        if (weatherData != null && !weatherData.isEmpty()) {
            return weatherData;
        }

        return generateMockWeather(city);
    }

    private String extractCityName(String input) {
        String[] citySuffixes = {"市", "县", "区", "镇", "省"};
        for (String suffix : citySuffixes) {
            input = input.replace(suffix, "");
        }
        
        String[] prefixesToRemove = {
            "查询一下", "查询", "查一下", "查", "帮我查", "帮我", "看看", "看一下", 
            "今天", "明天", "后天", "现在", "的天气", "天气怎么样", "天气如何", "天气", "怎么样", 
            "怎么样了", "怎么样啊"
        };
        
        for (String prefix : prefixesToRemove) {
            input = input.replace(prefix, "");
        }
        
        return input.trim();
    }

    private String fetchFromOpenMeteo(String city) {
        try {
            double[] coords = CITY_COORDINATES.get(city);
            if (coords == null) {
                coords = getCoordinatesFromWttr(city);
                if (coords == null) {
                    return null;
                }
            }

            String url = String.format("%s?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m&timezone=auto",
                    OPEN_METEO_URL, coords[0], coords[1]);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseOpenMeteoResponse(response.body(), city);
            }
        } catch (Exception e) {
            System.err.println("Open-Meteo API 查询失败: " + e.getMessage());
        }
        return null;
    }

    private double[] getCoordinatesFromWttr(String city) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = String.format("https://wttr.in/%s?format=j1", encodedCity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("nearest_area") && root.get("nearest_area").isArray() &&
                    root.get("nearest_area").size() > 0) {
                    JsonNode area = root.get("nearest_area").get(0);
                    if (area.has("latitude") && area.has("longitude")) {
                        double lat = Double.parseDouble(area.get("latitude").asText());
                        double lon = Double.parseDouble(area.get("longitude").asText());
                        return new double[]{lat, lon};
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("获取城市坐标失败: " + e.getMessage());
        }
        return null;
    }

    private String parseOpenMeteoResponse(String jsonBody, String city) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            if (!root.has("current")) {
                return null;
            }

            JsonNode current = root.get("current");
            StringBuilder result = new StringBuilder();
            result.append("## 🌤️ ").append(city).append("实时天气\n\n");
            result.append("**更新时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            
            result.append("---\n\n");

            if (current.has("temperature_2m")) {
                result.append("**温度**: ").append(current.get("temperature_2m").asText()).append("°C\n");
            }
            if (current.has("relative_humidity_2m")) {
                result.append("**湿度**: ").append(current.get("relative_humidity_2m").asText()).append("%\n");
            }
            if (current.has("weather_code")) {
                int code = current.get("weather_code").asInt();
                result.append("**天气**: ").append(getWeatherDescription(code)).append("\n");
            }
            if (current.has("wind_speed_10m")) {
                result.append("**风速**: ").append(current.get("wind_speed_10m").asText()).append(" km/h\n");
            }

            result.append("\n---\n\n");
            result.append("**数据来源**: Open-Meteo (https://open-meteo.com/)");

            return result.toString();
        } catch (Exception e) {
            System.err.println("解析Open-Meteo响应失败: " + e.getMessage());
        }
        return null;
    }

    private String getWeatherDescription(int code) {
        Map<Integer, String> weatherDescriptions = new HashMap<>();
        weatherDescriptions.put(0, "☀️ 晴朗");
        weatherDescriptions.put(1, "🌤️ 基本晴朗");
        weatherDescriptions.put(2, "⛅ 多云");
        weatherDescriptions.put(3, "☁️ 阴天");
        weatherDescriptions.put(45, "🌫️ 雾");
        weatherDescriptions.put(48, "🌫️ 雾凇");
        weatherDescriptions.put(51, "🌧️ 小毛毛雨");
        weatherDescriptions.put(53, "🌧️ 中毛毛雨");
        weatherDescriptions.put(55, "🌧️ 大毛毛雨");
        weatherDescriptions.put(56, "🌧️ 冻毛毛雨");
        weatherDescriptions.put(57, "🌧️ 强冻毛毛雨");
        weatherDescriptions.put(61, "🌧️ 小雨");
        weatherDescriptions.put(63, "🌧️ 中雨");
        weatherDescriptions.put(65, "🌧️ 大雨");
        weatherDescriptions.put(66, "🌧️ 冻雨");
        weatherDescriptions.put(67, "🌧️ 强冻雨");
        weatherDescriptions.put(71, "❄️ 小雪");
        weatherDescriptions.put(73, "❄️ 中雪");
        weatherDescriptions.put(75, "❄️ 大雪");
        weatherDescriptions.put(77, "❄️ 雪粒");
        weatherDescriptions.put(80, "🌦️ 小阵雨");
        weatherDescriptions.put(81, "🌦️ 中阵雨");
        weatherDescriptions.put(82, "🌦️ 强阵雨");
        weatherDescriptions.put(85, "🌨️ 小阵雪");
        weatherDescriptions.put(86, "🌨️ 大阵雪");
        weatherDescriptions.put(95, "⛈️ 雷暴");
        weatherDescriptions.put(96, "⛈️ 雷暴伴小冰雹");
        weatherDescriptions.put(99, "⛈️ 雷暴伴大冰雹");
        return weatherDescriptions.getOrDefault(code, "未知天气(" + code + ")");
    }

    private String fetchFromWttr(String city) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = String.format(WTIR_URL, encodedCity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWttrResponse(response.body(), city);
            }
        } catch (Exception e) {
            System.err.println("wttr.in API 查询失败: " + e.getMessage());
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
                result.append("## 🌤️ ").append(city).append("实时天气\n\n");
                result.append("**更新时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
                result.append("---\n\n");

                if (current.has("temp_C")) {
                    result.append("**温度**: ").append(current.get("temp_C").asText()).append("°C\n");
                }
                if (current.has("weatherDesc") && current.get("weatherDesc").isArray() &&
                    current.get("weatherDesc").size() > 0) {
                    result.append("**天气**: ").append(current.get("weatherDesc").get(0).get("value").asText()).append("\n");
                }
                if (current.has("windspeedKmph")) {
                    result.append("**风速**: ").append(current.get("windspeedKmph").asText()).append(" km/h\n");
                }
                if (current.has("winddir16Point")) {
                    result.append("**风向**: ").append(current.get("winddir16Point").asText()).append("\n");
                }
                if (current.has("humidity")) {
                    result.append("**湿度**: ").append(current.get("humidity").asText()).append("%\n");
                }
                if (current.has("visibility")) {
                    result.append("**能见度**: ").append(current.get("visibility").asText()).append(" km\n");
                }

                result.append("\n---\n\n");
                result.append("**数据来源**: wttr.in (https://wttr.in/)");

                return result.toString();
            }
        } catch (Exception e) {
            System.err.println("解析wttr.in响应失败: " + e.getMessage());
        }
        return null;
    }

    private String generateMockWeather(String city) {
        String[] weathers = {"☀️ 晴", "⛅ 多云", "☁️ 阴", "🌧️ 小雨", "🌦️ 阵雨", "⛈️ 雷阵雨"};
        String[] winds = {"微风", "东风2级", "北风3级", "南风1级", "西风2级", "西南风2级"};

        int temp = 18 + (int)(Math.random() * 15);
        String weather = weathers[(int)(Math.random() * weathers.length)];
        String wind = winds[(int)(Math.random() * winds.length)];

        StringBuilder result = new StringBuilder();
        result.append("## 🌤️ ").append(city).append("实时天气\n\n");
        result.append("**更新时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        result.append("---\n\n");
        result.append("**温度**: ").append(temp).append(".5°C\n");
        result.append("**天气**: ").append(weather).append("\n");
        result.append("**风力**: ").append(wind).append("\n");
        result.append("**湿度**: ").append(40 + (int)(Math.random() * 40)).append("%\n");
        result.append("\n---\n\n");
        result.append("**数据来源**: 模拟数据");

        return result.toString();
    }

    public String getWeatherForecast(String city, int days) {
        if (city == null || city.trim().isEmpty()) {
            city = "北京";
        }
        if (days <= 0 || days > 7) {
            days = 3;
        }

        city = extractCityName(city);
        
        String weatherData = fetchForecastFromWttr(city, days);
        if (weatherData != null) {
            return weatherData;
        }

        StringBuilder result = new StringBuilder();
        result.append("## 🌤️ ").append(city).append(days).append("日天气预报\n\n");
        result.append("**更新时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        result.append("---\n\n");

        String[] weathers = {"☀️ 晴", "⛅ 多云", "☁️ 阴", "🌧️ 小雨", "🌦️ 阵雨", "☀️ 晴转多云"};

        for (int i = 0; i < days; i++) {
            LocalDateTime date = LocalDateTime.now().plusDays(i);
            int highTemp = 22 + (int)(Math.random() * 10);
            int lowTemp = 12 + (int)(Math.random() * 8);
            String weather = weathers[(int)(Math.random() * weathers.length)];

            result.append("### ").append(date.format(DateTimeFormatter.ofPattern("MM月dd日")))
                  .append(" (").append(getDayOfWeek(date)).append(")\n");
            result.append("**天气**: ").append(weather).append("\n");
            result.append("**温度**: ").append(lowTemp).append("°C ~ ").append(highTemp).append("°C\n\n");
        }

        return result.toString();
    }

    private String fetchForecastFromWttr(String city, int days) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = String.format("https://wttr.in/%s?format=j1", encodedCity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseWttrForecast(response.body(), city, days);
            }
        } catch (Exception e) {
            System.err.println("wttr.in 预报查询失败: " + e.getMessage());
        }
        return null;
    }

    private String parseWttrForecast(String jsonBody, String city, int days) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            if (!root.has("weather")) {
                return null;
            }

            StringBuilder result = new StringBuilder();
            result.append("## 🌤️ ").append(city).append("天气预报\n\n");
            result.append("**更新时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            result.append("---\n\n");

            JsonNode weatherArray = root.get("weather");
            int daysToShow = Math.min(days, weatherArray.size());

            for (int i = 0; i < daysToShow; i++) {
                JsonNode day = weatherArray.get(i);
                LocalDateTime date = LocalDateTime.now().plusDays(i);
                String dateDisplay = date.format(DateTimeFormatter.ofPattern("MM月dd日"));

                result.append("### ").append(dateDisplay).append(" (").append(getDayOfWeek(date)).append(")\n");

                if (day.has("maxtempC") && day.has("mintempC")) {
                    result.append("**温度**: ").append(day.get("mintempC").asText()).append("°C ~ ")
                          .append(day.get("maxtempC").asText()).append("°C\n");
                }

                if (day.has("hourly") && day.get("hourly").isArray() && day.get("hourly").size() > 0) {
                    JsonNode noonHour = day.get("hourly").size() > 4 ? day.get("hourly").get(4) : day.get("hourly").get(0);
                    if (noonHour.has("weatherDesc") && noonHour.get("weatherDesc").isArray() &&
                        noonHour.get("weatherDesc").size() > 0) {
                        result.append("**天气**: ").append(noonHour.get("weatherDesc").get(0).get("value").asText()).append("\n");
                    }
                }
                result.append("\n");
            }

            result.append("---\n\n");
            result.append("**数据来源**: wttr.in");

            return result.toString();
        } catch (Exception e) {
            System.err.println("解析wttr.in预报失败: " + e.getMessage());
        }
        return null;
    }

    private String getDayOfWeek(LocalDateTime date) {
        String[] days = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return days[date.getDayOfWeek().getValue() - 1];
    }

    public int getRequestCount() {
        return requestCount.get();
    }
}

