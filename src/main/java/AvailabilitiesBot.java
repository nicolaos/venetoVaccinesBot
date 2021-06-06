import com.fasterxml.jackson.core.JsonProcessingException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class AvailabilitiesBot extends TelegramLongPollingBot {

    public AvailabilitiesBot() {
    }

    public AvailabilitiesBot(ScanAvailabilities scanAvailabilities) {
        this.scanAvailabilities = scanAvailabilities;
    }

    private ScanAvailabilities scanAvailabilities;

    @Override
    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            if ( update.getMessage().getText().equalsIgnoreCase("scan") ) {
                System.out.println("***** SCAN REQUEST RECEIVED *****");
                try {
                    scanAvailabilities.scan(this);
                } catch (InterruptedException | JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else if ( update.getMessage().getText().equalsIgnoreCase("stopScan") ) {
                System.out.println("***** STOP SCAN *****");
                scanAvailabilities.setCircuitBreak(false);
            } else {
                SendMessage message = new SendMessage(); // Create a SendMessage object with mandatory fields
                message.setChatId(update.getMessage().getChatId().toString());
                message.setText(update.getMessage().getText());
                System.out.println("update.getMessage().getChatId()=" + update.getMessage().getChatId());

                try {
                    execute(message); // Call method to send the message
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return System.getenv("AVAILABILITIES_BOT_NAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("AVAILABILITIES_BOT_TOKEN");
    }
}