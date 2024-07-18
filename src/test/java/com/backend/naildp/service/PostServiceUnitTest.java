package com.backend.naildp.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import com.backend.naildp.common.Boundary;
import com.backend.naildp.config.JpaAuditingConfiguration;
import com.backend.naildp.dto.home.HomePostResponse;
import com.backend.naildp.dto.home.PostSummaryResponse;
import com.backend.naildp.entity.ArchivePost;
import com.backend.naildp.entity.Photo;
import com.backend.naildp.entity.Post;
import com.backend.naildp.entity.PostLike;
import com.backend.naildp.repository.ArchivePostRepository;
import com.backend.naildp.repository.PostLikeRepository;
import com.backend.naildp.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
@Import(JpaAuditingConfiguration.class)
class PostServiceUnitTest {

	@InjectMocks
	PostService postService;

	@Mock
	PostRepository postRepository;

	@Mock
	ArchivePostRepository archivePostRepository;

	@Mock
	PostLikeRepository postLikeRepository;

	@Mock
	AuditingHandler auditingHandler;

	final static String NICKNAME = "mj";
	final static int POST_CNT = 20;
	final static int PAGE_NUMBER = 0;
	final static int PAGE_SIZE = 20;

	@DisplayName("최신 게시물 조회 단위 테스트 - 첫 호출")
	@Test
	void newPosts() {
		//given
		long cursorPostId = -1L;
		PageRequest pageRequest = PageRequest.of(PAGE_NUMBER, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));

		List<Post> posts = createTestPosts(POST_CNT);
		Slice<Post> pagedPost = new SliceImpl<>(posts, pageRequest, true);
		List<ArchivePost> archivePosts = pagedPost.stream()
			.map(post -> new ArchivePost(null, post))
			.collect(Collectors.toList());
		List<PostLike> postLikes = pagedPost.stream()
			.map(post -> new PostLike(null, post))
			.collect(Collectors.toList());

		when(postRepository.findPostsByBoundaryNotAndTempSaveFalse(eq(Boundary.NONE), eq(pageRequest))).thenReturn(
			pagedPost);
		when(archivePostRepository.findAllByArchiveUserNickname(eq(NICKNAME))).thenReturn(archivePosts);
		when(postLikeRepository.findAllByUserNickname(eq(NICKNAME))).thenReturn(postLikes);

		//when
		PostSummaryResponse postSummaryResponse = postService.homePosts("NEW", PAGE_SIZE,
			cursorPostId, NICKNAME);
		Slice<HomePostResponse> homePostResponses = postSummaryResponse.getPostSummaryList();
		List<HomePostResponse> likedPostResponses = homePostResponses.stream()
			.filter(HomePostResponse::getLike)
			.collect(Collectors.toList());
		List<HomePostResponse> savedPostResponses = homePostResponses.stream()
			.filter(HomePostResponse::getSaved)
			.collect(Collectors.toList());

		//then
		verify(postRepository).findPostsByBoundaryNotAndTempSaveFalse(Boundary.NONE, pageRequest);
		verify(postRepository, never()).findPostsByIdBeforeAndBoundaryNotAndTempSaveIsFalse(anyLong(),
			any(Boundary.class), any(PageRequest.class));
		verify(archivePostRepository).findAllByArchiveUserNickname(NICKNAME);
		verify(postLikeRepository).findAllByUserNickname(NICKNAME);

		assertThat(homePostResponses.getNumber()).isEqualTo(0);
		assertThat(homePostResponses.getSize()).isEqualTo(20);
		assertThat(homePostResponses.isFirst()).isTrue();
		assertThat(homePostResponses.hasNext()).isTrue();

		assertThat(likedPostResponses.size()).isEqualTo(20);
		assertThat(savedPostResponses.size()).isEqualTo(20);

