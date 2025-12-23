DROP DATABASE IF EXISTS snapway;
CREATE DATABASE IF NOT EXISTS snapway;
USE snapway;

-- 외래 키 체크 해제 (테이블 삭제 순서 무관하게 하기 위함)
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 기존 테이블 삭제
DROP TABLE IF EXISTS trip_hashtag;
DROP TABLE IF EXISTS reply;
DROP TABLE IF EXISTS trip_photo;
DROP TABLE IF EXISTS trip_record;
DROP TABLE IF EXISTS trip;
DROP TABLE IF EXISTS article;
DROP TABLE IF EXISTS member;
DROP TABLE IF EXISTS travel_style;

SET FOREIGN_KEY_CHECKS = 1;


-- ==============================================
-- 2. 테이블 생성
-- ==============================================

-- [1] TRAVEL_STYLE 테이블 (신규 추가)
-- 여행 스타일의 기준 정보를 관리합니다.
CREATE TABLE travel_style
(
  style_code  VARCHAR(20) NOT NULL COMMENT '스타일 코드 (Enum.name)',
  description VARCHAR(50) NOT NULL COMMENT '스타일 설명',
  PRIMARY KEY (style_code)
) COMMENT '여행 스타일 기준 정보 테이블';

-- [2] MEMBER 테이블 (수정됨)
-- 기존 ENUM style 컬럼 제거 -> style_code 외래키 컬럼 추가
CREATE TABLE member
(
  id          INTEGER      NOT NULL AUTO_INCREMENT,
  email       VARCHAR(100) NOT NULL COMMENT '이메일 (Unique)',
  password    VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
  username    VARCHAR(50)  NOT NULL COMMENT '닉네임',
  role        ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  profile_img VARCHAR(2048) NULL    COMMENT '프로필 이미지 경로',
  gender      ENUM('MALE', 'FEMALE') NULL,
  birthday    DATE         NULL,
  style_code  VARCHAR(20)  NULL COMMENT '선호 여행 스타일 (FK)',
  PRIMARY KEY (id),
  CONSTRAINT UQ_email UNIQUE (email)
) COMMENT '회원 정보 관리 테이블';

-- [3] ARTICLE 테이블
CREATE TABLE article
(
  article_id  INTEGER      NOT NULL AUTO_INCREMENT,
  title       VARCHAR(255) NOT NULL,
  tags        VARCHAR(255) NULL COMMENT '태그 (콤마 구분 문자열)',
  author_id   INTEGER      NOT NULL,
  content     TEXT         NOT NULL,
  likes       INTEGER      DEFAULT 0 COMMENT '좋아요 수',
  uploaded_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (article_id)
) COMMENT '게시판 관리 테이블';

-- [4] REPLY 테이블
CREATE TABLE reply
(
  reply_id   INTEGER  NOT NULL AUTO_INCREMENT,
  article_id INTEGER  NOT NULL,
  replier_id INTEGER  NOT NULL,
  content    TEXT     NOT NULL,
  replied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (reply_id)
) COMMENT '댓글 관리 테이블';

-- [5] TRIP 테이블 (수정됨)
-- tags 컬럼 제거
CREATE TABLE trip
(
  trip_id     INTEGER      NOT NULL AUTO_INCREMENT,
  title       VARCHAR(255) NOT NULL,
  id          INTEGER      NOT NULL COMMENT '작성자(Member) ID',
  uploaded_at DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
  start_date  DATE         NULL,
  end_date    DATE         NULL,
  visibility  ENUM('PUBLIC', 'PRIVATE') DEFAULT 'PUBLIC',
  PRIMARY KEY (trip_id)
) COMMENT '여행(폴더) 테이블';

-- [6] TRIP_HASHTAG 테이블 (신규 추가)
-- Trip과 TravelStyle의 N:M 관계를 해소합니다.
CREATE TABLE trip_hashtag 
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    trip_id     INTEGER     NOT NULL,
    style_code  VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
) COMMENT '여행 기록 태그 매핑 테이블';

