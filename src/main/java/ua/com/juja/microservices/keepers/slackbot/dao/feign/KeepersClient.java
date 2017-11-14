package ua.com.juja.microservices.keepers.slackbot.dao.feign;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;

/**
 * @author Ivan Shapovalov
 */
@FeignClient(name = "gateway")
public interface KeepersClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/keepers",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String[] addKeeper(KeeperRequest keeperRequest);

    @RequestMapping(method = RequestMethod.PUT, value = "/v1/keepers",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String[] deactivateKeeper(KeeperRequest keeperRequest);

    @RequestMapping(method = RequestMethod.PUT, value = "/v1/keepers/{uuid}",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String[] getKeeperDirections(KeeperRequest keeperRequest, @RequestParam(value = "uuid") String uuid);
}
