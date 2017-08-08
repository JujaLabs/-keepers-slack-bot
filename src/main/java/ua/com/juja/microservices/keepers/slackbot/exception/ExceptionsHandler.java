package ua.com.juja.microservices.keepers.slackbot.exception;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Danil Kuznetsov
 * @author Dmitriy Lyashenko
 */
@RestControllerAdvice
public class ExceptionsHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(Exception.class)
    public RichMessage handleAllOtherExceptions(Exception ex) {
        logger.warn("Other Exception': {}", ex.getMessage());
        return new RichMessage(ex.getMessage());
    }

    @ExceptionHandler(WrongCommandFormatException.class)
    public RichMessage handleWrongCommandFormatException(Exception ex) {
        logger.warn("WrongCommandFormatException: {}", ex.getMessage());
        return new RichMessage(ex.getMessage());
    }

    @ExceptionHandler(UserExchangeException.class)
    public RichMessage handleUserExchangeException(UserExchangeException ex) {
        logger.warn("UserExchangeException: {}", ex.detailMessage());
        return new RichMessage(ex.getMessage());
    }

    @ExceptionHandler(KeeperExchangeException.class)
    public RichMessage handleKeeperExchangeException(KeeperExchangeException ex) {
        logger.warn("KeeperExchangeException : {}", ex.detailMessage());
        return new RichMessage(ex.getMessage());
    }
}