-- [7] TRIP_RECORD 테이블
CREATE TABLE trip_record
(
  record_id    INTEGER      NOT NULL AUTO_INCREMENT,
  trip_id      INTEGER      NOT NULL,
  latitude     DOUBLE       NULL COMMENT '위도',
  longitude    DOUBLE       NULL COMMENT '경도',
  place_name   VARCHAR(255) NULL,
  ai_content   TEXT         NULL COMMENT 'AI 자동 생성 일기',
  visited_date DATETIME     NULL,
  PRIMARY KEY (record_id)
) COMMENT '여행 기록(마커) 테이블';

-- [8] TRIP_PHOTO 테이블
CREATE TABLE trip_photo
(
  photo_code INTEGER       NOT NULL AUTO_INCREMENT,
  record_id  INTEGER       NOT NULL,
  file_path  VARCHAR(2048) NULL COMMENT '이미지 저장 경로',
  photo_name VARCHAR(255)  NULL,
  caption    TEXT          NULL,
  PRIMARY KEY (photo_code)
) COMMENT '여행 기록 사진 리스트 테이블';


-- ==============================================
-- 3. 외래 키 (Foreign Key) 설정
-- ==============================================

-- Member -> TravelStyle (신규)
ALTER TABLE member
  ADD CONSTRAINT FK_travel_style_TO_member
    FOREIGN KEY (style_code) REFERENCES travel_style (style_code)
    ON DELETE SET NULL;

-- Article -> Member
ALTER TABLE article
  ADD CONSTRAINT FK_member_TO_article
    FOREIGN KEY (author_id) REFERENCES member (id)
    ON DELETE CASCADE;

-- Reply -> Article
ALTER TABLE reply
  ADD CONSTRAINT FK_article_TO_reply
    FOREIGN KEY (article_id) REFERENCES article (article_id)
    ON DELETE CASCADE;

-- Reply -> Member
ALTER TABLE reply
  ADD CONSTRAINT FK_member_TO_reply
    FOREIGN KEY (replier_id) REFERENCES member (id)
    ON DELETE CASCADE;

-- Trip -> Member
ALTER TABLE trip
  ADD CONSTRAINT FK_member_TO_trip
    FOREIGN KEY (id) REFERENCES member (id)
    ON DELETE CASCADE;

-- TripHashtag -> Trip (신규)
ALTER TABLE trip_hashtag
  ADD CONSTRAINT FK_trip_TO_trip_hashtag
    FOREIGN KEY (trip_id) REFERENCES trip (trip_id)
    ON DELETE CASCADE;

-- TripHashtag -> TravelStyle (신규)
ALTER TABLE trip_hashtag
  ADD CONSTRAINT FK_travel_style_TO_trip_hashtag
    FOREIGN KEY (style_code) REFERENCES travel_style (style_code)
    ON DELETE CASCADE;

-- TripRecord -> Trip
ALTER TABLE trip_record
  ADD CONSTRAINT FK_trip_TO_trip_record
    FOREIGN KEY (trip_id) REFERENCES trip (trip_id)
    ON DELETE CASCADE;

-- TripPhoto -> TripRecord
ALTER TABLE trip_photo
  ADD CONSTRAINT FK_trip_record_TO_trip_photo
    FOREIGN KEY (record_id) REFERENCES trip_record (record_id)
    ON DELETE CASCADE;


-- ==============================================
-- 4. 기초 데이터 삽입 (TravelStyle)
-- ==============================================

INSERT INTO travel_style (style_code, description) VALUES
('NATURE', '자연 힐링'),
('CITY', '도시 탐험'),
('FOOD', '맛집 탐방'),
('ACTIVITY', '액티비티'),
('CULTURE', '문화 예술'),
('PHOTO', '인생샷'),
('HEALING', '휴양 휴식'),
('HISTORY', '역사 탐방'),
('SHOPPING', '쇼핑 투어'),
('LOCAL', '현지 체험'),
('FESTIVAL', '축제/공연'),
('DRIVE', '드라이브'),
('DATE', '커플 여행'),
('FAMILY', '가족 여행'),
('PET', '반려동물 동반');