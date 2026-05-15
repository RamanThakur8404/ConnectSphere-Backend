package com.connectsphere.search_service.event;

import com.connectsphere.search_service.constant.LogMessages;
import com.connectsphere.search_service.config.RabbitMQConfig;
import com.connectsphere.search_service.service.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventConsumer {

	private final SearchService searchService;

	// Handles post created/updated → index hashtags 
	@RabbitListener(queues = RabbitMQConfig.QUEUE_POST_INDEXED)
	public void handlePostIndexed(PostIndexEvent event) {

		log.info("EVENT_RECEIVED type=POST_INDEX postId={}", event.getPostId());

		try {
			searchService.indexPost(event.getPostId(), event.getContent(), event.getHashtags());

			log.info("EVENT_PROCESSED type=POST_INDEX postId={}", event.getPostId());

		} catch (Exception ex) {
			log.error("EVENT_FAILED type=POST_INDEX postId={}", event.getPostId(), ex);
		}
	}

	// Handles post deleted → remove index 
	@RabbitListener(queues = RabbitMQConfig.QUEUE_POST_REMOVED)
	public void handlePostRemoved(PostRemoveEvent event) {

		log.info("EVENT_RECEIVED type=POST_REMOVE postId={}", event.getPostId());

		try {
			searchService.removePostIndex(event.getPostId());

			log.info("EVENT_PROCESSED type=POST_REMOVE postId={}", event.getPostId());

		} catch (Exception ex) {
			log.error("EVENT_FAILED type=POST_REMOVE postId={}", event.getPostId(), ex);
		}
	}
}
