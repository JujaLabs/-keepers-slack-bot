package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.keepers.slackbot.dao.KeeperRepository;
import ua.com.juja.microservices.keepers.slackbot.exception.WrongCommandFormatException;
import ua.com.juja.microservices.keepers.slackbot.model.SlackParsedCommand;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;
import ua.com.juja.microservices.keepers.slackbot.service.KeeperService;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Horushko
 * @author Dmitriy Lyashenko
 * @author Ivan Shapovalov
 * @author Oleksii Skachkov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultKeeperServiceTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private UserDTO userFrom;
    private UserDTO user1;
    @MockBean
    private KeeperRepository keeperRepository;
    @MockBean
    private SlackUserHandlerService slackUserHandlerService;
    @Inject
    private KeeperService keeperService;
    private List<UserDTO> usersInText;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        userFrom = new UserDTO("uuid-from", "slack-from");
        user1 = new UserDTO("uuid1", "slack1");
        usersInText = new ArrayList<>();
    }

    @Test
    public void shouldSaveKeeperAndReturnValidText() {
        //given
        usersInText.add(user1);
        String[] expectedKeeperId = {"100"};
        final String keeperAddCommandText = String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        when(keeperRepository.addKeeper(keeperRequest)).thenReturn(expectedKeeperId);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), keeperAddCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, keeperAddCommandText, usersInText));
        String expected = String.format("Thanks, we added a new Keeper: %s in direction: teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        //when
        String actual = keeperService.sendKeeperAddRequest(userFrom.getSlackUser(), keeperAddCommandText);

        //then
        assertEquals(expected, actual);
        verify(keeperRepository).addKeeper(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), keeperAddCommandText);
    }

    @Test
    public void keeperAddShouldReturnERRORText() {
        //given
        usersInText.add(user1);
        String[] expectedEmptyArray = {};
        final String KEEPER_ADD_COMkeeperAddCommandTextAND_TEXT =
                String.format("%s teams", SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        when(keeperRepository.addKeeper(keeperRequest)).thenReturn(expectedEmptyArray);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), KEEPER_ADD_COMkeeperAddCommandTextAND_TEXT))
                .thenReturn(new SlackParsedCommand(userFrom, KEEPER_ADD_COMkeeperAddCommandTextAND_TEXT, usersInText));

        //when
        String actual = keeperService.sendKeeperAddRequest(userFrom.getSlackUser(), KEEPER_ADD_COMkeeperAddCommandTextAND_TEXT);

        //then
        assertEquals("ERROR. Something went wrong. Keeper was not added :(", actual);
        verify(keeperRepository).addKeeper(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(),
                KEEPER_ADD_COMkeeperAddCommandTextAND_TEXT);
    }

    @Test
    public void keeperAddWhenZeroUsersInTextShouldReturnErrorText() {
        //given
        final String keeperAddCommandText = "teams";
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), keeperAddCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, keeperAddCommandText, usersInText));

        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(containsString("We didn't find any slack user in your command 'teams'. " +
                "You must write the user's slack to perform the action with keepers."));

        //when
        keeperService.sendKeeperAddRequest(userFrom.getSlackUser(), keeperAddCommandText);

        //then
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), keeperAddCommandText);
    }

    @Test
    public void shouldDeactivateKeeperAndReturnValidText() {
        //given
        usersInText.add(user1);
        String[] expectedKeeperId = {"100"};
        final String keeperDeactivateCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        when(keeperRepository.deactivateKeeper(keeperRequest)).thenReturn(expectedKeeperId);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), keeperDeactivateCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, keeperDeactivateCommandText, usersInText));
        String expected = String.format("Keeper: %s in direction: teams deactivated",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));

        //when
        String actual = keeperService.sendKeeperDeactivateRequest(userFrom.getSlackUser(), keeperDeactivateCommandText);

        //then
        assertEquals(expected, actual);
        verify(keeperRepository).deactivateKeeper(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), keeperDeactivateCommandText);
    }

    @Test
    public void keeperDeactivateShouldReturnERRORText() {
        //given
        usersInText.add(user1);
        String[] expectedEmptyArray = {};
        final String keeperDeactivateCommandText = String.format("%s teams",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "teams");
        when(keeperRepository.deactivateKeeper(keeperRequest)).thenReturn(expectedEmptyArray);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), keeperDeactivateCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, keeperDeactivateCommandText, usersInText));

        //when
        String actual = keeperService.sendKeeperDeactivateRequest(userFrom.getSlackUser(), keeperDeactivateCommandText);

        //then
        assertEquals("ERROR. Something went wrong. Keeper was not deactivated :(", actual);
        verify(keeperRepository).deactivateKeeper(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), keeperDeactivateCommandText);
    }

    @Test
    public void getKeeperDirections() {
        //Given
        usersInText.add(user1);
        String[] directions = {"direction1"};
        final String getKeeperDirectionsCommandText = SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser());
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(directions);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), getKeeperDirectionsCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, getKeeperDirectionsCommandText, usersInText));
        String expected = String.format("The keeper %s has active directions: [direction1]",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        //When
        String actual = keeperService.getKeeperDirections(userFrom.getSlackUser(), getKeeperDirectionsCommandText);

        //Then
        assertEquals(expected, actual);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), getKeeperDirectionsCommandText);
    }

    @Test
    public void getKeeperDirectionsWhenFromUserInTextShouldThrowException() {
        //Given
        usersInText.add(userFrom);
        final String getKeeperDirectionsCommandText = String.format("%s",
                SlackParsedCommand.wrapSlackUserInFullPattern(userFrom.getSlackUser()));
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), getKeeperDirectionsCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, getKeeperDirectionsCommandText, usersInText));

        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(
                containsString("Your own slack in command. To get your own directions use another command"));

        //When
        keeperService.getKeeperDirections(userFrom.getSlackUser(), getKeeperDirectionsCommandText);

        //Then
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), getKeeperDirectionsCommandText);
        verifyNoMoreInteractions(slackUserHandlerService, keeperRepository);
    }

    @Test
    public void getKeeperDirectionsWithEmptyResult() {
        //Given
        usersInText.add(user1);
        String[] emptyArray = {};
        final String getKeeperDirectionsCommandText = String.format("%s",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), user1.getUuid(), "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(emptyArray);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), getKeeperDirectionsCommandText))
                .thenReturn(new SlackParsedCommand(userFrom, getKeeperDirectionsCommandText, usersInText));
        String expected = String.format("The keeper %s has no active directions.",
                SlackParsedCommand.wrapSlackUserInFullPattern(user1.getSlackUser()));
        //When
        String actual = keeperService.getKeeperDirections(userFrom.getSlackUser(), getKeeperDirectionsCommandText);

        //Then
        assertEquals(expected, actual);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), getKeeperDirectionsCommandText);
    }

    @Test
    public void getMyDirectionsShouldReturnValidText() {
        //Given
        usersInText.add(userFrom);
        String[] directions = {"direction1"};
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), userFrom.getUuid(), "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(directions);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), ""))
                .thenReturn(new SlackParsedCommand(userFrom, "", usersInText));
        String expected = String.format("The keeper %s has active directions: [direction1]",
                SlackParsedCommand.wrapSlackUserInFullPattern(userFrom.getSlackUser()));

        //When
        String actual = keeperService.getMyDirections(userFrom.getSlackUser());

        //Then
        assertEquals(expected, actual);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), "");
        verifyNoMoreInteractions(slackUserHandlerService, keeperRepository);
    }

    @Test
    public void getMyDirectionsShouldReturnEmptyResult() {
        //Given
        usersInText.add(userFrom);
        String[] emptyArray = {};
        KeeperRequest keeperRequest = new KeeperRequest(userFrom.getUuid(), userFrom.getUuid(), "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(emptyArray);
        when(slackUserHandlerService.createSlackParsedCommand(userFrom.getSlackUser(), ""))
                .thenReturn(new SlackParsedCommand(userFrom, "", usersInText));
        String expected = String.format("The keeper %s has no active directions.",
                SlackParsedCommand.wrapSlackUserInFullPattern(userFrom.getSlackUser()));


        //When
        String actual = keeperService.getMyDirections(userFrom.getSlackUser());

        //Then
        assertEquals(expected, actual);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackUserHandlerService).createSlackParsedCommand(userFrom.getSlackUser(), "");
    }
}