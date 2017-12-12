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
 * @author Oleksii Skachkov
 */
@ToString(exclude = {"slackIdPattern", "logger"})
@EqualsAndHashCode
public class SlackParsedCommand {
    public static final String SLACK_ID_PATTERN = "\\<@(.*?)(\\||\\>)";
    public static final String SLACK_ID_WRAPPER_PATTERN = "<@%s>";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private String slackIdPattern;
    private UserDTO fromUser;
    private String text;
    private List<UserDTO> usersInText;

    public SlackParsedCommand(UserDTO fromUser, String text, List<UserDTO> usersInText) {
        this.fromUser = fromUser;
        this.text = text;
        this.usersInText = usersInText;
        slackIdPattern = SLACK_ID_PATTERN;
        logger.debug("SlackParsedCommand created with parameters: " +
                        "fromSlackId: {} text: {} userCountInText {} users: {}",
                fromUser, text, usersInText.size(), usersInText.toString());
    }

    public List<UserDTO> getAllUsersFromText() {
        return usersInText;
    }

    public UserDTO getFirstUserFromText() {
        if (usersInText.size() == 0) {
            logger.warn("The text: '{}' doesn't contain any slack ids", text);
            throw new WrongCommandFormatException(String.format("The text '%s' doesn't contain any slack ids", text));
        } else {
            return usersInText.get(0);
        }
    }

    public String getTextWithoutSlackIds() {
        String result = text.replaceAll(slackIdPattern, "");
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

    public static String wrapSlackId(String slackId){
        return String.format(SLACK_ID_WRAPPER_PATTERN, slackId);
    }
}