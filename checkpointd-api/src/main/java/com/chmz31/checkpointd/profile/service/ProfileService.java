package com.chmz31.checkpointd.profile.service;

import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.profile.dto.PublicProfileGameResponse;
import com.chmz31.checkpointd.profile.dto.PublicProfileResponse;
import com.chmz31.checkpointd.profile.dto.PublicProfileStatsResponse;
import com.chmz31.checkpointd.profile.dto.UpdateProfileRequest;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

	private final UserRepository userRepository;
	private final LibraryEntryRepository libraryEntryRepository;

	public ProfileService(UserRepository userRepository, LibraryEntryRepository libraryEntryRepository) {
		this.userRepository = userRepository;
		this.libraryEntryRepository = libraryEntryRepository;
	}

	@Transactional(readOnly = true)
	public PublicProfileResponse getPublicProfile(String username) {
		User user = findByUsername(username);
		if (user.getProfileVisibility() != ProfileVisibility.PUBLIC) {
			throw new ResourceNotFoundException("Profile not found");
		}

		return buildProfile(user);
	}

	@Transactional(readOnly = true)
	public PublicProfileResponse getMyProfile(UUID userId) {
		return buildProfile(findById(userId));
	}

	@Transactional
	public PublicProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
		User user = findById(userId);
		user.setDisplayName(blankToNull(request.displayName()));
		user.setBio(blankToNull(request.bio()));
		if (request.profileVisibility() != null) {
			user.setProfileVisibility(request.profileVisibility());
		}

		return buildProfile(userRepository.save(user));
	}

	private PublicProfileResponse buildProfile(User user) {
		UUID userId = user.getId();
		var stats = new PublicProfileStatsResponse(
				libraryEntryRepository.countByUserId(userId),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED),
				libraryEntryRepository.countByUserIdAndRatingIsNotNull(userId),
				libraryEntryRepository.averageRatingByUserId(userId));
		var recentGames = libraryEntryRepository.findTop8ByUserIdOrderByUpdatedAtDesc(userId).stream()
				.map(PublicProfileGameResponse::from)
				.toList();

		return new PublicProfileResponse(
				user.getUsername(),
				user.getDisplayName(),
				user.getBio(),
				user.getProfileVisibility(),
				user.getCreatedAt(),
				stats,
				recentGames);
	}

	private User findByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
	}

	private User findById(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
