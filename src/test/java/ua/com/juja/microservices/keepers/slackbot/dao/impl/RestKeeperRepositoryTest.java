package ua.com.juja.microservices.keepers.slackbot.dao.impl;

import feign.FeignException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import ua.com.juja.microservices.keepers.slackbot.dao.KeeperRepository;
import ua.com.juja.microservices.keepers.slackbot.dao.feign.KeepersClient;
import ua.com.juja.microservices.keepers.slackbot.exception.KeeperExchangeException;
import ua.com.juja.microservices.keepers.slackbot.model.request.KeeperRequest;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
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
public class RestKeeperRepositoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Inject
    private KeeperRepository keeperRepository;
    @MockBean
    private KeepersClient keepersClient;

    @Test
    public void addKeeperWhenKeepersServiceReturnCorrectData() {
        //given
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", "uuid1", "direction");
        String[] expected = {"1000"};
        when(keepersClient.addKeeper(keeperRequest)).thenReturn(expected);

        //when
        String[] actual = keeperRepository.addKeeper(keeperRequest);

        //then
        assertArrayEquals(expected, actual);
        verify(keepersClient).addKeeper(keeperRequest);
        verifyNoMoreInteractions(keepersClient);
    }

    @Test
    public void addKeeperWhenKeepersServiceThrowExceptionWithCorrectContent() {
        //given
        String expectedJsonResponseBody =
                "status 400 reading KeepersClient#addKeeper(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", "uuid1", "direction");
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        when(keepersClient.addKeeper(keeperRequest)).thenThrow(feignException);

        thrown.expect(KeeperExchangeException.class);
        thrown.expectMessage(containsString("Oops something went wrong :("));

        try {
            //when
            keeperRepository.addKeeper(keeperRequest);
        } finally {
            //then
            verify(keepersClient).addKeeper(keeperRequest);
            verifyNoMoreInteractions(keepersClient);
        }
    }

    @Test
    public void addKeeperWhenKeepersServiceThrowExceptionWithIncorrectContent() {
        //given
        String expectedJsonResponseBody =
                "status 400 reading UsersClient#findUsersBySlackNames(); content: \n";
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", "uuid1", "direction");
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        when(keepersClient.addKeeper(keeperRequest)).thenThrow(feignException);

        thrown.expect(KeeperExchangeException.class);
        thrown.expectMessage(containsString("I'm, sorry. I cannot parse api error message from remote service :("));

        try {
            //when
            keeperRepository.addKeeper(keeperRequest);
        } finally {
            //then
            verify(keepersClient).addKeeper(keeperRequest);
            verifyNoMoreInteractions(keepersClient);
        }
    }

    @Test
    public void deactivateKeeperWhenKeepersServiceReturnCorrectId() {
        //given
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", "uuid1", "direction");
        String[] expected = {"1000"};
        when(keepersClient.deactivateKeeper(keeperRequest)).thenReturn(expected);

        //when
        String[] actual = keeperRepository.deactivateKeeper(keeperRequest);

        //then
        assertArrayEquals(expected, actual);
        verify(keepersClient).deactivateKeeper(keeperRequest);
        verifyNoMoreInteractions(keepersClient);
    }

    @Test
    public void deactivateKeeperWhenKeepersServiceThrowExceptionWithCorrectContent() {
        //given
        String expectedJsonResponseBody =
                "status 400 reading KeepersClient#deactivateKeeper(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", "uuid1", "direction");
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        when(keepersClient.deactivateKeeper(keeperRequest)).thenThrow(feignException);

        thrown.expect(KeeperExchangeException.class);
        thrown.expectMessage(containsString("Oops something went wrong :("));

        try {
            //when
            keeperRepository.deactivateKeeper(keeperRequest);
        } finally {
            //then
            verify(keepersClient).deactivateKeeper(keeperRequest);
            verifyNoMoreInteractions(keepersClient);
        }
    }

    @Test
    public void getKeeperDirectionsWhenKeepersServiceReturnCorrectIds() {
        //given
        String uuid = "uuid-keeper";
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", uuid, "direction");
        String[] expected = {"direction1", "direction2", "direction3"};
        when(keepersClient.getKeeperDirections(keeperRequest, uuid)).thenReturn(expected);

        //when
        String[] actual = keeperRepository.getKeeperDirections(keeperRequest);

        //then
        assertArrayEquals(expected, actual);
        verify(keepersClient).getKeeperDirections(keeperRequest, uuid);
        verifyNoMoreInteractions(keepersClient);
    }

    @Test
    public void getKeeperDirectionsWhenKeepersServiceThrowExceptionWithCorrectContent() {
        //given
        String expectedJsonResponseBody =
                "status 400 reading KeepersClient#getKeeperDirections(KeeperRequest); content:" +
                        "{\"httpStatus\":400,\n" +
                        "\"internalErrorCode\":1,\n" +
                        "\"clientMessage\":\"Oops something went wrong :(\",\n" +
                        "\"developerMessage\":\"General exception for this service\",\n" +
                        "\"exceptionMessage\":\"very big and scare error\",\n" +
                        "\"detailErrors\":[]\n" +
                        "}";
        String uuid = "uuid-keeper";
        KeeperRequest keeperRequest = new KeeperRequest("uuid-from", uuid, "direction");
        FeignException feignException = mock(FeignException.class);
        when(feignException.getMessage()).thenReturn(expectedJsonResponseBody);
        when(keepersClient.getKeeperDirections(keeperRequest, uuid)).thenThrow(feignException);

        thrown.expect(KeeperExchangeException.class);
        thrown.expectMessage(containsString("Oops something went wrong :("));

        try {
            //when
            keeperRepository.getKeeperDirections(keeperRequest);
        } finally {
            //then
            verify(keepersClient).getKeeperDirections(keeperRequest, uuid);
            verifyNoMoreInteractions(keepersClient);
        }
    }
}