package ua.com.juja.microservices.keepers.slackbot.utils;

import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Konstantin Sergey
 */
public class Utils {
    public static String getProperty(String propertyFile, String propertyName) {
        Properties properties = new Properties();
        ClassLoader loader = SlackParsedCommand.class.getClassLoader();
        try {
            properties.load(loader.getResourceAsStream(propertyFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty(propertyName);
    }
}
