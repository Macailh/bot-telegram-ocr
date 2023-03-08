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
import org.telegram.telegrambots.meta.api.objects.*;
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
    private final Tesseract tesseract;

    @Value("${botToken}")
    private String botToken;

    @Value("${botName}")
    private String botName;

    @Autowired
    public TelegramBoot(Tesseract tesseract) {
        this.tesseract = tesseract;
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
        if (update.hasMessage()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        if (message.hasPhoto()) {
            String textImg = getTextFromPhoto(message.getPhoto());
            sendMessage(textImg, message.getChatId());
        }
    }

    private void sendMessage(String textImg, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(textImg);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getTextFromPhoto(List<PhotoSize> photos) {
        String txt = "";
        PhotoSize image = photos.stream()
                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst()
                .orElse(null);

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


            } catch (TelegramApiException | IOException e) {
                throw new RuntimeException(e);
            } catch (TesseractException e) {
                e.printStackTrace();
            }
        }
        return txt;
    }
}