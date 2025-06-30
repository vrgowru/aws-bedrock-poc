CREATE TABLE benefitsassist_inference_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
  conversation_id VARCHAR(255),
  session_id VARCHAR(255),
  client_name VARCHAR(255) NOT NULL,
  client_session_id VARCHAR(255) NOT NULL,
  tracking_id VARCHAR(255) NOT NULL,
  submit_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
  user_id VARCHAR(255),
  user_role VARCHAR(50),
  request_date DATE NOT NULL,
  query TEXT NOT NULL,
  coverage_package_codes JSON NOT NULL,
  group_anniversary_date DATE,
  state_code CHAR(2) CHECK (state_code ~ '^[A-Z]{2}')
);
