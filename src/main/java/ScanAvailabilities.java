import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ScanAvailabilities {

    private boolean circuitBreak = true;

    public void setCircuitBreak(boolean circuitBreak) {
        this.circuitBreak = circuitBreak;
    }

    private static List<LocalDate> ignoredDays = new ArrayList<LocalDate>() {
        {
            //FERIE
            add(LocalDate.of(2021, 8, 16));
        }
    };

    public static String ULSS_6_EUGANEA = "641";
    public static String ULSS_3_SERENISSIMA = "178";

    public static HashMap<String, String> sedi_ulss_6 = new HashMap<String, String>() {
        {
            put("264", "Padova - Fiera (a) (Padiglione 8 - entrata da via Rismondo n.18 cancello M)");
        }
    };
    public static HashMap<String, String> sedi_ulss_3 = new HashMap<String, String>() {
        {
            put("3", "Mirano BOCCIODROMO Aggiuntivi (Via G. Matteotti 46, Mirano (VE))");
//            put("YY","Mirano BOCCIODROMO ");
            put("5", "Dolo PALAZZETTO DELLO SPORT (Viale dello Sport 1, Dolo (VE))");
            put("281", "Dolo Ospedale - Poliambulatori (Ospedale di Dolo - Presso Poliambulatori edificio n.1)");
            put("73", "Venezia PALA EXPO (Via Galileo Ferraris 5, Marghera (VE))");
        }
    };

    public static HashMap<String, String> ulssLabels = new HashMap<String, String>() {
        {
            put(ULSS_3_SERENISSIMA, "ULSS_3_SERENISSIMA");
            put(ULSS_6_EUGANEA, "ULSS_6_EUGANEA");
        }
    };

    public static HashMap<String, String> ulssLinks = new HashMap<String, String>() {
        {
            put(ULSS_3_SERENISSIMA, "https://vaccinicovid.regione.veneto.it/ulss3");
            put(ULSS_6_EUGANEA, "https://vaccinicovid.regione.veneto.it/ulss6");
        }
    };


    public static HashMap<String, HashMap<String, String>> ulss_e_sedi = new HashMap<String, HashMap<String, String>>() {
        {
            put(ULSS_3_SERENISSIMA, sedi_ulss_3);
            put(ULSS_6_EUGANEA, sedi_ulss_6);
        }
    };

    public void scan(TelegramLongPollingBot estiaBot) throws InterruptedException, JsonProcessingException {
        int rounds = 0;
        while ( circuitBreak ) {
            botWorkingNotification(estiaBot, rounds);

            List<DayAvailabilities> roundAvailabilities = new ArrayList<>();
            System.out.println("round " + rounds);
            for (String sede : sedi_ulss_3.keySet()) {
                roundAvailabilities.addAll(parseAvailabilities(dayRequest(ULSS_3_SERENISSIMA, sede), ULSS_3_SERENISSIMA, sede));
            }
            for (String sede : sedi_ulss_6.keySet()) {
                roundAvailabilities.addAll(parseAvailabilities(dayRequest(ULSS_6_EUGANEA, sede), ULSS_6_EUGANEA, sede));
            }

            roundAvailabilities.forEach(this::filterAvailabilities);

            for (DayAvailabilities avail : roundAvailabilities) {
                notify(avail);
            }

            notifyBot(estiaBot, roundAvailabilities.stream().filter(DayAvailabilities::isNotify).collect(Collectors.toList()));

            System.out.println("sleep round " + rounds);
            Thread.sleep(9000);
            rounds++;
        }
    }

    private List<DayAvailabilities> parseAvailabilities(String availArray, String ulssKey, String sedeKey) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());

        List<DayAvailabilities> dayAvailabilities = mapper.readValue(availArray, new TypeReference<List<DayAvailabilities>>() {
        });
        dayAvailabilities.forEach(avail -> {
            avail.setSede(sedeKey);
            avail.setUlss(ulssKey);
        });
        return dayAvailabilities;
    }

    private DayAvailabilities filterAvailabilities(DayAvailabilities avail) {
        if (ignoredDays.contains(avail.getStart())) {
            avail.setIgnored(true);
            avail.setNotify(false);
        }
        return avail;
    }

    private void notify(DayAvailabilities avail) {
        if (avail.isNotify()) {
            System.out.println(" GIORNO= " + avail.getStart() + " SEDE=" + ulss_e_sedi.get(avail.getUlss()).get(avail.getSede()) + "ULSS=" + ulssLabels.get(avail.getUlss()));
            // + " (magic) NUMBER= " + parseTitle(avail.getTitle())
        } else {
            System.out.println("ignored item" + avail);
        }
    }

    private void notifyBot(TelegramLongPollingBot estiaBot, List<DayAvailabilities> roundAvailabilities) {
        if ( roundAvailabilities.isEmpty() ) return;
        try {
            estiaBot.execute(notifyBot(roundAvailabilities));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendMessage notifyBot(List<DayAvailabilities> avails) {
        String availabilityList = avails.stream()
                .filter(DayAvailabilities::isNotify)
                .map(DayAvailabilities::toString)
                .collect(Collectors.joining("\n"));
        return botMessage(availabilityList);
    }

    private SendMessage botMessage(String botMessage) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder();
        builder.text(botMessage);
        builder.allowSendingWithoutReply(true);
        builder.chatId(System.getenv("TELEGRAM_ADMIN_CHAT_ID"));
        return builder.build();
    }

    private void botWorkingNotification(TelegramLongPollingBot estiaBot, int round) {
        if ( round % 50 != 0 ) return;
        try {
            estiaBot.execute(botMessage("Bot is working. Round="+round));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String dayRequest(String servizio, String sede) {
        Client client = ClientBuilder.newClient();
        try {
            UriBuilder uriBuilder = UriBuilder.fromUri("https://vaccinicovid.regione.veneto.it/ulss3");
            WebTarget webTarget = client.target(uriBuilder);
            Invocation.Builder request = webTarget.request();

            // set up the query params
            MultivaluedHashMap entity = new MultivaluedHashMap();
            entity.add("azione", "jscalendario");
            entity.add("servizio", servizio);
            entity.add("sede", sede);
            entity.add("start", "2021-05-31T00:00:00+02:00");
            entity.add("end", "2021-08-12T00:00:00+02:00");

            Response response = request.post(Entity.form(entity));
            String availabilities = "";
            if (response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                availabilities = response.readEntity(String.class);
                if (availabilities == null || !availabilities.trim().equalsIgnoreCase("[]")) {
//                System.out.println(availabilities);
                } else {
                    System.out.println("*** NO AVAIL ***  SEDE=" + sede + " ULSS=" + servizio);
                }
            } else {
                System.err.println(LocalDateTime.now() + " errore");
            }
            return availabilities;
        } finally {
            client.close();
        }
    }

}
