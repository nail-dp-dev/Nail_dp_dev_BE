package com.backend.naildp.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import com.backend.naildp.common.Boundary;
import com.backend.naildp.common.UserRole;
import com.backend.naildp.config.JpaAuditingConfiguration;
import com.backend.naildp.dto.auth.LoginRequestDto;
import com.backend.naildp.entity.Follow;
import com.backend.naildp.entity.Photo;
import com.backend.naildp.entity.Post;
import com.backend.naildp.entity.SocialLogin;
import com.backend.naildp.entity.User;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DataJpaTest
@Import(JpaAuditingConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

	@Autowired
	PostRepository postRepository;

	@Autowired
	EntityManager em;

	private User mj;
	private User jw;
	private User gw;
	private List<Post> posts = new ArrayList<>();
	private List<Photo> photos = new ArrayList<>();

	private static final int PAGE_SIZE = 30;
	private static final int TOTAL_POST_CNT = 30;

	@BeforeEach
	void setup() {
		mj = createTestMember("x@naver.com", "mj", "0101111", 1L);
		jw = createTestMember("y@naver.com", "jw", "0102222", 2L);
		gw = createTestMember("z@naver.com", "gw", "0103333", 3L);

		createFollow(jw, mj);

		createTestPostWithPhoto(TOTAL_POST_CNT, mj, Boundary.ALL);
		createTestPostWithPhoto(TOTAL_POST_CNT, gw, Boundary.ALL);
		createTestPostWithPhoto(TOTAL_POST_CNT, mj, Boundary.FOLLOW);
		createTestPostWithPhoto(TOTAL_POST_CNT, gw, Boundary.FOLLOW);
		createTestPostWithPhoto(TOTAL_POST_CNT, mj, Boundary.NONE);
		createTestPostWithPhoto(TOTAL_POST_CNT, gw, Boundary.NONE);

		createTestTempSavePostAndPhoto(mj);
		createTestTempSavePostAndPhoto(jw);
		createTestTempSavePostAndPhoto(gw);

		postRepository.saveAllAndFlush(posts);
		photos.forEach(photo -> em.persist(photo));
		em.flush();
		em.clear();
		log.info("========= 사전 데이터 끝 =========");
	}

	@DisplayName("전체공개이거나 팔로우 공개이고 임시저장이 아닌 게시물 페이징 조회 테스트")
	@Test
	void offsetPagingPostsWithFollow() {
		//given
		int postCntOpened = TOTAL_POST_CNT * 3;
		PageRequest pageRequest = PageRequest.of(0, postCntOpened, Sort.by(Sort.Direction.DESC, "id"));
		List<Follow> follows = findFollowByFollowerNickname("jw");
		List<User> followingsByJw = follows.stream().map(Follow::getFollowing).collect(Collectors.toList());

		//when
		Slice<Post> recentPosts = postRepository.findRecentPostsByFollowing(followingsByJw, pageRequest);

		//then
		assertThat(recentPosts.getSize()).isEqualTo(postCntOpened);
		assertThat(recentPosts.getNumberOfElements()).isEqualTo(postCntOpened);
		assertThat(recentPosts.getNumber()).isEqualTo(0);
		assertThat(recentPosts.getSort().isSorted()).isTrue();
		assertThat(recentPosts).extracting("boundary").containsOnly(Boundary.FOLLOW, Boundary.ALL);
		assertThat(recentPosts).extracting("tempSave").containsOnly(false);
	}

	@DisplayName("전체공개이거나 팔로우 공개이고 임시저장이 아닌 게시물 페이징 조회 테스트 - 두번째 호출부터")
	@Test
	void cursorPagingPostsWithFollow() {
		//given
		Post oldestPost = findOldestPost();
		int postCntOpened = TOTAL_POST_CNT * 3;
		long oldestPostId = oldestPost.getId() + 1;
		System.out.println("oldestPostId = " + oldestPostId);

		PageRequest pageRequest = PageRequest.of(0, postCntOpened, Sort.by(Sort.Direction.DESC, "id"));
		List<Follow> follows = findFollowByFollowerNickname("jw");
		List<User> followingsByJw = follows.stream().map(Follow::getFollowing).collect(Collectors.toList());

		//when
		Slice<Post> secondRecentPosts = postRepository.findRecentPostsByIdAndFollowing(oldestPostId, followingsByJw,
			pageRequest);

		//then
		assertThat(secondRecentPosts.getSize()).isEqualTo(postCntOpened);
		assertThat(secondRecentPosts.getNumberOfElements()).isEqualTo(postCntOpened);
		assertThat(secondRecentPosts.getNumber()).isEqualTo(0);
		assertThat(secondRecentPosts.getSort().isSorted()).isTrue();
		assertThat(secondRecentPosts).extracting("boundary").containsOnly(Boundary.FOLLOW, Boundary.ALL);
		assertThat(secondRecentPosts).extracting("tempSave").containsOnly(false);
	}

	@DisplayName("postId로 Post와 User 페치조인 테스트")
	@Test
	void findPostsAndWriter() {
		//given
		String nickname = "mj";
		User user = em.createQuery("select u from Users u where u.nickname = :nickname", User.class)
			.setParameter("nickname", nickname)
			.getSingleResult();
		List<Post> posts = em.createQuery("select p from Post p join fetch p.user u where u = :user and p.tempSave = false",
				Post.class)
			.setParameter("user", user)
			.getResultList();

		//when
		List<Post> findPosts = posts.stream()
			.map(post -> postRepository.findPostAndWriterById(post.getId()).orElseThrow())
			.collect(Collectors.toList());

		//then
		assertThat(findPosts).extracting("tempSave").containsOnly(false);
		assertThat(findPosts).extracting("user").containsOnly(user);
	}

	@Disabled
	@DisplayName("커서 기반 페이징으로 Post 조회")
	@Test
	void cursorPagingPosts() {
		//given
		int size = 20;
		int secondSize = 35;
		List<Post> findPosts = em.createQuery(
				"select p from Post p where p.tempSave = false order by p.createdDate desc", Post.class)
			.getResultList();

		PageRequest pageRequest = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdDate"));
		PageRequest secondPageRequest = PageRequest.of(0, secondSize, Sort.by(Sort.Direction.DESC, "createdDate"));
		Long firstPostId = findPosts.get(0).getId();
		Long nextPostId = findPosts.get(19).getId();
		System.out.println("firstPostId = " + firstPostId);
		System.out.println("nextPostId = " + nextPostId);
		em.clear();

		//when
		Slice<Post> slicedPosts = postRepository.findPostsByIdBeforeAndBoundaryNotAndTempSaveIsFalse(
			firstPostId, Boundary.NONE, pageRequest);
		Slice<Post> nextSlicedPosts = postRepository.findPostsByIdBeforeAndBoundaryNotAndTempSaveIsFalse(
			nextPostId, Boundary.NONE, secondPageRequest);

		//then
		assertThat(slicedPosts).hasSize(size);
		assertThat(slicedPosts.hasNext()).isTrue();
		assertThat(slicedPosts.getNumber()).isEqualTo(0);
		assertThat(slicedPosts.getNumberOfElements()).isEqualTo(size);

		assertThat(nextSlicedPosts).hasSize(10);
		assertThat(nextSlicedPosts.hasNext()).isFalse();
		assertThat(nextSlicedPosts.getNumber()).isEqualTo(0);
		assertThat(nextSlicedPosts.getNumberOfElements()).isEqualTo(10);

	}

	@Disabled
	@DisplayName("최신순으로 페이징한 Post 조회 테스트")
	@Test
	void pagingPosts() {
		//given
		int pageSizeBelowTotalPostCnt = TOTAL_POST_CNT - 1;
		int pageSizeAboveTotalPostCnt = TOTAL_POST_CNT + 1;
		int postCnt = TOTAL_POST_CNT;

		PageRequest pageRequestBelowPostCnt = PageRequest.of(0, pageSizeBelowTotalPostCnt,
			Sort.by(Sort.Direction.DESC, "createdDate"));
		PageRequest pageRequestAbovePostCnt = PageRequest.of(0, pageSizeAboveTotalPostCnt,
			Sort.by(Sort.Direction.DESC, "createdDate"));

		//when
		Slice<Post> slicedPostsBelowPostCnt = postRepository.findPostsByBoundaryNotAndTempSaveFalse(Boundary.NONE,
			pageRequestBelowPostCnt);
		Slice<Post> slicedPostsAbovePostCnt = postRepository.findPostsByBoundaryNotAndTempSaveFalse(Boundary.NONE,
			pageRequestAbovePostCnt);

		//then
		assertThat(slicedPostsBelowPostCnt).hasSize(pageSizeBelowTotalPostCnt);
		assertThat(slicedPostsBelowPostCnt.hasNext()).isTrue();
		assertThat(slicedPostsBelowPostCnt.getNumber()).isEqualTo(0);
		assertThat(slicedPostsBelowPostCnt.getNumberOfElements()).isEqualTo(pageSizeBelowTotalPostCnt);

		assertThat(slicedPostsAbovePostCnt).hasSize(postCnt);
		assertThat(slicedPostsAbovePostCnt.hasNext()).isFalse();
		assertThat(slicedPostsAbovePostCnt.getNumber()).isEqualTo(0);
		assertThat(slicedPostsAbovePostCnt.getNumberOfElements()).isEqualTo(postCnt);
	}

	private void createTestTempSavePostAndPhoto(User user) {
		Post post = new Post(user, "임시저장 게시물 - " + user.getNickname(), 0L, Boundary.ALL, true);
		Photo photo1 = new Photo(post, "임시저장 url 1", "임시저장 photo 1-");
		Photo photo2 = new Photo(post, "임시저장 url 2", "임시저장 photo 2-");
		post.addPhoto(photo1);
		post.addPhoto(photo2);
		posts.add(post);
		photos.add(photo1);
		photos.add(photo2);
	}

	private void createTestPostWithPhoto(int postCnt, User user, Boundary boundary) {
		for (int i = 0; i < postCnt; i++) {
			Post post = new Post(user, "" + i, 0L, boundary, false);
			Photo photo1 = new Photo(post, "url 1-" + user.getNickname() + i, "photo 1-" + user.getNickname() + i);
			Photo photo2 = new Photo(post, "url 2-" + user.getNickname() + i, "photo 2-" + user.getNickname() + i);
			post.addPhoto(photo1);
			post.addPhoto(photo2);
			posts.add(post);
			photos.add(photo1);
			photos.add(photo2);
		}
	}

	private User createTestMember(String email, String nickname, String phoneNumber, Long socialLoginId) {
		LoginRequestDto loginRequestDto = new LoginRequestDto(nickname, phoneNumber, true);
		User user = new User(loginRequestDto, UserRole.USER);
		SocialLogin socialLogin = new SocialLogin(socialLoginId, "kakao", email, user);
		em.persist(user);
		em.persist(socialLogin);
		return user;
	}

	private void createFollow(User follower, User following) {
		Follow follow = new Follow(follower, following);
		em.persist(follow);
	}

	private List<Follow> findFollowByFollowerNickname(String nickname) {
		return em.createQuery("select f from Follow f where f.follower.nickname = :follower",
				Follow.class)
			.setParameter("follower", nickname)
			.getResultList();
	}

	private Post findOldestPost() {
		return em.createQuery(
				"select p from Post p where p.tempSave = false and p.boundary <> 'NONE' order by p.id desc",
				Post.class)
			.setMaxResults(1)
			.getSingleResult();
	}
}