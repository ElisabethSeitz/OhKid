package de.neuefische.finalproject.ohboy.controller;

import de.neuefische.finalproject.ohboy.dao.MonsterMongoDao;
import de.neuefische.finalproject.ohboy.dao.RewardMongoDao;
import de.neuefische.finalproject.ohboy.dao.TaskMongoDao;
import de.neuefische.finalproject.ohboy.dao.UserDao;
import de.neuefische.finalproject.ohboy.dto.*;
import de.neuefische.finalproject.ohboy.model.Monster;
import de.neuefische.finalproject.ohboy.model.OhBoyUser;
import de.neuefische.finalproject.ohboy.model.Reward;
import de.neuefische.finalproject.ohboy.model.Task;
import de.neuefische.finalproject.ohboy.service.FacebookApiService;
import de.neuefische.finalproject.ohboy.utils.IdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.neuefische.finalproject.ohboy.model.Status.DONE;
import static de.neuefische.finalproject.ohboy.model.Status.OPEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "jwt.secretkey=somesecretkey",
        "oauth.facebook.client.id=facebookClient",
        "oauth.facebook.client.secret=secret",
        "oauth.facebook.redirect.uri=redirectUri",
})
class MonsterControllerTest {

    @LocalServerPort
    private int port;

    @MockBean
    private IdUtils mockedIdUtils;

    @MockBean
    private FacebookApiService mockedFacebookApiService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MonsterMongoDao monsterDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TaskMongoDao taskDao;

    @Autowired
    private RewardMongoDao rewardDao;

    @BeforeEach
    public void setupDao() {
        monsterDao.deleteAll();
        monsterDao.saveAll(List.of(
                new Monster("someId", "someUserId", "someName", "someImage", 0, 0),
                new Monster("someId2", "facebook@1234", "someName2", "someImage2", 50, 4),
                new Monster("someId3", "someUserId3", "someName3", "someImage3", 0, 0)
        ));

        userDao.deleteAll();
        userDao.save(new OhBoyUser("FacebookUser", "facebook@1234", true));

        taskDao.deleteAll();
        taskDao.saveAll(List.of(
                Task.builder().id("someTaskId").userId("facebook@1234").monsterId("someId2").description("someDescription").score(5).status(DONE).timestampOfDone(Instant.parse("1970-01-01T00:00:00Z")).build(),
                Task.builder().id("someTaskId2").userId("facebook@1234").monsterId("someId2").description("someDescription2").score(10).status(OPEN).build(),
                Task.builder().id("someTaskId3").userId("someUserId3").monsterId("someId3").description("someDescription3").score(15).status(OPEN).build()
        ));

        rewardDao.deleteAll();
        rewardDao.saveAll(List.of(
                Reward.builder().id("someRewardId").userId("facebook@1234").monsterId("someId2").description("someDescription").score(5).status(DONE).timestampOfDone(Instant.parse("1970-01-01T00:00:00Z")).build(),
                Reward.builder().id("someRewardId2").userId("facebook@1234").monsterId("someId2").description("someDescription2").score(10).status(OPEN).build(),
                Reward.builder().id("someRewardId3").userId("someUserId3").monsterId("someId3").description("someDescription3").score(15).status(OPEN).build()
        ));
    }

    private String getMonstersUrl() {
        return "http://localhost:" + port + "/api/monster";
    }

