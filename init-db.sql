-- Create databases for each microservice
CREATE DATABASE planning_poker_rooms;
CREATE DATABASE planning_poker_votes;
CREATE DATABASE planning_poker_audit;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE planning_poker_rooms TO planning_poker;
GRANT ALL PRIVILEGES ON DATABASE planning_poker_votes TO planning_poker;
GRANT ALL PRIVILEGES ON DATABASE planning_poker_audit TO planning_poker;

