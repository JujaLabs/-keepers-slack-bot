package ua.com.juja.microservices.keepers.slackbot.dao.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ua.com.juja.microservices.keepers.slackbot.dao.UserRepository;
import ua.com.juja.microservices.keepers.slackbot.dao.feign.UsersClient;
import ua.com.juja.microservices.keepers.slackbot.exception.ApiError;
import ua.com.juja.microservices.keepers.slackbot.exception.UserExchangeException;
import ua.com.juja.microservices.keepers.slackbot.model.dto.SlackNameRequest;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.utils.Utils;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Ivan Shapovalov
 */
@Repository
@Slf4j
public class RestUserRepository implements UserRepository {
    @Inject
    private UsersClient usersClient;

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        log.debug("Received SlackNames : [{}]", slackNames);
        SlackNameRequest slackNameRequest = new SlackNameRequest(slackNames);
        List<UserDTO> users;
        try {
            users = usersClient.findUsersBySlackNames(slackNameRequest);
            log.debug("Finished request to Users service. Users [{}]", users.toString());
        } catch (FeignException ex) {
            ApiError error = Utils.convertToApiError(ex.getMessage());
            log.warn("Users service returned an error: [{}]", error);
            throw new UserExchangeException(error, ex);
        }
        log.info("Got UserDTO:{} by users: {}", users, slackNames);
        return users;
    }
}