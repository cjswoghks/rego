package com.clipclap.rego.controller;


import com.clipclap.rego.model.entitiy.Question;
import com.clipclap.rego.model.entitiy.User;
import com.clipclap.rego.service.QuestionService;
import com.clipclap.rego.validation.QuestionForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RequestMapping("/question")
@RequiredArgsConstructor
@Controller
public class QuestionController {

    private final QuestionService questionService;
    private final com.clipclap.rego.service.UserService userService; // UserService를 주입하도록 수정

    // 삭제 처리
    @GetMapping("/delete/{id}")
    public String questionDelete(@PathVariable("id") Integer id, Principal principal) {
        Question question = questionService.getQuestion(id);
        if (!question.getWriter().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제 권한이 없습니다.");
        }
        questionService.delete(question);
        return "redirect:/question/list";
    }

    // 질문 수정 폼을 보여줌
    @PreAuthorize("isAuthenticated()") //인증을 요하는 메서드
    @GetMapping("/modify/{id}")
    public String qeuestionModify(QuestionForm questionForm,
                                  @PathVariable("id") Integer id,Principal principal){
        //1.파라미터받기
        //2.비즈니스로직수행
        Question question = questionService.getQuestion(id); //질문상세
        if ( !question.getWriter().getUsername().equals(principal.getName()) ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"수정권한이 없습니다.");
        }
        questionForm.setSubject(question.getSubject());
        questionForm.setContent(question.getContent());
        //3.Model  //4.View
        return "question_form"; //질문등록폼으로 이동
    }
    // 질문 수정 처리
    @PostMapping("/modify/{id}")
    public String modify(@Valid QuestionForm questionForm, BindingResult bindingResult,
                         @PathVariable("id") Integer id, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        Question question = questionService.getQuestion(id);
        if (!question.getWriter().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정 권한이 없습니다.");
        }
        questionService.modify(question, questionForm.getSubject(), questionForm.getContent());
        return String.format("redirect:/question/detail/%d", id);
    }

    // 질문 등록 폼을 보여줌
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/add")
    public String add(QuestionForm questionForm) {
        return "question_form";
    }

    // 질문 등록 처리
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/add")
    public String questionAdd(@Valid QuestionForm questionForm, BindingResult bindingResult,
                              Principal principal) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }
        User user = userService.getUser(principal.getName());
        questionService.add(questionForm.getSubject(), questionForm.getContent(), user);
        return "redirect:/question/list";
    }

    // 질문 상세 조회
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Integer id, Model model,
                         com.clipclap.rego.validation.AnswerForm answerForm) {
        Question question = questionService.getQuestion(id);
        model.addAttribute("question", question);
        return "question_detail";
    }

    // 페이징 기능이 있는 질문 목록 조회
    @GetMapping("/list")
    public String questionList(Model model,
                               @RequestParam(value = "page", defaultValue = "0") int page) {
        Page<Question> questionPage = questionService.getList(page);
        model.addAttribute("questionPage", questionPage);
        return "question_list";
    }

    // 질문 추천
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String questionVote(@PathVariable("id") Integer id, Principal principal) {
        Question question = questionService.getQuestion(id);
        User user = userService.getUser(principal.getName());
        questionService.vote(question, user);
        return String.format("redirect:/question/detail/%d", id);
    }
}