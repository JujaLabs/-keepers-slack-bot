package ua.com.juja.microservices.keepers.slackbot.exception;

/**
 * @author Nikolay Horushko
 */
public class WrongCommandFormatException extends RuntimeException {
    public WrongCommandFormatException(String message) {
        super(message);
    }
}