-- Deleting a user cascades everything they own/touched. Every other FK in the
-- schema (comment parent_id/comment_id, comment likes, comment reports,
-- list_id/review_id on lists/reviews/notifications) already cascades from
-- earlier migrations, so this only needs to fix the direct references to
-- users(id).

ALTER TABLE library_entries DROP CONSTRAINT fk_library_entries_user;
ALTER TABLE library_entries ADD CONSTRAINT fk_library_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE reviews DROP CONSTRAINT fk_reviews_user;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE game_lists DROP CONSTRAINT fk_game_lists_user;
ALTER TABLE game_lists ADD CONSTRAINT fk_game_lists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE follows DROP CONSTRAINT fk_follows_follower;
ALTER TABLE follows ADD CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE follows DROP CONSTRAINT fk_follows_followee;
ALTER TABLE follows ADD CONSTRAINT fk_follows_followee FOREIGN KEY (followee_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE list_likes DROP CONSTRAINT fk_list_likes_user;
ALTER TABLE list_likes ADD CONSTRAINT fk_list_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE review_likes DROP CONSTRAINT fk_review_likes_user;
ALTER TABLE review_likes ADD CONSTRAINT fk_review_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE list_comments DROP CONSTRAINT fk_list_comments_user;
ALTER TABLE list_comments ADD CONSTRAINT fk_list_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE list_comment_reports DROP CONSTRAINT fk_list_comment_reports_reporter;
ALTER TABLE list_comment_reports ADD CONSTRAINT fk_list_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE review_comments DROP CONSTRAINT fk_review_comments_user;
ALTER TABLE review_comments ADD CONSTRAINT fk_review_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE review_comment_reports DROP CONSTRAINT fk_review_comment_reports_reporter;
ALTER TABLE review_comment_reports ADD CONSTRAINT fk_review_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE list_comment_likes DROP CONSTRAINT fk_list_comment_likes_user;
ALTER TABLE list_comment_likes ADD CONSTRAINT fk_list_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE review_comment_likes DROP CONSTRAINT fk_review_comment_likes_user;
ALTER TABLE review_comment_likes ADD CONSTRAINT fk_review_comment_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE notifications DROP CONSTRAINT fk_notifications_recipient;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE notifications DROP CONSTRAINT fk_notifications_actor;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE CASCADE;
