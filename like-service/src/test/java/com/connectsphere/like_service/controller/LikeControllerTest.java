package com.connectsphere.like_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;
import com.connectsphere.like_service.dto.ChangeReactionDTO;
import com.connectsphere.like_service.dto.LikeRequestDTO;
import com.connectsphere.like_service.dto.LikeResponseDTO;
import com.connectsphere.like_service.dto.ReactionSummaryDTO;
import com.connectsphere.like_service.service.LikeService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = LikeController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LikeController Tests")
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LikeService likeService;

    private LikeResponseDTO responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new LikeResponseDTO();
        responseDto.setLikeId(1);
        responseDto.setUserId(100);
        responseDto.setTargetId(10);
        responseDto.setTargetType(TargetType.POST);
        responseDto.setReactionType(ReactionType.LIKE);
    }

    @Test
    void likeTarget() throws Exception {
        LikeRequestDTO request = new LikeRequestDTO();
        request.setUserId(100);
        request.setTargetId(10);
        request.setTargetType(TargetType.POST);
        request.setReactionType(ReactionType.LIKE);

        when(likeService.likeTarget(any(LikeRequestDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.likeId").value(1));
    }

    @Test
    void unlikeTarget() throws Exception {
        mockMvc.perform(delete("/api/v1/likes")
                .param("userId", "100")
                .param("targetId", "10")
                .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void hasLiked() throws Exception {
        when(likeService.hasLiked(100, 10, TargetType.POST)).thenReturn(true);

        mockMvc.perform(get("/api/v1/likes/has-liked")
                .param("userId", "100")
                .param("targetId", "10")
                .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getLikesByTarget() throws Exception {
        when(likeService.getLikesByTarget(10, TargetType.POST)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/likes/target/10")
                .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].likeId").value(1));
    }

    @Test
    void getLikesByUser() throws Exception {
        when(likeService.getLikesByUser(100)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/likes/user/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].likeId").value(1));
    }

    @Test
    void getLikeCount() throws Exception {
        when(likeService.getLikeCount(10, TargetType.POST)).thenReturn(5);

        mockMvc.perform(get("/api/v1/likes/count")
                .param("targetId", "10")
                .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    void getLikeCountByType() throws Exception {
        when(likeService.getLikeCountByType(10, TargetType.POST, ReactionType.LIKE)).thenReturn(3);

        mockMvc.perform(get("/api/v1/likes/count/by-type")
                .param("targetId", "10")
                .param("targetType", "POST")
                .param("reactionType", "LIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void getReactionSummary() throws Exception {
        ReactionSummaryDTO summary = new ReactionSummaryDTO();
        summary.setTargetId(10);
        summary.setTotalCount(1);

        when(likeService.getReactionSummary(10, TargetType.POST)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/likes/summary/10")
                .param("targetType", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetId").value(10));
    }

    @Test
    void changeReaction() throws Exception {
        ChangeReactionDTO req = new ChangeReactionDTO();
        req.setUserId(100);
        req.setTargetId(10);
        req.setTargetType(TargetType.POST);
        req.setNewReactionType(ReactionType.LOVE);

        when(likeService.changeReaction(any(ChangeReactionDTO.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/likes/change-reaction")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeId").value(1));
    }
}
