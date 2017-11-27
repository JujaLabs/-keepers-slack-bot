package ua.com.juja.microservices.keepers.slackbot.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ua.com.juja.microservices.keepers.slackbot.exception.WrongCommandFormatException;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * @author Konstantin Sergey
 */
public class SlackParsedCommandTest {
    private List<UserDTO> usersInText;
    private UserDTO fromUser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        fromUser = new UserDTO("uuid0", "@from");
        usersInText = new ArrayList<>();
    }

    @Test
    public void getFirstUserInText() {
        //given
        usersInText.add(new UserDTO("uuid1", "@slack1"));
        String text = "text text @slack1 text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        UserDTO result = slackParsedCommand.getFirstUserFromText();
        //then
        assertEquals("UserDTO(uuid=uuid1, slack=@slack1)", result.toString());
    }

    @Test
    public void getFirstUserInTextThrowExceptionIfNotUser() {
        //given
        String text = "text text text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //then
        thrown.expect(WrongCommandFormatException.class);
        thrown.expectMessage(containsString("The text 'text text text' doesn't contain any slack names"));
        //when
        slackParsedCommand.getFirstUserFromText();
    }

    @Test
    public void getAllUsers() {
        //given
        usersInText.add(new UserDTO("uuid1", "@slack1"));
        usersInText.add(new UserDTO("uuid2", "@slack2"));
        usersInText.add(new UserDTO("uuid3", "@slack3"));
        String text = "text @slack3 text@slack2 text @slack1";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        List<UserDTO> result = slackParsedCommand.getAllUsersFromText();
        //then
        assertEquals("[UserDTO(uuid=uuid1, slack=@slack1), UserDTO(uuid=uuid2, slack=@slack2)," +
                        " UserDTO(uuid=uuid3, slack=@slack3)]",
                result.toString());
    }

    @Test
    public void getText() {
        //given
        String text = "text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        String result = slackParsedCommand.getText();
        //then
        assertEquals("text", result);
    }

    @Test
    public void getFromUser() {
        //given
        String text = "text";
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //when
        UserDTO result = slackParsedCommand.getFromUser();
        //then
        assertEquals("UserDTO(uuid=uuid0, slack=@from)", result.toString());
    }

    @Test
    public void getUserCount() {
        //given
        usersInText.add(new UserDTO("uuid1", "@slack1"));
        String text = "text @slack1 text";
        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //then
        assertEquals(1, slackParsedCommand.getUserCountInText());
    }

    @Test
    public void getTextWithoutSlackNames() {
        //given
        String text = "   @slack text  @slack text @slack ";
        //when
        SlackParsedCommand slackParsedCommand = new SlackParsedCommand(fromUser, text, usersInText);
        //then
        assertEquals("text text", slackParsedCommand.getTextWithoutSlackNames());
    }
}