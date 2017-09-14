package ua.com.juja.microservices.keepers.slackbot.controller;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.exception.BaseBotException;
import ua.com.juja.microservices.keepers.slackbot.exception.UserExchangeException;
import ua.com.juja.microservices.keepers.slackbot.service.KeeperService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Konstantin Sergey
 */
@RestController
@RequestMapping(value = "${keepers.slackBot.rest.api.version}" + "${keepers.slackBot.baseCommandsUrl}")
public class KeepersSlackCommandController {
    private static final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command.";
    private static final String IN_PROGRESS = "In progress...";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${keepers.slackBot.slack.slashCommandToken}")
    private String slackToken;

    private KeeperService keeperService;
    private RestTemplate restTemplate;

    @Inject
    public KeepersSlackCommandController(KeeperService keeperService,
                                         RestTemplate restTemplate) {
        this.keeperService = keeperService;
        this.restTemplate = restTemplate;
    }

    @PostMapping(value = "${keepers.slackBot.endpoint.keeperAdd}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void addKeeper(@RequestParam("token") String token,
                          @RequestParam("user_name") String fromUser,
                          @RequestParam("text") String text,
                          @RequestParam("response_url") String responseUrl,
                          HttpServletResponse httpServletResponse) throws IOException {
        try {
            logger.debug("Received slash command KeeperAdd: from user: [{}] command: [{}] token: [{}] responseUrl: [{}]",
                    fromUser, text, token, responseUrl);

            if (!token.equals(slackToken)) {
                logger.warn("Received invalid slack token: [{}] in command KeeperAdd for user: [{}]", token, fromUser);
                sendQuickResponse(httpServletResponse, SORRY_MESSAGE);
            } else {
                sendQuickResponse(httpServletResponse, IN_PROGRESS);
                String response = keeperService.sendKeeperAddRequest(fromUser, text);
                logger.info("KeeperAdd command processed : user: [{}] text: [{}] and sent response into slack: [{}]",
                        fromUser, text, response);
                sendDelayedResponse(responseUrl, response);
            }
        } catch (BaseBotException bex) {
            sendBaseBotExceptionMessage(responseUrl, bex);
        } catch (Exception ex) {
            sendExceptionMessage(responseUrl, ex);
        }
    }

    @PostMapping(value = "${keepers.slackBot.endpoint.keeperDeactivate}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void deactivateKeeper(@RequestParam("token") String token,
                                 @RequestParam("user_name") String fromUser,
                                 @RequestParam("text") String text,
                                 @RequestParam("response_url") String responseUrl,
                                 HttpServletResponse httpServletResponse) {
        try {
            logger.debug("Received slash command KeeperDeactivate: from user: [{}] command: [{}] token: [{}]",
                    fromUser, text, token);

            if (!token.equals(slackToken)) {
                logger.warn("Received invalid slack token: [{}] in command KeeperDeactivate for user: [{}]", token, fromUser);
                sendQuickResponse(httpServletResponse, SORRY_MESSAGE);
            } else {
                sendQuickResponse(httpServletResponse, IN_PROGRESS);
                String response = keeperService.sendKeeperDeactivateRequest(fromUser, text);
                logger.info("KeeperDeactivate command processed : user: [{}] text: [{}] and sent response into slack: [{}]",
                        fromUser, text, response);
                sendDelayedResponse(responseUrl, response);
            }
        } catch (BaseBotException bex) {
            sendBaseBotExceptionMessage(responseUrl, bex);
        } catch (Exception ex) {
            sendExceptionMessage(responseUrl, ex);
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void getKeeperDirections(@RequestParam("token") String token,
                                    @RequestParam("user_name") String fromUser,
                                    @RequestParam("text") String text,
                                    @RequestParam("response_url") String responseUrl,
                                    HttpServletResponse httpServletResponse) {
        try {
            logger.debug("Received slash command GetKeeperDirections: from user: [{}] command: [{}] token: [{}]",
                    fromUser, text, token);

            if (!token.equals(slackToken)) {
                logger.warn("Received invalid slack token: [{}] in command Keeper for user: [{}]", token, fromUser);
                sendQuickResponse(httpServletResponse, SORRY_MESSAGE);
            } else {
                sendQuickResponse(httpServletResponse, IN_PROGRESS);
                String response = keeperService.getKeeperDirections(fromUser, text);
                logger.info("GetKeeperDirections command processed : user: [{}] text: [{}] and sent response to slack: [{}]",
                        fromUser, text, response);
                sendDelayedResponse(responseUrl, response);
            }
        } catch (BaseBotException bex) {
            sendBaseBotExceptionMessage(responseUrl, bex);
        } catch (Exception ex) {
            sendExceptionMessage(responseUrl, ex);
        }
    }

    private void sendQuickResponse(HttpServletResponse httpServletResponse, String message) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter printWriter = httpServletResponse.getWriter()) {
            printWriter.print(message);
            printWriter.flush();
        }
        logger.info("Sent a quick response with message '{}'", message);
    }

    private void sendDelayedResponse(String responseUrl, String response) {
        String slackAnswer = restTemplate.postForObject(responseUrl, new RichMessage(response), String.class);
        logger.info("Slack answered: [{}]", slackAnswer == null ? "null" : slackAnswer);
    }

    private void sendBaseBotExceptionMessage(String responseUrl, BaseBotException bex) {
        logger.warn("There was an exceptional situation: [{}]", bex.detailMessage());
        try {
            String message = bex.getMessage();
            if (bex instanceof UserExchangeException) {
                message = bex.getExceptionMessage();
            }
            String slackAnswer = restTemplate.postForObject(responseUrl, new RichMessage(message), String.class);
            logger.warn("Slack answered: [{}]", slackAnswer == null ? "null" : slackAnswer);
        } catch (Exception e) {
            logger.warn("Nested exception: [{}]", e.getMessage());
        }
    }

    private void sendExceptionMessage(String responseUrl, Exception ex) {
        logger.warn("There was an exceptional situation: [{}]", ex.getMessage());
        try {
            String message = ex.getMessage();
            if (ex instanceof ResourceAccessException) {
                message = "Some service unavailable";
            }
            String slackAnswer = restTemplate.postForObject(responseUrl, new RichMessage(message), String.class);
            logger.warn("Slack answered: [{}]", slackAnswer == null ? "null" : slackAnswer);
        } catch (Exception e) {
            logger.warn("Nested exception: [{}]", e.getMessage());
        }
    }
}