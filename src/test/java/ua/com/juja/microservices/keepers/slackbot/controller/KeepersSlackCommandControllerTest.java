package ua.com.juja.microservices.keepers.slackbot.controller;

import me.ramswaroop.jbot.core.slack.models.RichMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.exception.ApiError;
import ua.com.juja.microservices.keepers.slackbot.exception.UserExchangeException;
import ua.com.juja.microservices.keepers.slackbot.service.KeeperService;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Konstantin Sergey
 * @author Ivan Shapovalov
 */
@RunWith(SpringRunner.class)
@WebMvcTest(KeepersSlackCommandController.class)
public class KeepersSlackCommandControllerTest {

    private static final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command.";
    private static final String IN_PROGRESS = "In progress...";
    private static final String EXAMPLE_URL = "http://example.com";
    private static final String ERROR_MESSAGE = "Something went wrong!";
    private static final String TOKEN_WRONG = "wrongSlackToken";
    @Value("${slack.slashCommandToken}")
    private String tokenCorrect;

    @Inject
    private MockMvc mvc;

    @MockBean
    private KeeperService keeperService;

    @MockBean
    private RestTemplate restTemplate;

    private String keepersSlackbotAddKeeperUrl = "/v1/commands/keeper/add";
    private String keepersSlackbotDeactivateKeeperUrl = "/v1/commands/keeper/deactivate";
    private String keepersSlackbotGetKeeperDirectionsUrl = "/v1/commands/keeper";
    private String keepersSlackbotGetMyDirectionsUrl = "/v1/commands/keeper/myDirections";

