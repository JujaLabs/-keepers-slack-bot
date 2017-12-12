package ua.com.juja.microservices.keepers.slackbot.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.keepers.slackbot.dao.UserRepository;
import ua.com.juja.microservices.keepers.slackbot.model.dto.UserDTO;
import ua.com.juja.microservices.keepers.slackbot.service.UserService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

/**
 * @author Nikolay Horushko
 * @author Oleksii Skachkov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultUserServiceTest {

    @Inject
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    public void returnUsersListBySlacks() throws Exception {
        //given
        List<String> slackIdsRequest = Arrays.asList("slack-id1", "slack-id2");
        List<UserDTO> usersResponse = Arrays.asList(new UserDTO("uuid1", "slack-id1"),
                new UserDTO("uuid2", "slack-id2"));
        given(userRepository.findUsersBySlackIds(slackIdsRequest)).willReturn(usersResponse);
        //when
        List<UserDTO> result = userService.findUsersBySlackIds(slackIdsRequest);
        //then
        assertEquals("[UserDTO(uuid=uuid1, slackId=slack-id1), UserDTO(uuid=uuid2, slackId=slack-id2)]", result.toString());
    }
}