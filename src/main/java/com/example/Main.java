package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.api.ElpriserAPI.Prisklass.SE1;
import static com.example.api.ElpriserAPI.Prisklass.SE2;
import static com.example.api.ElpriserAPI.Prisklass.SE3;
import static com.example.api.ElpriserAPI.Prisklass.SE4;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        //Printar hjälp om inga kommandon skickas in
        if (args.length == 0) {
            printHelp();
        }

        //Printar hjälp om "--help" kommandot skickas in
        for (String a : args) {
            if ("--help".equals(a)) {
                printHelp();
            }
        }


        //Initierar variabler
        String zone = "";
        String date = LocalDate.now().toString();
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String chargingSpan = "";
        boolean sorted = false;



        //Kollar vilken zon som skickas in och om den är giltig
        for (int i = 0; i < args.length - 1; i++) {
            if ("--zone".equals(args[i])) {
                zone = args[i + 1];
            }
        }
        if (zone.isEmpty()) {
            System.out.println("Zone required");
        } else if (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
            System.out.println("Invalid zone. Use SE1|SE2|SE3|SE4");
        }

        ElpriserAPI.Prisklass zoneNumber = zone.equals("SE1") ? SE1 : zone.equals("SE2") ? SE2 : zone.equals("SE3") ? SE3 : zone.equals("SE4") ? SE4 : null;

        //Kollar datum som skickas in och om det är giltigt
        for (int i = 0; i < args.length - 1; i++) {
            if ("--date".equals(args[i])) {
                date = args[i + 1];
            }
        }


        boolean validDate = checkValidDate(date);
        if (!validDate) {
            System.out.println("Invalid date format. Use YYYY-MM-DD");
        }

        //Kollar om sorted kommandot skickas in
        for (String a : args) {
            if ("--sorted".equals(a)) {
                sorted = true;
            }
        }

        //Kollar om charging kommandot skickas in och vilket span som ska kollas
        for (int i = 0; i < args.length - 1; i++) {
            if ("--charging".equals(args[i])) {
                chargingSpan = args[i + 1];
            }
        }


        //Läser in priser för valt datum och zon
        var priser = elpriserAPI.getPriser(date, zoneNumber);
        //String tomorrowDate = LocalDate.parse(date).plusDays(1).toString();
        //var imorgonPriser = elpriserAPI.getPriser(tomorrowDate, zoneNumber);

        if (priser == null) {
            priser = List.of();
        }

        //Räknar ihop summan av alla priser
        double sumSekPerKWh = priser.stream()
                .mapToDouble(ElpriserAPI.Elpris::sekPerKWh)
                .sum();

        //Läser in hur många priser som läses in för att kunna dela och få medelvärde
        var stats = priser.stream()
                .mapToDouble(ElpriserAPI.Elpris::sekPerKWh)
                .summaryStatistics();

        //Kollar om första tiden är efter 13 för att läsa in nästa dag
        boolean after13 = !priser.isEmpty() && priser.get(0).timeStart().getHour() > 13;

        //Setup för utskriftsformat
        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH.mm");
        DateTimeFormatter hh = DateTimeFormatter.ofPattern("HH");
        DecimalFormatSymbols sv = new DecimalFormatSymbols(new Locale("sv", "SE"));
        DecimalFormat oreFmt = new DecimalFormat("0.00", sv);


        //Variabel för att spara medelpris per timme
        var medelPerTimme = priser.stream()
                .collect(Collectors.groupingBy(
                        p -> p.timeStart().getHour(),
                        Collectors.averagingDouble(ElpriserAPI.Elpris::sekPerKWh)
                ));

        //Lista av alla tider och priser
        List<String> tiderOchPriser = IntStream.range(0, 24)
                .filter(h -> medelPerTimme.containsKey(h))
                .mapToObj(h -> String.format("%02d-%02d %s öre",
                        h, (h + 1) % 24,
                        oreFmt.format(medelPerTimme.get(h) * 100.0)))
                .toList();

        //Hittar och skriver ut högst pris
        var maxPris = medelPerTimme.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        if (maxPris != null) {
            System.out.printf("Högsta pris %02d-%02d %s öre%n",
                    maxPris.getKey(), (maxPris.getKey() + 1) % 24,
                    oreFmt.format(maxPris.getValue() * 100.0));
        }

        //Hittar och skriver ut minst pris
        var minPris = medelPerTimme.entrySet().stream()
                .min(java.util.Map.Entry.comparingByValue())
                .orElse(null);
        if (minPris != null) {
            System.out.printf("Lägsta pris %02d-%02d %s öre%n",
                    minPris.getKey(), (minPris.getKey() + 1) % 24,
                    oreFmt.format(minPris.getValue() * 100.0));
        }

        //Sorterar priser och skriver ut dem i fallande ordning
        if (sorted) {
            List<String> tiderOchPriserSorterade = priser.stream()
                    .sorted(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed())
                    .map(p -> String.format("%s-%s %s öre",
                            p.timeStart().toLocalTime().format(hhmm),
                            p.timeEnd().toLocalTime().format(hhmm),
                            oreFmt.format(p.sekPerKWh() * 100.0))).toList();
            System.out.println(tiderOchPriserSorterade);
        }

        //Räknar ut medelvärdet på alla priser och skriver ut det
        double sumSekPerKWhToÖre = (sumSekPerKWh / stats.getCount()) * 100.0;
        System.out.printf("Medelpris: %.2f öre%n", sumSekPerKWhToÖre);
        if (priser.isEmpty())
            System.out.println("Ingen data");


        var allaPriser = new java.util.ArrayList<>(priser);
        if (zoneNumber != null && validDate) {
            var priserImorgon = elpriserAPI.getPriser(LocalDate.parse(date).plusDays(1), zoneNumber);
            if (priserImorgon != null) {
                var imorgonPriserLista = new java.util.ArrayList<>(priserImorgon);
                allaPriser.addAll(imorgonPriserLista);
            }
        }

        System.out.println(allaPriser);
        var timMap = allaPriser.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.timeStart().withMinute(0).withSecond(0).withNano(0),
                        java.util.stream.Collectors.averagingDouble(ElpriserAPI.Elpris::sekPerKWh)
                ));

        List<Double> hourly = timMap.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(java.util.Map.Entry::getValue)
                .toList();


        //Räknar ut bästa chargingspanet över 2, 4 och 8 timmar
        if (chargingSpan.equals("2h")) {
            findBestStart(hourly, 2);
        } else if (chargingSpan.equals("4h")) {
            findBestStart(hourly, 4);
        } else if (chargingSpan.equals("8h")) {
            findBestStart(hourly, 8);
        }


    }


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

    public static int findBestStart(java.util.List<Double> hourly, int h) {
        if (hourly == null || hourly.size() < h || h <= 0) return -1;

        double windowSum = 0.0;
        for (int i = 0; i < h; i++) windowSum += hourly.get(i);
        double minSum = windowSum;
        int bestStart = 0;

        for (int i = h; i < hourly.size(); i++) {
            windowSum += hourly.get(i) - hourly.get(i - h);
            if (windowSum < minSum) {
                minSum = windowSum;
                bestStart = i - h + 1;
            }
        }

        var sv = new java.text.DecimalFormatSymbols(new java.util.Locale("sv", "SE"));
        var oreFmt = new java.text.DecimalFormat("0.00", sv);
        double meanBest = minSum / h;

        System.out.printf("Påbörja laddning kl %02d:00%nMedelpris för fönster: %s öre%n",
                bestStart, oreFmt.format(meanBest * 100.0));
        return bestStart;
    }
}


