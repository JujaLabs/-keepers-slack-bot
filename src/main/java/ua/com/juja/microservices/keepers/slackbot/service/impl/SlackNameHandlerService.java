package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Nikolay Horushko
 * @author Konstantin Sergey
 */
@Service
public class SlackNameHandlerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private UserService userService;

    /**
     * Slack name cannot be longer than 21 characters and
     * can only contain letters, numbers, periods, hyphens, and underscores.
     * ([a-z0-9\.\_\-]){1,21}
     * quick test regExp http://regexr.com/
     */
    @Value("${keepers.slackNamePattern}")
    private String slackNamePattern;

    @Inject
    public SlackNameHandlerService(UserService userService) {
        this.userService = userService;
    }

    public SlackParsedCommand createSlackParsedCommand(String fromUser, String text) {
        if (!fromUser.startsWith("@")) {
            fromUser = "@" + fromUser;
            logger.debug("add '@' to slack name [{}]", fromUser);
        }
        Map<String, UserDTO> usersMap = receiveUsersMap(fromUser, text);
        UserDTO fromUserDTO = usersMap.get(fromUser);
        usersMap.remove(fromUser);

        return new SlackParsedCommand(fromUserDTO, text, new ArrayList<>(usersMap.values()));
    }

    private Map<String, UserDTO> receiveUsersMap(String fromSlackName, String text) {
        List<String> slackNames = receiveAllSlackNames(text);
        slackNames.add(fromSlackName);
        logger.debug("added \"fromSlackName\" slack name to request: [{}]", fromSlackName);
        logger.debug("send slack names: {} to user service", slackNames);
        List<UserDTO> users = userService.findUsersBySlackNames(slackNames);
        return users.stream()
                .collect(Collectors.toMap(user -> user.getSlack(), user -> user, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<String> receiveAllSlackNames(String text) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(slackNamePattern);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        logger.debug("Recieved slack names: {} from text:", result.toString(), text);
        return result;
    }
}
