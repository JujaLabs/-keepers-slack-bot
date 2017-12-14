package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author Ivan Shapovalov
 * @author Oleksii Skachkov
 */
@Service
public class SlackUserHandlerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private UserService userService;

    @Inject
    public SlackUserHandlerService(UserService userService) {
        this.userService = userService;
    }

    public SlackParsedCommand createSlackParsedCommand(String fromSlackUser, String text) {
        Map<String, UserDTO> usersMap = receiveUsersMap(fromSlackUser, text);
        UserDTO fromUserDTO = usersMap.get(fromSlackUser);
        if (usersMap.size() > 1) {
            usersMap.remove(fromSlackUser);
        }
        return new SlackParsedCommand(fromUserDTO, text, new ArrayList<>(usersMap.values()));
    }

    private Map<String, UserDTO> receiveUsersMap(String fromSlackUser, String text) {
        List<String> slackUsers = receiveAllSlackUsers(text);
        logger.debug("Added 'fromSlackUser' slack user to request: [{}]", fromSlackUser);
        slackUsers.add(fromSlackUser);
        logger.debug("Send slack users: {} to user service", slackUsers);
        List<UserDTO> users = userService.findUsersBySlackUsers(slackUsers);
        logger.debug("Receive users: {} from user service", users);
        Map<String, UserDTO> usersMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getSlackUser, user -> user, (e1, e2) -> e1, LinkedHashMap::new));
        logger.debug("Convert users to map '{}'", usersMap);
        return usersMap;
    }

    private List<String> receiveAllSlackUsers(String text) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(SlackParsedCommand.SLACK_USER_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1).trim());
        }
        logger.debug("Recieved slack users: {} from text:", result.toString(), text);
        return result;
    }
}
