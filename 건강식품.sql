-- 영양제 섭취 관리 — 최소 스키마 (MySQL 버전)
-- Engine: InnoDB, Charset: UTF8MB4

-- 1) 유저
CREATE TABLE users (
  user_code INT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(255) UNIQUE NOT NULL,
  user_name VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(50),
  birthday DATE,
  gender VARCHAR(16),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) 브랜드(회사)
CREATE TABLE brands (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE,
  country VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) 제품
CREATE TABLE products (
  itemSeq INT PRIMARY KEY,
  product_name VARCHAR(255) NOT NULL,
  brand_id INT,
  manufacturer VARCHAR(255),
  dosage_form VARCHAR(100),
  serving_size_amount DECIMAL(12,4),
  serving_size_unit VARCHAR(32),
  directions TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) 성분(영양소)
CREATE TABLE nutrients (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE,
  alias VARCHAR(255),
  canonical_unit VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- (보조) 제품-성분 연결: 제품 라벨 기준 "1회 섭취량" 당 표기
CREATE TABLE product_ingredients (
  id INT PRIMARY KEY AUTO_INCREMENT,
  itemSeq INT NOT NULL,
  nutrient_id INT NOT NULL,
  amount_per_serving DECIMAL(18,6) NOT NULL,
  unit VARCHAR(32) NOT NULL,
  note VARCHAR(255),
  CONSTRAINT fk_prod_ing_product FOREIGN KEY (itemSeq) REFERENCES products(itemSeq),
  CONSTRAINT fk_prod_ing_nutrient FOREIGN KEY (nutrient_id) REFERENCES nutrients(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) 유저가 선택한 제품 (장바구니/복용 리스트)
CREATE TABLE user_selected_products (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_code INT NOT NULL,
  itemSeq INT NOT NULL,
  servings_per_day DECIMAL(12,4) NOT NULL DEFAULT 1,
  start_date DATE,
  end_date DATE,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  note VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_sel_user FOREIGN KEY (user_code) REFERENCES users(user_code),
  CONSTRAINT fk_user_sel_product FOREIGN KEY (itemSeq) REFERENCES products(itemSeq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6) 각 유저별 성분 적정량 (개인화 RNI/AI, UL, 커스텀 기준)
CREATE TABLE user_nutrient_targets (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_code INT NOT NULL,
  nutrient_id INT NOT NULL,
  basis VARCHAR(16) NOT NULL,
  target_value DECIMAL(18,6) NOT NULL,
  ul_value DECIMAL(18,6),
  unit VARCHAR(32) NOT NULL,
  note VARCHAR(255),
  effective_from DATE,
  effective_to DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_target_user FOREIGN KEY (user_code) REFERENCES users(user_code),
  CONSTRAINT fk_target_nutrient FOREIGN KEY (nutrient_id) REFERENCES nutrients(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7) 게시판
CREATE TABLE board_posts (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_code INT NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  category VARCHAR(64),
  status VARCHAR(16) DEFAULT 'visible',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_board_user FOREIGN KEY (user_code) REFERENCES users(user_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
