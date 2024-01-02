package com.example.prj3be.controller;

import com.example.prj3be.domain.*;
import com.example.prj3be.service.BoardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Optional;

@RestController
//@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {
    private final BoardService boardService;

    @GetMapping("list")
    public Page<Board> list(Pageable pageable,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) AlbumFormat albumFormat,
                            @RequestParam(required = false) String[] albumDetails,
                            @RequestParam(required = false) String minPrice,
                            @RequestParam(required = false) String maxPrice,
                            @RequestParam(required = false) Long stockQuantity
                        ) {

        List<AlbumDetail> albumDetailList = (albumDetails == null) ? null : Arrays.stream(albumDetails).map(AlbumDetail::valueOf).collect(Collectors.toList());

        Page<Board> boardListPage = boardService.boardListAll(pageable, title, albumFormat, albumDetailList, minPrice, maxPrice, stockQuantity);


        // stackoverflowerror 발생 가능한 지점
        return boardListPage;
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("add")
    public void add(@Validated Board saveBoard,
                    @RequestParam(required = false) String[] albumDetails,
                    @RequestParam(value = "uploadFiles[]", required = false) MultipartFile[] files) throws IOException {

        List<AlbumDetail> AlbumDetailList = Arrays.stream(albumDetails)
                .map(AlbumDetail::valueOf)
                .collect(Collectors.toList());

        boardService.save(saveBoard, AlbumDetailList , files);
    }


    @GetMapping("id/{id}")
    public Optional<Board> get(@PathVariable Long id) {
        return boardService.getBoardById(id);
    }
    @GetMapping("file/id/{id}")
    public List<String> getURL(@PathVariable Long id) {
        return boardService.getBoardURL(id);
    }


    @PutMapping("edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void update(@PathVariable Long id,
                       Board updateBboard,
                       @RequestParam(value="removeFileIds[]", required = false) List<Long> removeFileIds,
                       @RequestParam(value = "uploadFiles", required = false) MultipartFile uploadFiles) throws IOException {
        System.out.println("updateBboard.getBoardFiles() = " + updateBboard.getBoardFiles());
        System.out.println("uploadFiles = " + uploadFiles);
        System.out.println("removeFileIds = " + removeFileIds);
        System.out.println("updateBboard.getStockQuantity() = " + updateBboard.getStockQuantity());

        if(removeFileIds != null){
            boardService.removeFileIds(removeFileIds);
        }

        if (uploadFiles == null) {
            boardService.update(id, updateBboard);
        } else {
            boardService.update(id, updateBboard, uploadFiles);
        }
    }


    @DeleteMapping("remove/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        boardService.delete(id);
    }


    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }


}





