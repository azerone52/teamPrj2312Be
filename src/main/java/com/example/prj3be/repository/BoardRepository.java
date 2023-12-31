package com.example.prj3be.repository;

import com.example.prj3be.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

// QuerydslPredicateExecutors는 조회만 가능.  <Board> 쓴 곳에만 적용됨
//조건 주는 방법 : where조건절 사용, boolean builder 두가지가 있음
public interface BoardRepository extends
        JpaRepository<Board, Long>,
        QuerydslPredicateExecutor<Board>{

}




