package ua.com.juja.microservices.keepers.slackbot.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.com.juja.microservices.keepers.slackbot.exception.WrongCommandFormatException;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.utils.Utils;

import java.util.List;

/**
 * @author Konstantin Sergey
 */
@ToString(exclude = {"slackNamePattern", "logger"})
@EqualsAndHashCode
public class SlackParsedCommand {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private String slackNamePattern;
    private UserDTO fromUser;
    private String text;
    private List<UserDTO> usersInText;

    public SlackParsedCommand(UserDTO fromUser, String text, List<UserDTO> usersInText) {
        this.fromUser = fromUser;
        this.text = text;
        this.usersInText = usersInText;
        slackNamePattern = Utils.getProperty(
                "application.properties",
                "keepers.slackNamePattern"
        );
        logger.debug("SlackParsedCommand created with parameters: " +
                        "fromSlackName: {} text: {} userCountInText {} users: {}",
                fromUser, text, usersInText.size(), usersInText.toString());
    }

    public List<UserDTO> getAllUsersFromText() {
        return usersInText;
    }

    public UserDTO getFirstUserFromText() {
        if (usersInText.size() == 0) {
            logger.warn("The text: '{}' doesn't contain any slack names", text);
            throw new WrongCommandFormatException(String.format("The text '%s' doesn't contain any slack names", text));
        } else {
            return usersInText.get(0);
        }
    }

    public String getTextWithoutSlackNames() {
        String result = text.replaceAll(slackNamePattern, "");
        result = result.replaceAll("\\s+", " ").trim();
        return result;
    }

    public UserDTO getFromUser() {
        return fromUser;
    }

    public String getText() {
        return text;
    }

    public int getUserCountInText() {
        return usersInText.size();
    }
}