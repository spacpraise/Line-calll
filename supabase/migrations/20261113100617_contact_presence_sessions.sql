-- Contact discovery uses only authenticated, batched lookups. Presence rows expire automatically.
create table if not exists public.user_presence_sessions (
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id text not null check (length(device_id) between 8 and 128),
  state text not null default 'online' check (state in ('online','in_call')),
  expires_at timestamptz not null,
  updated_at timestamptz not null default now(),
  primary key (user_id, device_id)
);
create index if not exists user_presence_sessions_expiry_idx
  on public.user_presence_sessions(expires_at);
alter table public.user_presence_sessions enable row level security;
revoke all on public.user_presence_sessions from anon, authenticated;
