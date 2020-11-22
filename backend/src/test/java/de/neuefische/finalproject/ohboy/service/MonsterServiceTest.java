package de.neuefische.finalproject.ohboy.service;

import de.neuefische.finalproject.ohboy.dao.MonsterMongoDao;
import de.neuefische.finalproject.ohboy.dto.AddMonsterDto;
import de.neuefische.finalproject.ohboy.dto.UpdateMonsterDto;
import de.neuefische.finalproject.ohboy.model.Monster;
import de.neuefische.finalproject.ohboy.utils.IdUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

class MonsterServiceTest {

    //Given
    final IdUtils idUtils = mock(IdUtils.class);
    final MonsterMongoDao monsterMongoDao = mock(MonsterMongoDao.class);

    final MonsterService monsterService = new MonsterService(monsterMongoDao, idUtils);

    final List<Monster> monsters = new ArrayList<>(List.of(
            new Monster("some id", "@facebookSomeUserId", "some name", "some image", 0, 0, 0, 0, 0, 0, 0),
            new Monster("some id2", "@facebookSomeUserId2", "some name2", "some image2", 0, 0, 0, 0, 0, 0, 0),
            new Monster("some id3", "@facebookSomeUserId3", "some name3", "some image3", 0, 0, 0, 0, 0, 0, 0),
            new Monster("some id4", "@facebookSomeUserI4", "some name4", "some image4", 0, 0, 0, 0, 0, 0, 0)
    ));

    final List<Monster> getStockMonsters(){
        return monsters;
    }

    @Test
    @DisplayName("The \"getAll\" method should return all Monster objects in a list")
    void getAll() {
        when(monsterMongoDao.findAll()).thenReturn(getStockMonsters());

        //When
        List<Monster> allMonsters = monsterService.getAll();

        //Then
        assertThat(allMonsters, containsInAnyOrder(getStockMonsters().toArray()));
    }

    @Test
    @DisplayName("The \"findAllByUserId\" method should return all Monster objects that match the UserId in a list")
    void findAllByUserId() {
        //Given
        String userId = "@facebookSomeUserId";

        List<Monster> expectedMonsters = new ArrayList<>(List.of(new Monster(
                "userId", userId, "some name", "some image", 0, 0, 0, 0, 0, 0, 0
        )));

        when(monsterMongoDao.findAllByUserId(userId)).thenReturn(expectedMonsters);

        //When
        List<Monster> resultMonsters = monsterService.findAllByUserId(userId);

        //Then
        assertThat(resultMonsters, is(expectedMonsters));
    }

    @Test
    @DisplayName("The \"add\" method should return the added Monster object")
    void add() {
        //Given
        String expectedId = "randomId";

        AddMonsterDto monsterDto = new AddMonsterDto(
                "some name",
                "some UserId",
                "some image"
        );

        Monster expectedMonster = new Monster(
                expectedId,
                "some UserId",
                "some name",
                "some image",
                0, 0, 0, 0, 0, 0, 0
        );

        when(idUtils.generateId()).thenReturn(expectedId);
        when(monsterMongoDao.save(expectedMonster)).thenReturn(expectedMonster);

        //When
        Monster newMonster = monsterService.add(monsterDto);

        //Then
        assertThat(newMonster, is(expectedMonster));
        verify(monsterMongoDao).save(expectedMonster);
    }

    @Test
    @DisplayName("The \"update\" method should return the updated Monster object")
    void update() {
        //Given
        String monsterId = "randomId";

        UpdateMonsterDto monsterDto = new UpdateMonsterDto(
                monsterId,
                "some userId",
                "some updatedName",
                "some updatedImage"
        );

        Monster monster = new Monster(
                monsterId,
                "some userId",
                "some name",
                "some image",
                5, 10, 20, 2, 4, 6, 7
        );

        Monster updatedMonster = new Monster(
                monsterId,
                "some userId",
                "some updatedName",
                "some updatedImage",
                5, 10, 20, 2, 4, 6, 7
        );

        when(monsterMongoDao.findById(monsterId)).thenReturn(Optional.of(monster));
        when(monsterMongoDao.save(updatedMonster)).thenReturn(updatedMonster);

        //When
        Monster result = monsterService.update(monsterDto);

        //Then
        assertThat(result, is(updatedMonster));
        verify(monsterMongoDao).save(updatedMonster);
    }

    @Test
    @DisplayName("The \"update\" method should throw forbidden when user with not matching userId try to modify a Monster object")
    void updateForbidden() {
        //Given
        String monsterId = "randomId";

        UpdateMonsterDto monsterDto = new UpdateMonsterDto(
                monsterId,
                "some otherUserId",
                "some updatedName",
                "some updatedImage"
        );

        Monster monster = new Monster(
                monsterId,
                "some userId",
                "some name",
                "some image",
                5, 10, 20, 2, 4, 6, 7
        );

        when(monsterMongoDao.findById(monsterId)).thenReturn(Optional.of(monster));

        //When
        try {
            monsterService.update(monsterDto);
            fail("missing exception");
        } catch (ResponseStatusException exception) {
            assertThat(exception.getStatus(), is(HttpStatus.FORBIDDEN));
        }
    }

    @Test
    @DisplayName("The \"update\" method should throw not found when id not found")
    void updateNotFound() {
        //Given
        String monsterId = "randomId";

        UpdateMonsterDto monsterDto = new UpdateMonsterDto(
                monsterId,
                "some otherUserId",
                "some updatedName",
                "some updatedImage"
        );

        when(monsterMongoDao.findById(monsterId)).thenReturn(Optional.empty());

        //When
        try {
            monsterService.update(monsterDto);
            fail("missing exception");
        } catch (ResponseStatusException exception) {
            assertThat(exception.getStatus(), is(HttpStatus.NOT_FOUND));
        }
    }
}