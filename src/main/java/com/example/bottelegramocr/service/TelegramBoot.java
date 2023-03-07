package com.example.bottelegramocr.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

@Service
public class TelegramBoot extends TelegramLongPollingBot {
    private final static Logger LOGGER = LoggerFactory.getLogger(TelegramBoot.class);

    @Autowired
    private Tesseract tesseract;


    @Value("${botToken}")
    private String botToken;

    @Value("${botName}")
    private String botName;

    public TelegramBoot() {
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Get the message from the user
        final String messageTextReceived = update.getMessage().getText();
        String txt = "";
        LOGGER.info("Message received " + messageTextReceived);

        // Get user id
        final long chatId = update.getMessage().getChatId();

        if (update.getMessage().hasPhoto()) {
            // Get image from message
            List<PhotoSize> photos = update.getMessage().getPhoto();

            PhotoSize image = photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null);
            LOGGER.info(image.getFileId());

            if (image != null) {
                GetFile getFile = new GetFile(image.getFileId());

                try {
                    File file = execute(getFile);
                    InputFile inputFile = new InputFile(file.getFileUrl(getBotToken()));

                    InputStream inputStream = new URL(file.getFileUrl(getBotToken())).openStream();

                    BufferedImage imagef = ImageIO.read(inputStream);

                    String text = tesseract.doOCR(imagef);
                    txt = text;
                    LOGGER.info(text);

                    SendMessage messageimg = new SendMessage();
                    messageimg.setChatId(chatId);
                    messageimg.setText(text);



                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (TesseractException e) {
                    e.printStackTrace();
                }
            }
        }


        // Create message object
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(txt);

        try {
            // Send message
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}