    @Test
    public void onReceiveAllSlashCommandsWhenIncorrectTokenShouldReturnSorryMessage() throws Exception {
        List<String> urls = Arrays.asList(
                keepersSlackbotAddKeeperUrl,
                keepersSlackbotDeactivateKeeperUrl,
                keepersSlackbotGetKeeperDirectionsUrl,
                keepersSlackbotGetMyDirectionsUrl);
        urls.forEach(url -> {
            try {
                mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(url),
                        SlackUrlUtils.getUriVars(TOKEN_WRONG, "/command", ""))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                        .andExpect(status().isOk())
                        .andExpect(content().string(SORRY_MESSAGE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddSendOkRichMessage() throws Exception {
        // given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        final String KEEPER_RESPONSE = "Thanks, we added a new Keeper: @slack1 in direction: teams";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest("@from-user", KEEPER_ADD_COMMAND_TEXT))
                .thenReturn(KEEPER_RESPONSE);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(KEEPER_RESPONSE));
        verify(keeperService).sendKeeperAddRequest("@from-user", KEEPER_ADD_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddWhenRuntimeExceptionThrownShouldSendExceptionMessage() throws Exception {
        // given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verify(keeperService).sendKeeperAddRequest("@from-user", KEEPER_ADD_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddWhenNestedExceptionAfterRuntimeExceptionThrownDoNothing()
            throws Exception {
        // given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        RuntimeException runtimeException = new RuntimeException();
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class)))
                .thenThrow(runtimeException);

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verify(keeperService).sendKeeperAddRequest("@from-user", KEEPER_ADD_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddWhenSomeServiceUnavailableShouldSendExceptionMessage() throws Exception {
        // given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        ApiError apiError = new ApiError(
                400, "KMF-F5-D2",
                "Sorry, User server return an error",
                "Exception - UserExchangeException",
                "Something went wrong",
                Collections.emptyList()
        );
        UserExchangeException userExchangeException = new UserExchangeException(apiError, new RuntimeException());
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest(any(String.class), any(String.class)))
                .thenThrow(userExchangeException);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains("Something went wrong"));
        verify(keeperService).sendKeeperAddRequest("@from-user", KEEPER_ADD_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddWhenNestedExceptionWhenSomeServiceUnavailableShouldDoNothing()
            throws Exception {
        // given
        final String KEEPER_ADD_COMMAND_TEXT = "@slack1 teams";
        ApiError apiError = new ApiError(
                400, "KMF-F5-D2",
                "Sorry, User server return an error",
                "Exception - UserExchangeException",
                "Something went wrong",
                Collections.emptyList()
        );
        UserExchangeException userExchangeException = new UserExchangeException(apiError, new RuntimeException());
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest(any(String.class), any(String.class)))
                .thenThrow(userExchangeException);
        RuntimeException runtimeException = new RuntimeException();
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class)))
                .thenThrow(runtimeException);

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotAddKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", KEEPER_ADD_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains("Something went wrong"));
        verify(keeperService).sendKeeperAddRequest("@from-user", KEEPER_ADD_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperDeactivateSendOkRichMessage() throws Exception {
        // given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 teams";
        final String KEEPER_RESPONSE = "Keeper: @slack1 in direction: teams dismissed";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperDeactivateRequest("@from-user", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .thenReturn(KEEPER_RESPONSE);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        //than
        assertTrue(richMessageCaptor.getValue().getText().contains(KEEPER_RESPONSE));
        verify(keeperService).sendKeeperDeactivateRequest("@from-user", KEEPER_DEACTIVATE_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperDeactivateWhenRuntimeExceptionShouldSendExceptionMessage() throws Exception {
        // given
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack1 teams";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperDeactivateRequest(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verify(keeperService).sendKeeperDeactivateRequest("@from-user", KEEPER_DEACTIVATE_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperDeactivateWhenSomeServiceUnavailableShouldSendExceptionMessage() throws Exception {
        // given
        final String commandText = "@slack1 teams";
        ApiError apiError = new ApiError(
                400, "KMF-F5-D2",
                "Sorry, User server return an error",
                "Exception - UserExchangeException",
                "Something went wrong",
                Collections.emptyList()
        );
        UserExchangeException userExchangeException = new UserExchangeException(apiError, new RuntimeException());
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperDeactivateRequest(any(String.class), any(String.class)))
                .thenThrow(userExchangeException);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotDeactivateKeeperUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", commandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains("Something went wrong"));
        verify(keeperService).sendKeeperDeactivateRequest("@from-user", commandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetKeeperDirectionsSendOkRichMessage() throws Exception {
        // given
        final String GET_DIRECTIONS_COMMAND_TEXT = "@slack1";
        final String KEEPER_RESPONSE = "The keeper @slack1 has active directions: [direction1, direction2]";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getKeeperDirections("@from-user", GET_DIRECTIONS_COMMAND_TEXT))
                .thenReturn(KEEPER_RESPONSE);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", GET_DIRECTIONS_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(KEEPER_RESPONSE));
        verify(keeperService).getKeeperDirections("@from-user", GET_DIRECTIONS_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetKeeperDirectionsWhenRuntimeExceptionThrownShouldSendExceptionMessage() throws
            Exception {
        // given
        final String GET_DIRECTIONS_COMMAND_TEXT = "@slack1";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getKeeperDirections(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", GET_DIRECTIONS_COMMAND_TEXT))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verify(keeperService).getKeeperDirections("@from-user", GET_DIRECTIONS_COMMAND_TEXT);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetKeeperDirectionsWhenSomeServiceUnavailableShouldSendExceptionMessage() throws
            Exception {
        // given
        final String commandText = "@slack1 teams";
        ApiError apiError = new ApiError(
                400, "KMF-F5-D2",
                "Sorry, User server return an error",
                "Exception - UserExchangeException",
                "Something went wrong",
                Collections.emptyList()
        );
        UserExchangeException userExchangeException = new UserExchangeException(apiError, new RuntimeException());
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getKeeperDirections(any(String.class), any(String.class)))
                .thenThrow(userExchangeException);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetKeeperDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", commandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains("Something went wrong"));
        verify(keeperService).getKeeperDirections("@from-user", commandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetMyDirectionsSendOkRichMessage() throws Exception {
        // given
        final String KEEPER_RESPONSE = "The keeper @from-user has active directions: [direction1, direction2]";
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        // when
        when(keeperService.getMyDirections("@from-user")).thenReturn(KEEPER_RESPONSE);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // then
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        assertTrue(richMessageCaptor.getValue().getText().contains(KEEPER_RESPONSE));
        verify(keeperService).getMyDirections("@from-user");
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetMyDirectionsWhenRuntimeExceptionThrownShouldSendExceptionMessage() throws Exception {
        // given
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getMyDirections("@from-user")).thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verify(keeperService).getMyDirections("@from-user");
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetMyDirectionsWhenSomeServiceUnavailableShouldSendExceptionMessage() throws Exception {
        // given
        final String commandText = "";
        ApiError apiError = new ApiError(
                400, "KMF-F5-D2",
                "Sorry, User server return an error",
                "Exception - UserExchangeException",
                "Something went wrong",
                Collections.emptyList()
        );
        UserExchangeException userExchangeException = new UserExchangeException(apiError, new RuntimeException());
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getMyDirections("@from-user")).thenThrow(userExchangeException);
        when(restTemplate.postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class))).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(keepersSlackbotGetMyDirectionsUrl),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper", commandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        assertTrue(richMessageCaptor.getValue().getText().contains("Something went wrong"));
        verify(keeperService).getMyDirections("@from-user");
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }
}