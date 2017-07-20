package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.com.juja.microservices.keepers.slackbot.dao.UserRepository;
import ua.com.juja.microservices.keepers.slackbot.model.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Nikolay Horushko
 */
@Service
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public DefaultUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        logger.debug("Received SlackName: [{}] for conversion", slackNames.toString());
        List<UserDTO> users = userRepository.findUsersBySlackNames(slackNames);
        logger.info("Found users: [{}] by SlackName: [{}]", users.toString(), slackNames.toString());
        return users;
    }
}
