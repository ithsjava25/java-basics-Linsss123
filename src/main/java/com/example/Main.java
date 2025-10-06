package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        if (args.length == 0) {
            printHelp();
        }

        for (String a : args) {
            if ("--help".equals(a)) {
                printHelp();
            }
        }
        String zone = "";
        String date = LocalDate.now().toString();
        String charging = "";
        boolean sorted = false;


        for (int i = 0; i < args.length - 1; i++) {
            if ("--zone".equals(args[i])) {
                zone = args[i + 1];
            }
        }
        if (zone.isEmpty()) {
            System.out.println("Zone required");
        }
        else if (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
            System.out.println("Invalid zone. Use SE1|SE2|SE3|SE4");
        }

        for (int i = 0; i < args.length - 1; i++) {
            if ("--date".equals(args[i])) {
                date = args[i + 1];
            }
        }

        boolean validDate = checkValidDate(date);
        if (!validDate) {
            System.out.println("Invalid date format. Use YYYY-MM-DD");
        }

        for (int i = 0; i < args.length - 1; i++) {
            if ("--sorted".equals(args[i])) {
                sorted = true;
            }
        }
        for (int i = 0; i < args.length - 1; i++) {
            if ("--charging".equals(args[i])) {
                charging = args[i + 1];
            }
        }

        System.out.println("Zone: " + zone);
        System.out.println("Date: " + date);
        System.out.println("Charging: " + charging);
        System.out.println("Sorted: " + sorted);


    }



        /*System.out.println(elpriserAPI.getPriser("2025-10-06", SE1));
        var stats = elpriserAPI.getPriser("2025-10-06", SE1).stream()
                .mapToDouble(ElpriserAPI.Elpris::sekPerKWh)
                .summaryStatistics();
        double sumSekPerKWh = elpriserAPI.getPriser("2025-10-06", SE1).stream()
                .mapToDouble(ElpriserAPI.Elpris::sekPerKWh)
                .sum();
        System.out.println("Summa SEK/kWh: " + sumSekPerKWh / stats.getCount());*/


    private static void printHelp() {
        System.out.println("Usage information");
        System.out.println("  --zone      SE1|SE2|SE3|SE4 (krävs)");
        System.out.println("  --date      YYYY-MM-DD (valfritt, default: idag)");
        System.out.println("  --sorted    Visa priser i fallande ordning (valfritt)");
        System.out.println("  --charging  2h|4h|8h för optimalt laddningsfönster (valfritt)");
        System.out.println("  --help      Visa denna hjälp");

    }
    public static boolean checkValidDate(String s) {
        try {
            LocalDate.parse(s); // ISO-8601: YYYY-MM-DD
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}


