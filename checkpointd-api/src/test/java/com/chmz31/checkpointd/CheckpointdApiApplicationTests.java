package com.chmz31.checkpointd;

import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CheckpointdApiApplicationTests {

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private ReviewRepository reviewRepository;

	@Test
	void contextLoads() {
	}

}
