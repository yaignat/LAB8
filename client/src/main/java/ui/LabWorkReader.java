package ui;

import data.*;
import java.util.ArrayList;
import java.util.List;

public class LabWorkReader {

    // ... твой существующий код для консоли (readLabWorkFromConsole и т.д.) оставь без изменений ...

    public static List<LabWork> parseServerResponse(String response) {
        List<LabWork> list = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            return list;
        }

        if (response.contains("Ошибка аутентификации") ||
                response.contains("Неверный логин") ||
                response.startsWith("Ошибка")) {
            return list;
        }

        String[] objects = response.split("LabWork\\{");

        for (String objStr : objects) {
            if (objStr == null || objStr.trim().isEmpty()) continue;

            try {
                long id = extractLong(objStr, "id=", ",");
                String name = extractString(objStr, "name='", "'");
                if (name.isEmpty()) continue;

                String coordsStr = extractString(objStr, "coordinates={", "}");
                if (coordsStr.isEmpty()) continue;

                String[] coordsParts = coordsStr.split(",");
                double x = Double.parseDouble(coordsParts[0].trim());
                long y = Long.parseLong(coordsParts[1].trim());

                float minP = (float) extractDouble(objStr, "minimalPoint=", ",");
                double pqMax = extractDouble(objStr, "personalQualitiesMaximum=", ",");

                String diffStr = extractString(objStr, "difficulty=", ",");
                if (diffStr.isEmpty()) continue;
                Difficulty diff = Difficulty.valueOf(diffStr.trim().toUpperCase());

                String discStr = extractString(objStr, "discipline=", "}");
                String discName = discStr;
                Integer lectureHours = 0;

                if (discStr.contains("(") && discStr.contains("ч)")) {
                    int nameEnd = discStr.indexOf(" (");
                    if (nameEnd > 0) {
                        discName = discStr.substring(0, nameEnd).trim();
                        String hoursPart = discStr.substring(nameEnd + 2, discStr.indexOf("ч)")).trim();
                        try {
                            lectureHours = Integer.parseInt(hoursPart);
                        } catch (NumberFormatException e) {
                            lectureHours = 0;
                        }
                    }
                }

                int ownerId = 0;
                if (objStr.contains("owner=")) {
                    ownerId = (int) extractLong(objStr, "owner=", "}");
                }

                Coordinates coords = new Coordinates(x, y);
                Discipline disc = new Discipline(discName, lectureHours);
                LabWork lw = new LabWork(name, coords, minP, pqMax, diff, disc);

                lw.setId(id);
                lw.setOwnerId(ownerId);
                list.add(lw);

            } catch (Exception e) {
                // Игнорируем ошибки парсинга отдельных объектов
            }
        }
        return list;
    }

    private static String extractString(String src, String start, String end) {
        int s = src.indexOf(start);
        if (s == -1) return "";
        s += start.length();
        int e = src.indexOf(end, s);
        if (e == -1) e = src.length();
        return src.substring(s, e).trim();
    }

    private static double extractDouble(String src, String start, String end) {
        try {
            String val = extractString(src, start, end);
            return val.isEmpty() ? 0.0 : Double.parseDouble(val);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static long extractLong(String src, String start, String end) {
        try {
            String val = extractString(src, start, end);
            return val.isEmpty() ? 0L : Long.parseLong(val);
        } catch (Exception e) {
            return 0L;
        }
    }
}