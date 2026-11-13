-- Authoritative call state, atomic transitions, and one active call per participant.
-- Existing duplicate/stale active rows are terminalized before enforcing the invariant.
alter table public.calls
  add column if not exists expires_at timestamptz,
  add column if not exists state_version bigint not null default 1,
  add column if not exists accepted_device_id text,
  add column if not exists last_activity_at timestamptz,
  add column if not exists heartbeat_enabled boolean not null default false;

update public.calls
set expires_at = coalesce(expires_at, created_at + interval '45 seconds'),
    last_activity_at = coalesce(last_activity_at, accepted_at, created_at);

-- Expire abandoned ringing calls and resolve any legacy duplicate active calls.
update public.calls
set status = 'missed', ended_at = coalesce(ended_at, now()), state_version = state_version + 1
where status = 'ringing' and coalesce(expires_at, created_at + interval '45 seconds') <= now();

with ranked as (
  select id, row_number() over (
    partition by participant_id order by created_at desc, id desc
  ) as rn
  from (
    select id, caller_id as participant_id, created_at from public.calls where status in ('ringing','accepted')
    union all
    select id, recipient_id as participant_id, created_at from public.calls where status in ('ringing','accepted')
  ) active
)
update public.calls c
set status = 'failed', ended_at = coalesce(c.ended_at, now()), state_version = c.state_version + 1
where c.id in (select id from ranked where rn > 1)
  and c.status in ('ringing','accepted');

create unique index if not exists calls_one_active_per_caller
  on public.calls(caller_id) where status in ('ringing','accepted');
create unique index if not exists calls_one_active_per_recipient
  on public.calls(recipient_id) where status in ('ringing','accepted');
create index if not exists calls_expiry_idx on public.calls(expires_at)
  where status = 'ringing';

-- Only this SECURITY DEFINER function may perform lifecycle transitions.
create or replace function public.transition_line_call(
  p_call_id uuid,
  p_action text,
  p_device_id text default null
) returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_user uuid := auth.uid();
  v_call public.calls%rowtype;
  v_now timestamptz := clock_timestamp();
  v_duration integer := 0;
begin
  if v_user is null then raise exception 'Authentication required' using errcode = '28000'; end if;
  if p_action not in ('accept','decline','end','heartbeat','expire') then
    raise exception 'Unsupported call transition' using errcode = '22023';
  end if;
  if p_action = 'expire' and current_setting('role', true) not in ('service_role','postgres') then
    raise exception 'Not permitted' using errcode = '42501';
  end if;

  select * into v_call from public.calls where id = p_call_id for update;
  if not found or (v_call.caller_id <> v_user and v_call.recipient_id <> v_user) then
    raise exception 'Call not found' using errcode = 'P0002';
  end if;

  if v_call.status = 'ringing' and coalesce(v_call.expires_at, v_call.created_at + interval '45 seconds') <= v_now then
    update public.calls set status='missed', ended_at=v_now, state_version=state_version+1
      where id=p_call_id and status='ringing';
    return jsonb_build_object('status','missed','accepted',false,'transitioned',true);
  end if;

  if p_action = 'accept' then
    if v_call.recipient_id <> v_user then raise exception 'Only the recipient can answer' using errcode='42501'; end if;
    if nullif(p_device_id,'') is null then
      raise exception 'Device ID is required' using errcode='22023';
    end if;
    if v_call.status = 'accepted' then
      if v_call.accepted_device_id = p_device_id then
        return jsonb_build_object('status','accepted','accepted',true,'transitioned',false,'idempotent',true);
      end if;
      return jsonb_build_object('status','accepted','accepted',false,'transitioned',false,'reason','answered_on_another_device');
    end if;
    if v_call.status <> 'ringing' then
      return jsonb_build_object('status',v_call.status,'accepted',false,'transitioned',false);
    end if;
    if exists (
      select 1 from public.calls
      where id <> p_call_id and status='accepted'
        and (caller_id=v_user or recipient_id=v_user)
    ) then
      return jsonb_build_object('status','busy','accepted',false,'transitioned',false);
    end if;
    update public.calls set status='accepted', accepted_at=v_now, last_activity_at=v_now,
      heartbeat_enabled=true, accepted_device_id=nullif(p_device_id,''), state_version=state_version+1
      where id=p_call_id and status='ringing';
    return jsonb_build_object('status','accepted','accepted',true,'transitioned',true);
  elsif p_action = 'decline' then
    if v_call.recipient_id <> v_user then raise exception 'Only the recipient can decline' using errcode='42501'; end if;
    if v_call.status = 'declined' then return jsonb_build_object('status','declined','declined',true,'transitioned',false); end if;
    if v_call.status <> 'ringing' then return jsonb_build_object('status',v_call.status,'declined',false,'transitioned',false); end if;
    update public.calls set status='declined', ended_at=v_now, state_version=state_version+1
      where id=p_call_id and status='ringing';
    return jsonb_build_object('status','declined','declined',true,'transitioned',true);
  elsif p_action = 'heartbeat' then
    if v_call.status <> 'accepted' then return jsonb_build_object('status',v_call.status,'heartbeat',false); end if;
    update public.calls set last_activity_at=v_now, heartbeat_enabled=true where id=p_call_id and status='accepted';
    return jsonb_build_object('status','accepted','heartbeat',true);
  else
    if v_call.status not in ('ringing','accepted') then
      return jsonb_build_object('status',v_call.status,'ended',false,'transitioned',false);
    end if;
    if v_call.accepted_at is not null then
      v_duration := greatest(0, floor(extract(epoch from (v_now-v_call.accepted_at)))::integer);
    end if;
    update public.calls set status='ended', ended_at=v_now, duration_seconds=v_duration,
      state_version=state_version+1 where id=p_call_id and status in ('ringing','accepted');
    return jsonb_build_object('status','ended','ended',true,'duration_seconds',v_duration,'transitioned',true);
  end if;
