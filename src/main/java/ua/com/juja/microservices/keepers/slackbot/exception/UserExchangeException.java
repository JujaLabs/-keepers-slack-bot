package ua.com.juja.microservices.keepers.slackbot.exception;

/**
 * @author Nikolay Horushko
 */
public class UserExchangeException extends BaseBotException{
    public UserExchangeException(ApiError error, Exception ex) {
        super(error, ex);
    }
}
