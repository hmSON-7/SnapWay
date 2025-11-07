CREATE TABLE `users` (
  `user_code` integer PRIMARY KEY,
  `user_id` varchar(255) UNIQUE NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(255),
  `created_at` timestamp DEFAULT (now()),
  `phone` integer UNIQUE,
  `birthday` datetime NOT NULL,
  `gender` integer NOT NULL,
  `medical_history` varchar(255)
);

CREATE TABLE `drug` (
  `itemSeq` integer PRIMARY KEY,
  `itemName` varchar(255),
  `entpName` varchar(255),
  `efcyQesitm` varchar(255),
  `useMethodQesitm` varchar(255),
  `atpnWarnQesitm` varchar(255),
  `atpnQesitm` varchar(255),
  `intrcQesitm` varchar(255),
  `seQesitm` varchar(255),
  `depositMethodQesitm` varchar(255)
);

CREATE TABLE `MedicationLog` (
  `user_code` integer PRIMARY KEY,
  `itemSeq` integer UNIQUE,
  `frequency` varchar(255),
  `start_date` datetime,
  `end_date` datetime,
  `instruction` varchar(255),
  `status` char
);

CREATE TABLE `DrugIngredient` (
  `itemSeq` integer,
  `ingredient_id` integer,
  `quantity` varchar(255),
  `unit` varchar(255)
);

ALTER TABLE `MedicationLog` ADD FOREIGN KEY (`user_code`) REFERENCES `users` (`user_code`);

ALTER TABLE `MedicationLog` ADD FOREIGN KEY (`itemSeq`) REFERENCES `drug` (`itemSeq`);

ALTER TABLE `DrugIngredient` ADD FOREIGN KEY (`itemSeq`) REFERENCES `drug` (`itemSeq`);