    private String login(){
        when(mockedFacebookApiService.getAccessTokenFromFacebook("code")).thenReturn("access_token");
        when(mockedFacebookApiService.getFacebookUserData("access_token")).thenReturn(new FacebookUserDto("FacebookUser", "1234"));

        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/auth/login/facebook", new FacebookCodeDto(
                "code"
        ), String.class);

        return response.getBody();
    }

    private <T> HttpEntity<T> getValidAuthorizationEntity(T data) {
        String token = login();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<T>(data,headers);
    }


    @Test
    public void testGetMappingForbiddenWhenNoValidJWT() {
        // GIVEN
        String url = getMonstersUrl();
        // WHEN

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // THEN
        assertThat(response.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    @Test
    public void testGetAllByUserIdMapping() {
        // GIVEN
        String url = getMonstersUrl();

        // WHEN
        HttpEntity<Void> entity = getValidAuthorizationEntity(null);
        ResponseEntity<Monster[]> response = restTemplate.exchange(url,HttpMethod.GET,entity, Monster[].class);

        // THEN
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        List<Monster> stockMonsters = new ArrayList<>(List.of(
                Monster.builder()
                .id("someId2")
                .userId("facebook@1234")
                .name("someName2")
                .image("someImage2")
                .scoreDoneTasks(4)
                .payoutDoneRewards(50)
                .build()
        ));

        assertThat(response.getBody(), is(stockMonsters.toArray()));
    }

    @Test
    public void postMonsterShouldAddANewMonster() {
        // GIVEN
        String url = getMonstersUrl();
        AddMonsterDto monsterToAdd = new AddMonsterDto(
                "some name",
                "some image"
        );
        when(mockedIdUtils.generateId()).thenReturn("some generated id");

        // WHEN
        HttpEntity<AddMonsterDto> entity = getValidAuthorizationEntity(monsterToAdd);
        ResponseEntity<Monster> response = restTemplate.exchange(url, HttpMethod.POST,entity, Monster.class);

        // THEN
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(Monster.builder()
                .id("some generated id")
                .userId("facebook@1234")
                .name("some name")
                .image("some image")
                .scoreDoneTasks(0)
                .payoutDoneRewards(0)
                .build()
        ));
    }

    @Test
    public void updateMonsterShouldUpdateExistingMonster() {
        //GIVEN
        String url = getMonstersUrl() + "/someId2";

        UpdateMonsterDto updateMonster = UpdateMonsterDto.builder()
                .id("someId2")
                .name("updatedName")
                .image("updatedImage")
                .build();

        //WHEN
        HttpEntity<UpdateMonsterDto> entity = getValidAuthorizationEntity(updateMonster);
        ResponseEntity<Monster> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Monster.class);

        //THEN
        Optional<Monster> savedMonster = monsterDao.findById("someId2");
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        Monster expectedMonster = Monster.builder()
                .id("someId2")
                .userId("facebook@1234")
                .name("updatedName")
                .image("updatedImage")
                .payoutDoneRewards(50)
                .scoreDoneTasks(4)
                .build();

        assertThat(response.getBody(), is(expectedMonster));
        assertThat(savedMonster.get(), is(expectedMonster));
    }

    @Test
    public void updateMonsterWhenNoExistingMonsterShouldReturnNotFound() {
        //GIVEN
        String url = getMonstersUrl() + "/someIdXY";

        UpdateMonsterDto updateMonster = UpdateMonsterDto.builder()
                .id("someIdXY")
                .name("updatedName")
                .image("updatedImage")
                .build();

        //WHEN
        HttpEntity<UpdateMonsterDto> entity = getValidAuthorizationEntity(updateMonster);
        ResponseEntity<Monster> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Monster.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    public void updateMonsterWithNotMatchingUserIdShouldReturnForbidden() {
        //GIVEN
        String url = getMonstersUrl() + "/someId";

        UpdateMonsterDto updateMonster = UpdateMonsterDto.builder()
                .id("someId")
                .name("updatedName")
                .image("updatedImage")
                .build();

        //WHEN
        HttpEntity<UpdateMonsterDto> entity = getValidAuthorizationEntity(updateMonster);
        ResponseEntity<Monster> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Monster.class);

        //THEN
        Optional<Monster> savedMonster = monsterDao.findById("someId");
        assertThat(response.getStatusCode(), is(HttpStatus.FORBIDDEN));

        Monster expectedMonster = Monster.builder()
                .id("someId")
                .userId("someUserId")
                .name("someName")
                .image("someImage")
                .payoutDoneRewards(0)
                .scoreDoneTasks(0)
                .build();

        assertThat(savedMonster.get(), is(expectedMonster));
    }

    @Test
    public void updateMonsterWithNotMatchingIdsShouldReturnBadRequest() {
        //GIVEN
        String url = getMonstersUrl() + "/someId";

        UpdateMonsterDto updateMonster = UpdateMonsterDto.builder()
                .id("someId2")
                .name("updatedName")
                .image("updatedImage")
                .build();

        //WHEN
        HttpEntity<UpdateMonsterDto> entity = getValidAuthorizationEntity(updateMonster);
        ResponseEntity<Monster> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Monster.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void removeMonsterShouldRemoveExistingMonster() {
        //GIVEN
        String url = getMonstersUrl() + "/someId2";

        //WHEN
        HttpEntity<Void> entity = getValidAuthorizationEntity(null);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        boolean monsterPresent = monsterDao.findById("someId2").isPresent();
        assertThat(monsterPresent, is(false));
    }

    @Test
    public void removeMonsterShouldRemoveAllRelatedTasks() {
        //GIVEN
        String url = getMonstersUrl() + "/someId2";

        //WHEN
        HttpEntity<Void> entity = getValidAuthorizationEntity(null);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        boolean relatedTasks = taskDao.findAllByMonsterId("someId2").isEmpty();
        assertThat(relatedTasks, is(true));
    }

    @Test
    public void removeMonsterShouldRemoveAllRelatedRewards() {
        //GIVEN
        String url = getMonstersUrl() + "/someId2";

        //WHEN
        HttpEntity<Void> entity = getValidAuthorizationEntity(null);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        boolean relatedRewards = rewardDao.findAllByMonsterId("someId2").isEmpty();
        assertThat(relatedRewards, is(true));
    }

    @Test
    public void removeMonsterWithNotMatchingUserIdShouldReturnForbidden() {
        //GIVEN
        String url = getMonstersUrl() + "/someId";

        //WHEN
        HttpEntity<Void> entity = getValidAuthorizationEntity(null);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.FORBIDDEN));
        boolean monsterPresent = monsterDao.findById("someId").isPresent();
        assertThat(monsterPresent, is(true));
    }

    @Test
    public void removeMonsterWhenNoExistingMonsterShouldReturnNotFound() {
        //GIVEN
        String url = getMonstersUrl() + "/someIdXY";

        //WHEN
        HttpEntity<Void> entity = getValidAuthorizationEntity(null);
        ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

        //THEN
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

}