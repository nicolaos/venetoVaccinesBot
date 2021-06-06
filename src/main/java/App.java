import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {
    public static void main(String[] args) {
        ScanAvailabilities scanAvailabilities = new ScanAvailabilities();
        AvailabilitiesBot availabilitiesBot = new AvailabilitiesBot(scanAvailabilities);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(availabilitiesBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
