package com.backend.naildp.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.naildp.common.NotificationType;
import com.backend.naildp.entity.Notification;
import com.backend.naildp.entity.User;
import com.backend.naildp.exception.CustomException;
import com.backend.naildp.exception.ErrorCode;
import com.backend.naildp.repository.NotificationRepository;
import com.backend.naildp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Transactional
	public long generateFollowNotification(UUID publisherId, UUID subscriberId) {
		User receiver = userRepository.findById(subscriberId)
			.orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다.", ErrorCode.NOT_FOUND));
		User followerUser = userRepository.findById(publisherId)
			.orElseThrow(() -> new CustomException("사용자를 찾을 수 없습니다.", ErrorCode.NOT_FOUND));
		Notification followNotification = Notification.builder()
			.receiver(receiver)
			.content(followerUser.getNickname())
			.notificationType(NotificationType.FOLLOW)
			.isRead(false)
			.build();
		Notification savedNotification = notificationRepository.save(followNotification);

		return savedNotification.getId();
	}
}