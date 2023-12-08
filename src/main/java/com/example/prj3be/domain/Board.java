package com.example.prj3be.domain;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "prj3")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Board {
    @Id
    @GeneratedValue
    @Column(name = "board_id")
    private Long id;
    private String title;
    private String artist;

    @Enumerated(EnumType.STRING)
    private AlbumFormat albumFormat;

    private String price;
    private String agency;
    private String content;
    private LocalDate releaseDate;
    private Long stockQuantity;


    private String fileName;
    private String category;

//    @OneToOne
//    @JoinColumn(name = "item_id") //item_id를 외래키로 사용. item_id가 Board의 pk를 참조
//    private Item item;

    @OneToMany(mappedBy = "board")
    @JsonManagedReference
    private List<Comment> comments = new ArrayList<>();
    @OneToMany(mappedBy = "board")
    @JsonManagedReference
    private List<BoardFile> boardFiles = new ArrayList<>();

    public Board(Long id, String title, String artist, AlbumFormat albumFormat, String price, String agency, LocalDate releaseDate, List<Comment> comments) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumFormat = albumFormat;
        this.price = price;
        this.agency = agency;
        this.releaseDate = releaseDate;
        this.comments = comments;

    }
    public String getFileName() {
        return this.fileName;
    }
}