		System.out.println("homePostResponses = " + homePostResponses.hasNext());
	}

	@DisplayName("최신 게시물 조회 단위 테스트 - 두번쨰 호출부터")
	@Test
	void newPostsWithCursorId() {
		//given
		long cursorPostId = 10L;
		PageRequest pageRequest = PageRequest.of(PAGE_NUMBER, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));

		List<Post> posts = createTestPosts(POST_CNT);
		Slice<Post> pagedPost = new SliceImpl<>(posts, pageRequest, false);
		List<ArchivePost> archivePosts = pagedPost.stream()
			.map(post -> new ArchivePost(null, post))
			.collect(Collectors.toList());
		List<PostLike> postLikes = pagedPost.stream()
			.map(post -> new PostLike(null, post))
			.collect(Collectors.toList());

		when(postRepository.findPostsByIdBeforeAndBoundaryNotAndTempSaveIsFalse(eq(cursorPostId), eq(Boundary.NONE),
			any(PageRequest.class))).thenReturn(pagedPost);
		when(archivePostRepository.findAllByArchiveUserNickname(NICKNAME)).thenReturn(archivePosts);
		when(postLikeRepository.findAllByUserNickname(NICKNAME)).thenReturn(postLikes);

		//when
		PostSummaryResponse postSummaryResponse = postService.homePosts("NEW", PAGE_SIZE, cursorPostId, NICKNAME);
		Slice<HomePostResponse> homePostResponses = postSummaryResponse.getPostSummaryList();
		List<HomePostResponse> likedPostResponses = homePostResponses.stream()
			.filter(HomePostResponse::getLike)
			.collect(Collectors.toList());
		List<HomePostResponse> savedPostResponses = homePostResponses.stream()
			.filter(HomePostResponse::getSaved)
			.collect(Collectors.toList());

		//then
		verify(postRepository, never()).findPostsByBoundaryNotAndTempSaveFalse(Boundary.ALL, pageRequest);
		verify(postRepository).findPostsByIdBeforeAndBoundaryNotAndTempSaveIsFalse(eq(cursorPostId),
			any(Boundary.class), any(PageRequest.class));
		verify(archivePostRepository).findAllByArchiveUserNickname(NICKNAME);
		verify(postLikeRepository).findAllByUserNickname(NICKNAME);

		assertThat(homePostResponses.getNumber()).isEqualTo(0);
		assertThat(homePostResponses.getSize()).isEqualTo(20);
		assertThat(homePostResponses.isFirst()).isTrue();
		assertThat(homePostResponses.hasNext()).isFalse();

		assertThat(likedPostResponses.size()).isEqualTo(20);
		assertThat(savedPostResponses.size()).isEqualTo(20);
	}

	@DisplayName("좋아요한 게시글 조회 테스트")
	@Test
	void findPagedLikedPost() {
		//given
		String nickname = "mj";
		int postCnt = 20;
		int pageSize = 20;
		PageRequest pageRequest = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdDate"));

		List<PostLike> postLikes = createPostLikes(postCnt);
		PageImpl<PostLike> pagedPostLikes = new PageImpl<>(postLikes, pageRequest, pageSize);
		Page<Post> pagedPosts = pagedPostLikes.map(PostLike::getPost);
		List<ArchivePost> archivePosts = pagedPosts.stream()
			.map(post -> new ArchivePost(null, post))
			.collect(Collectors.toList());

		when(postLikeRepository.findPagedPostLikesByBoundaryOpened(any(PageRequest.class), anyString(),
			any(Boundary.class))).thenReturn(pagedPostLikes);
		when(archivePostRepository.findAllByArchiveUserNickname(nickname)).thenReturn(archivePosts);

		//when
		Page<HomePostResponse> likedPostResponses = postService.findLikedPost(nickname, 0);
		Page<Boolean> savedList = likedPostResponses.map(HomePostResponse::getSaved);
		Page<Boolean> likedList = likedPostResponses.map(HomePostResponse::getLike);

		//then
		verify(postLikeRepository).findPagedPostLikesByBoundaryOpened(any(PageRequest.class), anyString(),
			any(Boundary.class));
		verify(archivePostRepository).findAllByArchiveUserNickname(nickname);
		assertThat(savedList.getTotalElements()).isEqualTo(20);
		assertThat(likedList.getTotalElements()).isEqualTo(20);
	}

	@DisplayName("익명 사용자 - 최신 게시글 조회 테스트")
	@Test
	void recentPostsAccessByAnonymousUser() {
		//given
		long cursorId = -1L;
		String nickname = "";
		List<Post> posts = createTestPosts(POST_CNT);
		PageRequest pageRequest = PageRequest.of(PAGE_NUMBER, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"));
		Slice<Post> pagedPost = new SliceImpl<>(posts, pageRequest, false);

		when(postRepository.findPostsByBoundaryAndTempSaveFalse(eq(Boundary.ALL), eq(pageRequest)))
			.thenReturn(pagedPost);

		//when
		PostSummaryResponse postSummaryResponse = postService.homePosts("NEW", PAGE_SIZE, cursorId, nickname);

		//then
		verify(postRepository).findPostsByBoundaryAndTempSaveFalse(Boundary.ALL, pageRequest);
		verify(postRepository, never())
			.findPostsByBoundaryNotAndTempSaveFalse(any(Boundary.class), any(PageRequest.class));
		verify(postRepository, never())
			.findPostsByIdBeforeAndBoundaryNotAndTempSaveIsFalse(anyLong(), any(Boundary.class),
				any(PageRequest.class));
		verify(archivePostRepository, never()).findAllByArchiveUserNickname(NICKNAME);
		verify(postLikeRepository, never()).findAllByUserNickname(NICKNAME);
	}

	private List<Post> createTestPosts(int postCnt) {
		List<Post> posts = new ArrayList<>();
		for (int i = 0; i < postCnt; i++) {
			Post post = new Post(null, "" + i, 0L, Boundary.ALL, false);
			Photo photo = new Photo(post, "url" + i, "photo" + i);
			auditingHandler.markCreated(post);
			post.addPhoto(photo);
			posts.add(post);
		}
		return posts;
	}

	private List<PostLike> createPostLikes(int postCnt) {
		List<PostLike> postLikes = new ArrayList<>();
		List<Post> posts = createTestPosts(postCnt);
		posts.forEach(post -> {
			PostLike postLike = new PostLike(null, post);
			postLikes.add(postLike);
		});
		return postLikes;
	}
}