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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.service.KeeperService;
import ua.com.juja.microservices.utils.SlackUrlUtils;

import javax.inject.Inject;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Konstantin Sergey
 * @author Ivan Shapovalov
 * @author Oleksii Skachkov
 */
@RunWith(SpringRunner.class)
@WebMvcTest(KeepersSlackCommandController.class)
public class KeepersSlackCommandControllerTest {

    private static final String SORRY_MESSAGE = "Sorry! You're not lucky enough to use our slack command.";
    private static final String IN_PROGRESS = "In progress...";
    private static final String EXAMPLE_URL = "http://example.com";
    private static final String ERROR_MESSAGE = "Something went wrong!";
    private static final String TOKEN_WRONG = "wrongSlackToken";
    @Value("${keepers.slackBot.slack.slashCommandToken}")
    private String tokenCorrect;
    @Value("${keepers.slackBot.rest.api.version}")
    private String version;

    @Inject
    private MockMvc mvc;

    @MockBean
    private KeeperService keeperService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void onReceiveSlashCommandKeeperAddIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        // given
        final String keeperAddCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/command", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(SORRY_MESSAGE));

        //then
        verifyZeroInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddSendOkRichMessage() throws Exception {
        // given
        final String keeperAddCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        final String keeperResponse = String.format("Thanks, we added a new Keeper: %s in direction: teams",
                SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);


        when(keeperService.sendKeeperAddRequest("slack-from", keeperAddCommandText)).thenReturn(keeperResponse);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).sendKeeperAddRequest("slack-from", keeperAddCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);

        assertTrue(richMessageCaptor.getValue().getText().contains(keeperResponse));
    }

    @Test
    public void onReceiveSlashKeeperAddShouldSendExceptionMessage() throws Exception {
        // given
        final String keeperAddCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).sendKeeperAddRequest("slack-from", keeperAddCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperAddWhenUsersServiceUnavailableShouldSendExceptionMessage() throws Exception {
        // given
        final String keeperAddCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        ResourceAccessException exception = new ResourceAccessException("Some service unavailable");
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperAddRequest(any(String.class), any(String.class))).thenThrow(exception);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/add"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-add", keeperAddCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).sendKeeperAddRequest("slack-from", keeperAddCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        verifyNoMoreInteractions(keeperService, restTemplate);

        assertTrue(richMessageCaptor.getValue().getText().contains("Some service unavailable"));
    }

    @Test
    public void onReceiveSlashCommandKeeperDeactivateIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        // given
        final String keeperDeactivateCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/command", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(SORRY_MESSAGE));

        //then
        verifyZeroInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperDeactivateSendOkRichMessage() throws Exception {
        // given
        final String keeperDeactivateCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        final String keeperResponse = String.format("Keeper: %s in direction: teams dismissed",
                SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperDeactivateRequest("slack-from", keeperDeactivateCommandText))
                .thenReturn(keeperResponse);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        //then
        verify(keeperService).sendKeeperDeactivateRequest("slack-from", keeperDeactivateCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(keeperResponse));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashKeeperDeactivateShouldSendExceptionMessage() throws Exception {
        // given
        final String keeperDeactivateCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.sendKeeperDeactivateRequest(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper/deactivate"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper-deactivate", keeperDeactivateCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).sendKeeperDeactivateRequest("slack-from", keeperDeactivateCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashCommandGetKeeperDirectionsIncorrectTokenShouldSendSorryRichMessage() throws Exception {
        // given
        final String getDirectionsCommandText = SlackParsedCommand.wrapSlackUserInFullPattern("slack1");

        //when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper"),
                SlackUrlUtils.getUriVars(TOKEN_WRONG, "/command", getDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(SORRY_MESSAGE));

        // then
        verifyZeroInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetKeeperDirectionsSendOkRichMessage() throws Exception {
        // given
        final String getDirectionsCommandText = SlackParsedCommand.wrapSlackUserInFullPattern("slack1");
        final String keeperResponse = String.format("The keeper %s has active directions: [direction1, direction2]",
                SlackParsedCommand.wrapSlackUserInFullPattern("slack1"));
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getKeeperDirections("slack-from", getDirectionsCommandText))
                .thenReturn(keeperResponse);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", getDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).getKeeperDirections("slack-from", getDirectionsCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(keeperResponse));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetKeeperDirectionsShouldSendExceptionMessage() throws Exception {
        // given
        final String getDirectionsCommandText = SlackParsedCommand.wrapSlackUserInFullPattern("slack1");
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getKeeperDirections(any(String.class), any(String.class)))
                .thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils.getUrlTemplate(version + "/commands/keeper"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", getDirectionsCommandText))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).getKeeperDirections("slack-from", getDirectionsCommandText);
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetMyDirectionsSendOkRichMessage() throws Exception {
        // given
        final String keeperResponse = String.format("The keeper %s has active directions: [direction1, direction2]",
                SlackParsedCommand.wrapSlackUserInFullPattern("slack-from"));
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getMyDirections("slack-from")).thenReturn(keeperResponse);
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils
                        .getUrlTemplate(version + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).getMyDirections("slack-from");
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(keeperResponse));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }

    @Test
    public void onReceiveSlashGetMyDirectionsShouldSendExceptionMessage() throws Exception {
        // given
        ArgumentCaptor<RichMessage> richMessageCaptor = ArgumentCaptor.forClass(RichMessage.class);

        when(keeperService.getMyDirections("slack-from")).thenThrow(new RuntimeException(ERROR_MESSAGE));
        when(restTemplate.postForObject(anyString(), any(RichMessage.class), anyObject())).thenReturn("[OK]");

        // when
        mvc.perform(MockMvcRequestBuilders.post(SlackUrlUtils
                        .getUrlTemplate(version + "/commands/keeper/myDirections"),
                SlackUrlUtils.getUriVars(tokenCorrect, "/keeper/AAA111", ""))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(IN_PROGRESS));

        // then
        verify(keeperService).getMyDirections("slack-from");
        verify(restTemplate).postForObject(eq(EXAMPLE_URL), richMessageCaptor.capture(), eq(String.class));
        assertTrue(richMessageCaptor.getValue().getText().contains(ERROR_MESSAGE));
        verifyNoMoreInteractions(keeperService, restTemplate);
    }
}