end;
$$;

revoke all on function public.transition_line_call(uuid,text,text) from public, anon;
grant execute on function public.transition_line_call(uuid,text,text) to authenticated, service_role;

-- Prevent direct client-side mutation; lifecycle changes must go through call-control.
revoke insert, update, delete on public.calls from anon, authenticated;

-- Serialize call creation across caller/recipient role combinations.
create or replace function public.enforce_one_active_line_call()
returns trigger language plpgsql security definer set search_path=public,pg_temp as $$
declare
  first_id uuid;
  second_id uuid;
begin
  if new.status not in ('ringing','accepted') then return new; end if;
  first_id := least(new.caller_id, new.recipient_id);
  second_id := greatest(new.caller_id, new.recipient_id);
  perform pg_advisory_xact_lock(hashtextextended(first_id::text, 0));
  perform pg_advisory_xact_lock(hashtextextended(second_id::text, 0));
  if exists (
    select 1 from public.calls c
    where c.id is distinct from new.id
      and c.status in ('ringing','accepted')
      and (c.caller_id in (new.caller_id,new.recipient_id)
        or c.recipient_id in (new.caller_id,new.recipient_id)
        or c.caller_id in (new.caller_id,new.recipient_id)
        and c.recipient_id in (new.caller_id,new.recipient_id))
  ) then
    raise exception 'A participant already has an active call' using errcode='23505';
  end if;
  return new;
end $$;
drop trigger if exists calls_one_active_participant_guard on public.calls;
create trigger calls_one_active_participant_guard
before insert or update of caller_id, recipient_id, status on public.calls
for each row execute function public.enforce_one_active_line_call();

-- Periodic server-side sweeper guarantees abandoned calls become terminal even
-- when neither participant opens the app again. Supabase projects with pg_cron
-- enabled get a one-minute schedule; API requests remain a secondary fallback.
create or replace function public.sweep_stale_line_calls()
returns integer language plpgsql security definer set search_path=public,pg_temp as $$
declare affected integer := 0;
begin
  update public.calls
  set status='missed', ended_at=coalesce(ended_at,now()), state_version=state_version+1
  where status='ringing' and coalesce(expires_at,created_at+interval '45 seconds') <= now();
  get diagnostics affected = row_count;
  update public.calls
  set status='ended', ended_at=coalesce(ended_at,now()), state_version=state_version+1
  where status='accepted' and heartbeat_enabled=true
    and coalesce(last_activity_at,accepted_at,created_at) < now()-interval '3 minutes';
  get diagnostics affected = affected + row_count;
  update public.calls
  set status='ended', ended_at=coalesce(ended_at,now()), state_version=state_version+1
  where status='accepted' and heartbeat_enabled=false
    and coalesce(accepted_at,created_at) < now()-interval '12 hours';
  get diagnostics affected = affected + row_count;
  return affected;
end $$;
revoke all on function public.sweep_stale_line_calls() from public, anon, authenticated;
grant execute on function public.sweep_stale_line_calls() to service_role;

do $schedule$
begin
  if exists (select 1 from pg_extension where extname='pg_cron') then
    perform cron.schedule('line-call-stale-call-sweeper', '* * * * *',
      'select public.sweep_stale_line_calls();');
  end if;
exception when others then
  raise notice 'pg_cron scheduling unavailable; stale call sweeper must be scheduled from Supabase Cron.';
end
$schedule$;
