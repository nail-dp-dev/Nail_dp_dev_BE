package com.backend.naildp.dto.comment;

import java.time.LocalDateTime;

import com.backend.naildp.entity.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentInfoResponse {

	private long commentId;
	private String commentContent;
	private String profileUrl;
	private String commentUserNickname;
	private LocalDateTime commentDate;
	private long likeCount;
	private long replyCount;

	public static CommentInfoResponse of(Comment comment) {
		return CommentInfoResponse.builder()
			.commentId(comment.getId())
			.commentContent(comment.getCommentContent())
			.profileUrl(comment.getUser().getThumbnailUrl())
			.commentUserNickname(comment.getUser().getNickname())
			.commentDate(comment.getCreatedDate())
			.likeCount(comment.getLikeCount())
			.replyCount(0)
			.build();
	}
}
