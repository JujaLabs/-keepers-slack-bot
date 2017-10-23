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
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultKeeperServiceTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    UserDTO fromUser;
    @MockBean
    private KeeperRepository keeperRepository;
    @MockBean
    private SlackNameHandlerService slackNameHandlerService;
    @Inject
    private KeeperService keeperService;
    private List<UserDTO> usersInText;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        fromUser = new UserDTO("uuid0", "@from");
        usersInText = new ArrayList<>();
        usersInText.add(new UserDTO("uuid1", "@slack_name"));
    }

    @Test
    public void shouldSaveKeeperAndReturnValidText() {
        //given
        String[] expectedKeeperId = {"100"};
        final String KEEPER_ADD_COMMAND_TEXT = "@slack_name teams";
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid1", "teams");
        when(keeperRepository.addKeeper(keeperRequest)).thenReturn(expectedKeeperId);
        when(slackNameHandlerService.createSlackParsedCommand("@from", KEEPER_ADD_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, KEEPER_ADD_COMMAND_TEXT, usersInText));

        //when
        String result = keeperService.sendKeeperAddRequest("@from", KEEPER_ADD_COMMAND_TEXT);

        //then
        assertEquals("Thanks, we added a new Keeper: @slack_name in direction: teams", result);
        verify(keeperRepository).addKeeper(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", KEEPER_ADD_COMMAND_TEXT);
    }

    @Test
    public void keeperAddShouldReturnERRORText() {
        //given
        String[] expectedEmptyArray = {};
        final String KEEPER_ADD_COMMAND_TEXT = "@slack_name teams";
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid1", "teams");
        when(keeperRepository.addKeeper(keeperRequest)).thenReturn(expectedEmptyArray);
        when(slackNameHandlerService.createSlackParsedCommand("@from", KEEPER_ADD_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, KEEPER_ADD_COMMAND_TEXT, usersInText));

        //when
        String result = keeperService.sendKeeperAddRequest("@from", KEEPER_ADD_COMMAND_TEXT);

        //then
        assertEquals("ERROR. Something went wrong. Keeper was not added :(", result);
        verify(keeperRepository).addKeeper(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", KEEPER_ADD_COMMAND_TEXT);
    }

    @Test
    public void keeperAddWhenZeroUsersInTextShouldReturnERRORText() {
        //given
        usersInText.clear();
        final String KEEPER_ADD_COMMAND_TEXT = "teams";
        when(slackNameHandlerService.createSlackParsedCommand("@from", KEEPER_ADD_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, KEEPER_ADD_COMMAND_TEXT, usersInText));

        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(containsString("We didn't find any slack name in your command. " +
                "'teams' You must write the user's slack name to perform the action with keepers."));

        //when
        keeperService.sendKeeperAddRequest("@from", KEEPER_ADD_COMMAND_TEXT);

        //then
        verify(slackNameHandlerService).createSlackParsedCommand("@from", KEEPER_ADD_COMMAND_TEXT);
    }

    @Test
    public void shouldDeactivateKeeperAndReturnValidText() {
        //given
        String[] expectedKeeperId = {"100"};
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack_name teams";
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid1", "teams");
        when(keeperRepository.deactivateKeeper(keeperRequest)).thenReturn(expectedKeeperId);
        when(slackNameHandlerService.createSlackParsedCommand("@from", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, KEEPER_DEACTIVATE_COMMAND_TEXT, usersInText));

        //when
        String result = keeperService.sendKeeperDeactivateRequest("@from", KEEPER_DEACTIVATE_COMMAND_TEXT);

        //then
        assertEquals("Keeper: @slack_name in direction: teams deactivated", result);
        verify(keeperRepository).deactivateKeeper(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", KEEPER_DEACTIVATE_COMMAND_TEXT);
    }

    @Test
    public void keeperDeactivateShouldReturnERRORText() {
        //given
        String[] expectedEmptyArray = {};
        final String KEEPER_DEACTIVATE_COMMAND_TEXT = "@slack_name teams";
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid1", "teams");
        when(keeperRepository.deactivateKeeper(keeperRequest)).thenReturn(expectedEmptyArray);
        when(slackNameHandlerService.createSlackParsedCommand("@from", KEEPER_DEACTIVATE_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, KEEPER_DEACTIVATE_COMMAND_TEXT, usersInText));

        //when
        String result = keeperService.sendKeeperDeactivateRequest("@from", KEEPER_DEACTIVATE_COMMAND_TEXT);

        //then
        assertEquals("ERROR. Something went wrong. Keeper was not deactivated :(", result);
        verify(keeperRepository).deactivateKeeper(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", KEEPER_DEACTIVATE_COMMAND_TEXT);
    }

    @Test
    public void getKeeperDirections() {
        //Given
        String[] expected = {"direction1"};
        final String GET_KEEPER_DIRECTIONS_COMMAND_TEXT = "@slack_name";
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid1", "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(expected);
        when(slackNameHandlerService.createSlackParsedCommand("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, GET_KEEPER_DIRECTIONS_COMMAND_TEXT, usersInText));

        //When
        String result = keeperService.getKeeperDirections("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT);

        //Then
        assertEquals("The keeper @slack_name has active directions: [direction1]", result);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT);
    }

    @Test
    public void getKeeperDirectionsWhenFromUserInTextShouldThrowException() {
        //Given
        usersInText.clear();
        usersInText.add(new UserDTO("uuid0", "@from"));
        final String GET_KEEPER_DIRECTIONS_COMMAND_TEXT = "@from";
        when(slackNameHandlerService.createSlackParsedCommand("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, GET_KEEPER_DIRECTIONS_COMMAND_TEXT, usersInText));

        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(
                containsString("Your own slackname in command. To get your own directions use another command"));

        //When
        keeperService.getKeeperDirections("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT);

        //Then
        verify(slackNameHandlerService).createSlackParsedCommand("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT);
        verifyNoMoreInteractions(slackNameHandlerService, keeperRepository);
    }

    @Test
    public void getKeeperDirectionsWithEmptyResult() {
        //Given
        String[] emptyArray = {};
        final String GET_KEEPER_DIRECTIONS_COMMAND_TEXT = "@slack_name";
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid1", "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(emptyArray);
        when(slackNameHandlerService.createSlackParsedCommand("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT))
                .thenReturn(new SlackParsedCommand(fromUser, GET_KEEPER_DIRECTIONS_COMMAND_TEXT, usersInText));

        //When
        String result = keeperService.getKeeperDirections("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT);

        //Then
        assertEquals("The keeper @slack_name has no active directions.", result);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", GET_KEEPER_DIRECTIONS_COMMAND_TEXT);
    }

    @Test
    public void getMyDirectionsShouldReturnValidText() {
        //Given
        usersInText.clear();
        usersInText.add(new UserDTO("uuid", "@from"));
        String[] expected = {"direction1"};
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid0", "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(expected);
        when(slackNameHandlerService.createSlackParsedCommand("@from", ""))
                .thenReturn(new SlackParsedCommand(fromUser, "", usersInText));

        //When
        String result = keeperService.getMyDirections("@from");

        //Then
        assertEquals("The keeper @from has active directions: [direction1]", result);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", "");
        verifyNoMoreInteractions(slackNameHandlerService, keeperRepository);
    }

    @Test
    public void getMyDirectionsShouldReturnEmptyResult() {
        //Given
        usersInText.clear();
        usersInText.add(new UserDTO("uuid0", "@from"));
        String[] emptyArray = {};
        KeeperRequest keeperRequest = new KeeperRequest("uuid0", "uuid0", "");
        when(keeperRepository.getKeeperDirections(keeperRequest)).thenReturn(emptyArray);
        when(slackNameHandlerService.createSlackParsedCommand("@from", ""))
                .thenReturn(new SlackParsedCommand(fromUser, "", usersInText));

        //When
        String result = keeperService.getMyDirections("@from");

        //Then
        assertEquals("The keeper @from has no active directions.", result);
        verify(keeperRepository).getKeeperDirections(keeperRequest);
        verify(slackNameHandlerService).createSlackParsedCommand("@from", "");
    }
}