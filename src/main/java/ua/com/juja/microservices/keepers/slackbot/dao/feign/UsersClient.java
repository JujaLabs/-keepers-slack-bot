package ua.com.juja.microservices.keepers.slackbot.dao.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ua.com.juja.microservices.keepers.slackbot.model.dto.SlackNameRequest;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import java.util.List;

/**
 * @author Ivan Shapovalov
 */
@FeignClient(name = "gateway")
public interface UsersClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/users/usersBySlackNames",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    List<UserDTO> findUsersBySlackNames(SlackNameRequest request);
}
