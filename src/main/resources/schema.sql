create table if not exists Application(id UUID primary key, application_id UUID, name varchar, client_id varchar, active boolean, created timestamp, access_date_time timestamp);

create table if not exists Application_User(id uuid primary key, application_id uuid, user_id uuid, role_name varchar);

