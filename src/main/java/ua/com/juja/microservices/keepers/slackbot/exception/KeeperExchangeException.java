package ua.com.juja.microservices.keepers.slackbot.exception;

/**
 * @author Nikolay Horushko
 */
public class KeeperExchangeException extends BaseBotException {
    public KeeperExchangeException(ApiError error, Exception ex) {
        super(error, ex);
    }
}
