package com.example.prj3be.service;

import com.example.prj3be.domain.*;

import com.example.prj3be.repository.*;
import com.querydsl.core.BooleanBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.prj3be.domain.QBoard.board;

@Service
@Transactional
//@RequiredArgsConstructor
public class BoardService {
    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final LikeRepository likeRepository;
    private final BoardFileRepository boardFileRepository;
    private final AlbumGenreRepository albumGenreRepository;
    private final CartItemRepository cartItemRepository;

    @Value("${image.file.prefix}")
    private String urlPrefix;

    @Value("${aws.s3.bucket.name}")
    private String bucket;

    private final S3Client s3;


    public Page<Board> boardListAll(Pageable pageable, String title, AlbumFormat albumFormat, List<AlbumDetail> albumDetails, String minPrice, String maxPrice, Long stockQuantity) {
        QBoard qBoard = board;
        BooleanBuilder builder = new BooleanBuilder();

        //Title 검색조건
        if (title != null && !title.isEmpty()) {
            builder.and(qBoard.title.containsIgnoreCase(title));
        }
        //AlbumFormat 검색조건
        if (albumFormat != null) {
            builder.and(qBoard.albumFormat.eq(albumFormat));
        }
        //AlbumDetail 검색조건
        if (albumDetails != null && !albumDetails.isEmpty()) {
            BooleanBuilder genreBuilder = new BooleanBuilder();
            for (AlbumDetail detail :
                    albumDetails) {
                genreBuilder.or(qBoard.albumGenres.any().albumDetail.eq(detail));
            }
            builder.and(genreBuilder);
        }

        //가격 검색조건
        if (minPrice != null && !minPrice.isEmpty()) {
            double minPriceValue = Double.parseDouble(minPrice);
            builder.and(qBoard.price.goe(minPriceValue));
        }
        if (maxPrice != null && !maxPrice.isEmpty()) {
            double maxPriceValue = Double.parseDouble(maxPrice);
            builder.and(qBoard.price.loe(maxPriceValue));
        }
        return boardRepository.findAll(builder, pageable);
    }


    public void save(Board board, List<AlbumDetail> albumDetails, MultipartFile[] files) throws IOException {
        boardRepository.save(board);    //jpa의 save()메소드엔 파일을 넣지 못함

        List<AlbumGenre> albumGenreList = new ArrayList<>();
        for (AlbumDetail detail : albumDetails) {
            AlbumGenre albumGenre = AlbumGenre.builder()
                    .board(board)
                    .albumDetail(detail)
                    .build();
            albumGenreList.add(albumGenre);
        }
        albumGenreRepository.saveAll(albumGenreList);   // List처럼 n개의 데이터 저장


        BoardFile boardFile = new BoardFile();
        Optional<Board> findBoard = boardRepository.findById(board.getId()); //board.getId()로 변경
        Board savedBoard = findBoard.get();


        for (int i = 0;
             i < files.length; i++) {
            String url = urlPrefix + "prj3/" + board.getId() + "/" + files[i].getOriginalFilename();
            boardFile.setFileName(files[i].getOriginalFilename());
            boardFile.setFileUrl(url);
            boardFile.setBoard(savedBoard);
            boardFileRepository.save(boardFile);    //boardFile 테이블에 files 정보(fileName, fileUrl) 저장
            upload(files[i], board.getId());
        }

    }

    //AWS s3에 파일 업로드
    private void upload(MultipartFile file, Long id) throws IOException {
        String key = "prj3/" + id + "/" + file.getOriginalFilename();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3.putObject(objectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    public String get(Long id, BoardFile boardFile) {

//        id로 board에 있는 id, title, price값을 가져와서 board에 넣음
        Optional<Board> board = boardRepository.findById(id);

        //파일 table에서 id로 파일명을 알아온 후에 boadFiles에 집어넣음
//        Optional<BoardFile> boardFiles = boardFileRepository.findById(id);

        //Optional은 foreach 사용 불가. Optional은 set메소드 이용해서 내부의 값 직접 설정할수 없음
        // optional -> get메소드
//        if (boardFiles.isPresent()) {
//            BoardFile boardFile1 = boardFiles.get(); //보드 파일이 존재한다면 파일에 있는걸 boardFile1에 넣음
//            String url = urlPrefix + "prj3/"+ id +"/" + boardFile1.getFileName(); //보드파일1의 파일name을 url에 넣음
//            boardFile1.setFileUrl(url); //boardFile1에 setter로 FileUrl필드에 url값을 집어넣음
//            String fileUrl = boardFile1.getFileUrl();  //boardFile에 들어간 url값을 fileUrl변수에 넣음
//            return fileUrl;
//        } else {
//            return null;
//        }
        return "ddd";
    }

    public Optional<Board> getBoardById(Long id) {
        return boardRepository.findById(id);
    }

    public Board update(Long id, Board updateBoard) {
        Optional<Board> boardById = boardRepository.findById(id);
        if (boardById.isPresent()) {
            Board board1 = boardById.get();
            board1.setTitle(updateBoard.getTitle());
            board1.setArtist(updateBoard.getArtist());
            board1.setPrice(updateBoard.getPrice());
            board1.setReleaseDate(updateBoard.getReleaseDate());
            board1.setContent(updateBoard.getContent());
            board1.setStockQuantity(updateBoard.getStockQuantity());

//            앨범 포멧은 변경할 수 없는 걸로 해서 추가 안했어요.
            return boardRepository.save(board1);
        }
        return null;
    }

    public void update(Long id, Board updateBboard, MultipartFile uploadFiles) throws IOException {
        Board updatedBoard = update(id, updateBboard);

        BoardFile boardFile = new BoardFile();
        String url = urlPrefix + "prj3/" + id + "/" + uploadFiles.getOriginalFilename();
        boardFile.setFileName(uploadFiles.getOriginalFilename());
        boardFile.setFileUrl(url);
        boardFile.setBoard(updatedBoard);
        boardFileRepository.save(boardFile);    //boardFile 테이블에 files 정보(fileName, fileUrl) 저장
        upload(uploadFiles, id);
    }

    public void removeFileIds(List<Long> removeFileIds){
        //기존 이미지 삭제
        removeFileIds.forEach(fileId->boardFileRepository.deleteById(fileId));
    }

    public void delete(Long id) {
        boardFileRepository.deleteBoardFileByBoardId(id);
        likeRepository.deleteByBoardId(id);
        albumGenreRepository.deleteAlbumGenreByBoardId(id);
        commentRepository.deleteCommentByBoardId(id);
        // 카트가 있다면 삭제 없다면 패스

        int i = cartItemRepository.deleteCartItemByBoardId(id);
        System.out.println("지워진 갯수 = " + i);

        boardRepository.deleteById(id);
    }

    public List<String> getBoardURL(Long id) {
        return boardFileRepository.findFileUrlsByBoardId(id);
    }

    public BoardService(CommentRepository commentRepository, BoardRepository boardRepository, LikeRepository likeRepository, BoardFileRepository boardFileRepository, AlbumGenreRepository albumGenreRepository, CartItemRepository cartRepository, S3Client s3) {
        this.commentRepository = commentRepository;
        this.boardRepository = boardRepository;
        this.likeRepository = likeRepository;
        this.boardFileRepository = boardFileRepository;
        this.albumGenreRepository = albumGenreRepository;
        this.cartItemRepository = cartRepository;
        this.s3 = s3;
    }

}
