-- Line Call core schema: profiles and one-to-one voice calls.
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  phone_number text,
  phone_verified boolean not null default false,
  display_name text,
  avatar_url text,
  fcm_token text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint profiles_phone_e164_check check (
    phone_number is null or (
      left(phone_number, 1) = '+'
      and substring(phone_number from 2) ~ '^[1-9][0-9]{6,14}$'
    )
  )
);
create unique index if not exists profiles_phone_number_unique
  on public.profiles(phone_number) where phone_number is not null;

create table if not exists public.calls (
  id uuid primary key default gen_random_uuid(),
  caller_id uuid not null references auth.users(id) on delete cascade,
  recipient_id uuid not null references auth.users(id) on delete cascade,
  caller_phone text not null,
  recipient_phone text not null,
  room_name text not null unique,
  status text not null default 'ringing'
    check (status in ('ringing','accepted','declined','missed','ended','failed')),
  created_at timestamptz not null default now(),
  accepted_at timestamptz,
  ended_at timestamptz,
  duration_seconds integer not null default 0 check (duration_seconds >= 0)
);
create index if not exists calls_recipient_status_created_idx
  on public.calls(recipient_id, status, created_at desc);
create index if not exists calls_caller_created_idx
  on public.calls(caller_id, created_at desc);

alter table public.profiles enable row level security;
alter table public.calls enable row level security;
grant select on public.profiles to authenticated;
grant insert (id, email, phone_number, display_name, avatar_url) on public.profiles to authenticated;
grant update (phone_number, display_name, avatar_url, updated_at, fcm_token) on public.profiles to authenticated;
grant select on public.calls to authenticated;

drop policy if exists "Users can read their own profile" on public.profiles;
create policy "Users can read their own profile"
  on public.profiles for select to authenticated
  using ((select auth.uid()) = id);
drop policy if exists "Users can create their own profile" on public.profiles;
create policy "Users can create their own profile"
  on public.profiles for insert to authenticated
  with check ((select auth.uid()) = id);
drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
  on public.profiles for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);
drop policy if exists "Participants can read their calls" on public.calls;
create policy "Participants can read their calls"
  on public.calls for select to authenticated
  using ((select auth.uid()) = caller_id or (select auth.uid()) = recipient_id);

  )
);
create unique index if not exists profiles_phone_number_unique
  on public.profiles(phone_number) where phone_number is not null;

create table if not exists public.calls (
  id uuid primary key default gen_random_uuid(),
  caller_id uuid not null references auth.users(id) on delete cascade,
  recipient_id uuid not null references auth.users(id) on delete cascade,
  caller_phone text not null,
  recipient_phone text not null,
  room_name text not null unique,
  status text not null default 'ringing'
    check (status in ('ringing','accepted','declined','missed','ended','failed')),
  created_at timestamptz not null default now(),
  accepted_at timestamptz,
  ended_at timestamptz,
  duration_seconds integer not null default 0 check (duration_seconds >= 0)
);
create index if not exists calls_recipient_status_created_idx
  on public.calls(recipient_id, status, created_at desc);
create index if not exists calls_caller_created_idx
  on public.calls(caller_id, created_at desc);

alter table public.profiles enable row level security;
alter table public.calls enable row level security;
grant select, insert, update on public.profiles to authenticated;
grant select on public.calls to authenticated;

drop policy if exists "Users can read their own profile" on public.profiles;
create policy "Users can read their own profile"
  on public.profiles for select to authenticated
  using ((select auth.uid()) = id);
drop policy if exists "Users can create their own profile" on public.profiles;
create policy "Users can create their own profile"
  on public.profiles for insert to authenticated
  with check ((select auth.uid()) = id);
drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
  on public.profiles for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);
drop policy if exists "Participants can read their calls" on public.calls;
create policy "Participants can read their calls"
  on public.calls for select to authenticated
  using ((select auth.uid()) = caller_id or (select auth.uid()) = recipient_id);
