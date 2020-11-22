package de.neuefische.finalproject.ohboy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reward")
public class Reward {

    @Id
    private String id;
    private String monsterId;
    private String description;
    private int score;
    private Status status;
    private Instant timestampOfDone;
}
