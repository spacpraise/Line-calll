-- Durable, participant-scoped call signaling events for Supabase Realtime.
alter table public.calls
  add column if not exists ended_by uuid,
  add column if not exists end_reason text,
  add column if not exists connection_state text not null default 'pending';

create table if not exists public.call_events (
  id uuid primary key default gen_random_uuid(),
  call_id uuid references public.calls(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  event_type text not null check (event_type in (
    'incoming_call','ringing_confirmation','call_accepted','call_declined',
    'call_ended','recipient_busy','caller_cancelled',
    'connection_established','connection_lost','reconnection_completed'
  )),
  state_version bigint not null default 0,
  payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists call_events_user_created_idx
  on public.call_events(user_id, created_at desc);
alter table public.call_events enable row level security;
drop policy if exists "Users can read their own call events" on public.call_events;
create policy "Users can read their own call events"
  on public.call_events for select to authenticated
  using (user_id = (select auth.uid()));
revoke all on public.call_events from anon, authenticated;
grant select on public.call_events to authenticated;

-- Publish only the minimal event envelope. RLS limits each subscriber to their own rows.
do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname='supabase_realtime' and schemaname='public' and tablename='call_events'
  ) then
    alter publication supabase_realtime add table public.call_events;
  end if;
end $$;

create or replace function public.emit_line_call_realtime_events()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_type text;
  v_payload jsonb := '{}'::jsonb;
begin
  if TG_OP = 'INSERT' then
    if NEW.status = 'ringing' then
      insert into public.call_events(call_id,user_id,event_type,state_version,payload)
      values
        (NEW.id,NEW.recipient_id,'incoming_call',NEW.state_version,
          jsonb_build_object('caller_phone',NEW.caller_phone,'created_at',NEW.created_at)),
        (NEW.id,NEW.caller_id,'ringing_confirmation',NEW.state_version,
          jsonb_build_object('recipient_phone',NEW.recipient_phone,'created_at',NEW.created_at));
    end if;
    return NEW;
  end if;

  if NEW.status is distinct from OLD.status then
    v_type := case NEW.status
      when 'accepted' then 'call_accepted'
      when 'declined' then 'call_declined'
      when 'ended' then
        case when OLD.status='ringing' and NEW.ended_by=NEW.caller_id
          then 'caller_cancelled' else 'call_ended' end
      when 'missed' then 'call_ended'
      when 'failed' then 'call_ended'
      else null
    end;
    if v_type is not null then
      v_payload := jsonb_build_object(
        'status',NEW.status,'end_reason',NEW.end_reason,
        'ended_by',NEW.ended_by,'duration_seconds',NEW.duration_seconds
      );
      insert into public.call_events(call_id,user_id,event_type,state_version,payload)
      values
        (NEW.id,NEW.caller_id,v_type,NEW.state_version,v_payload),
        (NEW.id,NEW.recipient_id,v_type,NEW.state_version,v_payload);
    end if;
  end if;

  if NEW.connection_state is distinct from OLD.connection_state then
    v_type := case NEW.connection_state
      when 'connected' then 'connection_established'
      when 'lost' then 'connection_lost'
      when 'reconnected' then 'reconnection_completed'
      else null
    end;
    if v_type is not null then
      v_payload := jsonb_build_object('connection_state',NEW.connection_state);
      insert into public.call_events(call_id,user_id,event_type,state_version,payload)
      values
        (NEW.id,NEW.caller_id,v_type,NEW.state_version,v_payload),
        (NEW.id,NEW.recipient_id,v_type,NEW.state_version,v_payload);
    end if;
  end if;
  return NEW;
end;
$$;

drop trigger if exists calls_emit_line_realtime_events on public.calls;
create trigger calls_emit_line_realtime_events
after insert or update of status, connection_state on public.calls
for each row execute function public.emit_line_call_realtime_events();

-- Keep the transition function authoritative while adding caller identity and
-- the three media-connection signaling transitions.
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
  v_connection_state text;
begin
  if v_user is null then raise exception 'Authentication required' using errcode = '28000'; end if;
  if p_action not in ('accept','decline','end','heartbeat','expire',
    'connection_established','connection_lost','reconnection_completed') then
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
    update public.calls set status='missed', ended_at=v_now, end_reason='ring_timeout',
      state_version=state_version+1 where id=p_call_id and status='ringing';
    return jsonb_build_object('status','missed','accepted',false,'transitioned',true);
  end if;

  if p_action = 'accept' then
    if v_call.recipient_id <> v_user then raise exception 'Only the recipient can answer' using errcode='42501'; end if;
    if nullif(p_device_id,'') is null then raise exception 'Device ID is required' using errcode='22023'; end if;
    if v_call.status = 'accepted' then
      if v_call.accepted_device_id = p_device_id then
        return jsonb_build_object('status','accepted','accepted',true,'transitioned',false,'idempotent',true);
      end if;
      return jsonb_build_object('status','accepted','accepted',false,'transitioned',false,'reason','answered_on_another_device');
    end if;
    if v_call.status <> 'ringing' then
      return jsonb_build_object('status',v_call.status,'accepted',false,'transitioned',false);
    end if;
    if exists (select 1 from public.calls where id<>p_call_id and status='accepted'
      and (caller_id=v_user or recipient_id=v_user)) then
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
    update public.calls set status='declined', ended_at=v_now, ended_by=v_user,
      end_reason='recipient_declined', state_version=state_version+1
      where id=p_call_id and status='ringing';
    return jsonb_build_object('status','declined','declined',true,'transitioned',true);
  elsif p_action = 'heartbeat' then
    if v_call.status <> 'accepted' then return jsonb_build_object('status',v_call.status,'heartbeat',false); end if;
    update public.calls set last_activity_at=v_now, heartbeat_enabled=true where id=p_call_id and status='accepted';
    return jsonb_build_object('status','accepted','heartbeat',true);
  elsif p_action in ('connection_established','connection_lost','reconnection_completed') then
    if v_call.status <> 'accepted' then
      return jsonb_build_object('status',v_call.status,'signaled',false,'transitioned',false);
    end if;
    v_connection_state := case p_action
      when 'connection_established' then 'connected'
      when 'connection_lost' then 'lost'
      else 'reconnected'
    end;
    if v_call.connection_state = v_connection_state then
      return jsonb_build_object('status','accepted','connection_state',v_connection_state,'signaled',true,'transitioned',false);
    end if;
    if (p_action='connection_established' and v_call.connection_state <> 'pending')
      or (p_action='connection_lost' and v_call.connection_state not in ('connected','reconnected'))
      or (p_action='reconnection_completed' and v_call.connection_state <> 'lost') then
      return jsonb_build_object('status','accepted','connection_state',v_call.connection_state,'signaled',false,'transitioned',false);
    end if;
    update public.calls set connection_state=v_connection_state, state_version=state_version+1
      where id=p_call_id and status='accepted';
    return jsonb_build_object('status','accepted','connection_state',v_connection_state,'signaled',true,'transitioned',true);
  else
    if v_call.status not in ('ringing','accepted') then
      return jsonb_build_object('status',v_call.status,'ended',false,'transitioned',false);
    end if;
    if v_call.accepted_at is not null then
      v_duration := greatest(0, floor(extract(epoch from (v_now-v_call.accepted_at)))::integer);
    end if;
    update public.calls set status='ended', ended_at=v_now, ended_by=v_user,
      end_reason=case when v_call.status='ringing' and v_user=v_call.caller_id
        then 'caller_cancelled' else 'participant_ended' end,
      duration_seconds=v_duration, state_version=state_version+1
      where id=p_call_id and status in ('ringing','accepted');
    return jsonb_build_object('status','ended','ended',true,'duration_seconds',v_duration,'transitioned',true);
  end if;
end;
$$;
revoke all on function public.transition_line_call(uuid,text,text) from public, anon;
grant execute on function public.transition_line_call(uuid,text,text) to authenticated, service_role;

revoke all on function public.emit_line_call_realtime_events() from public, anon, authenticated;

-- Old event envelopes are not needed forever. Retain seven days for reconnect/recovery.
create index if not exists call_events_created_at_idx on public.call_events(created_at);
