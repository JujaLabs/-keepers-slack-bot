package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.*;
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
public class SlackIdHandlerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private UserService userService;

    @Inject
    public SlackIdHandlerService(UserService userService) {
        this.userService = userService;
    }

    public SlackParsedCommand createSlackParsedCommand(String fromUserId, String text) {
        Map<String, UserDTO> usersMap = receiveUsersMap(fromUserId, text);
        UserDTO fromUserDTO = usersMap.get(fromUserId);
        if (usersMap.size() > 1) {
            usersMap.remove(fromUserId);
        }
        return new SlackParsedCommand(fromUserDTO, text, new ArrayList<>(usersMap.values()));
    }

        private Map<String, UserDTO> receiveUsersMap(String fromSlackId, String text) {
        List<String> slackIds = receiveAllSlackIds(text);
        logger.debug("added \"fromSlackId\" slack id to request: [{}]", fromSlackId);
        slackIds.add(fromSlackId);
        logger.debug("send slack ids: {} to user service", slackIds);
        List<UserDTO> users = userService.findUsersBySlackIds(slackIds);
        logger.debug("Receive users: {} from user service", users);
         Map<String, UserDTO> usersMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getSlackId, user -> user, (e1, e2) -> e1, LinkedHashMap::new));
        logger.debug("Convert users to map '{}'", usersMap);
        return usersMap;
    }

    private List<String> receiveAllSlackIds(String text) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(SlackParsedCommand.SLACK_ID_PATTERN);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1).trim());
        }
        logger.debug("Recieved slack ids: {} from text:", result.toString(), text);
        return result;
    }
}
