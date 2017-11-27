package ua.com.juja.microservices.keepers.slackbot.dao.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.dao.UserRepository;
import ua.com.juja.microservices.keepers.slackbot.exception.ApiError;
import ua.com.juja.microservices.keepers.slackbot.exception.UserExchangeException;
import ua.com.juja.microservices.keepers.slackbot.model.dto.SlackNameRequest;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 */
@Repository
public class RestUserRepository extends AbstractRestRepository implements UserRepository {

    private final RestTemplate restTemplate;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${users.baseURL}")
    private String urlBase;
    @Value("${users.rest.api.version}")
    private String version;
    @Value("${users.endpoint.usersBySlackNames}")
    private String urlGetUsers;

    @Inject
    public RestUserRepository(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<UserDTO> findUsersBySlackNames(List<String> slackNames) {
        logger.debug("Received SlackNames : [{}]", slackNames);

        for (int i = 0; i < slackNames.size(); i++) {
            if (!slackNames.get(i).startsWith("@")) {
                logger.debug("add '@' to SlackName : [{}]", slackNames.get(i));
                String slackName = slackNames.get(i);
                slackNames.set(i, "@" + slackName);
            }
        }

        SlackNameRequest slackNameRequest = new SlackNameRequest(slackNames);
        HttpEntity<SlackNameRequest> request = new HttpEntity<>(slackNameRequest, setupBaseHttpHeaders());

        List<UserDTO> result;
        try {
            logger.debug("Started request to Users service. Request is : [{}]", request.toString());
            ResponseEntity<UserDTO[]> response = restTemplate.exchange(urlBase + version + urlGetUsers,
                    HttpMethod.POST, request, UserDTO[].class);
            logger.debug("Finished request to Users service. Response is: [{}]", response.toString());
            result = Arrays.asList(response.getBody());
        } catch (HttpClientErrorException ex) {
            ApiError error = convertToApiError(ex);
            logger.warn("Users service returned an error: [{}]", error);
            throw new UserExchangeException(error, ex);
        }

        logger.info("Got UserDTO:{} by users: {}", result, slackNames);
        return result;
    }
}