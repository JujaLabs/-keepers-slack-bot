package ua.com.juja.microservices.keepers.slackbot.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.com.juja.microservices.keepers.slackbot.exception.WrongCommandFormatException;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import java.util.List;

/**
 * @author Konstantin Sergey
 * @author Oleksii Skachkov
 */
@ToString(exclude = {"logger"})
@EqualsAndHashCode
public class SlackParsedCommand {
    public static final String SLACK_USER_PATTERN = "\\<@(.*?)(\\||\\>)";
    public static final String SLACK_USER_FULL_PATTERN = "\\<@(.*?)(\\>)";
    public static final String SLACK_USER_WRAPPER_FULL_PATTERN = "<@%s>";
    public static final String SLACK_USER_WRAPPER_PARTIAL_PATTERN = "<@%1$s|%1$s>";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private UserDTO fromUser;
    private String text;
    private List<UserDTO> usersInText;

    public SlackParsedCommand(UserDTO fromUser, String text, List<UserDTO> usersInText) {
        this.fromUser = fromUser;
        this.text = text;
        this.usersInText = usersInText;
        logger.debug("SlackParsedCommand created with parameters: fromSlackUser : {} text: {} userCountInText {} users: {}",
                fromUser, text, usersInText.size(), usersInText.toString());
    }

    public static String wrapSlackUserInFullPattern(String slackUser) {
        return String.format(SLACK_USER_WRAPPER_FULL_PATTERN, slackUser);
    }

    public static String wrapSlackUserInPartialPattern(String slackUser) {
        return String.format(SLACK_USER_WRAPPER_PARTIAL_PATTERN, slackUser);
    }

    public List<UserDTO> getAllUsersFromText() {
        return usersInText;
    }

    public UserDTO getFirstUserFromText() {
        if (usersInText.size() == 0) {
            logger.warn("The text: '{}' doesn't contain any slack users", text);
            throw new WrongCommandFormatException(String.format("The text '%s' doesn't contain any slack users", text));
        } else {
            return usersInText.get(0);
        }
    }

    public String getTextWithoutSlackUsers() {
        String result = text.replaceAll(SLACK_USER_FULL_PATTERN, "");
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