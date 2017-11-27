package ua.com.juja.microservices.keepers.slackbot.exception;

/**
 * @author Nikolay Horushko
 */
public class BaseBotException extends RuntimeException {
    private final ApiError error;

    public BaseBotException(ApiError error, Exception ex) {
        super(error.getClientMessage(), ex);
        this.error = error;
    }

    public String detailMessage() {
        return error.toString();
    }

    @Override
    public String getMessage() {
        return error.getClientMessage();
    }

    public String getExceptionMessage() {
        return error.getExceptionMessage();
    }
}
